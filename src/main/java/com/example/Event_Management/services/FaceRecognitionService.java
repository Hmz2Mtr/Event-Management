package com.example.Event_Management.services;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Service
public class FaceRecognitionService {
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);
    private List<CascadeClassifier> faceDetectors;
    private static final double SIMILARITY_THRESHOLD = 0.8;
    public static final double HIGH_CONFIDENCE_THRESHOLD = 0.5;
    public static final double LOW_CONFIDENCE_THRESHOLD = 0.3;

    private static final String[] CASCADE_FILES = new String[] {
        "haarcascade_frontalface_default.xml",
        "haarcascade_frontalface_alt.xml",
        "haarcascade_frontalface_alt2.xml"
    };

    public enum MatchQuality {
        HIGH_CONFIDENCE,
        MEDIUM_CONFIDENCE,
        LOW_CONFIDENCE,
        NO_MATCH
    }

    public class FaceQualityMetrics {
        double brightness;
        double contrast;
        double sharpness;
        double pose;
        boolean isOccluded;
    }

    private Map<Integer, Rect> trackedFaces = new HashMap<>();

    static {
        // Load OpenCV native library
        nu.pattern.OpenCV.loadLocally();
    }

    @PostConstruct
    public void init() throws IOException {
        faceDetectors = new ArrayList<>();
        
        for (String cascadeFile : CASCADE_FILES) {
            try {
                CascadeClassifier detector = loadCascadeClassifier(cascadeFile);
                if (detector != null && !detector.empty()) {
                    faceDetectors.add(detector);
                    logger.info("Successfully loaded cascade classifier: {}", cascadeFile);
                }
            } catch (Exception e) {
                logger.warn("Failed to load cascade classifier {}: {}", cascadeFile, e.getMessage());
            }
        }

        if (faceDetectors.isEmpty()) {
            throw new IOException("Failed to load any cascade classifiers");
        }
        
        logger.info("Initialized {} face detectors", faceDetectors.size());
    }

    private CascadeClassifier loadCascadeClassifier(String cascadeFileName) throws IOException {
        // Try multiple locations for the cascade classifier
        File cascadeFile = null;
        
        // First, try to load from classpath resources
        try {
            Resource resource = new ClassPathResource("haarcascades/" + cascadeFileName);
            cascadeFile = resource.getFile();
        } catch (Exception e) {
            logger.debug("Could not load {} from classpath, trying alternative locations", cascadeFileName);
            
            // Try various locations
            String[] possiblePaths = {
                "haarcascades/" + cascadeFileName,
                "src/main/resources/haarcascades/" + cascadeFileName,
                cascadeFileName
            };

            for (String path : possiblePaths) {
                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    cascadeFile = filePath.toFile();
                    break;
                }
            }

            // If still not found, try to extract from OpenCV's resources
            if (cascadeFile == null || !cascadeFile.exists()) {
                cascadeFile = extractCascadeFromOpenCV(cascadeFileName);
            }
        }

        if (cascadeFile == null || !cascadeFile.exists()) {
            logger.warn("Could not find cascade file: {}", cascadeFileName);
            return null;
        }

        CascadeClassifier classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        if (classifier.empty()) {
            logger.warn("Failed to initialize classifier from: {}", cascadeFile.getAbsolutePath());
            return null;
        }

        return classifier;
    }

    private File extractCascadeFromOpenCV(String cascadeFileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/org/opencv/data/" + cascadeFileName)) {
            if (is != null) {
                File tempFile = File.createTempFile("cascade", ".xml");
                tempFile.deleteOnExit();
                try (FileOutputStream os = new FileOutputStream(tempFile)) {
                    FileCopyUtils.copy(is, os);
                }
                return tempFile;
            }
        }
        return null;
    }

    public Mat detectAndAlignFace(Mat image) {
        if (image.empty()) {
            logger.warn("Input image is empty");
            return null;
        }

        // Convert to grayscale and preprocess
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        
        // Create a brighter version for detection
        Mat brightGray = new Mat();
        gray.convertTo(brightGray, -1, 1.3, 30); // Increase brightness
        Imgproc.equalizeHist(brightGray, brightGray);

        // Try detection with different parameters
        MatOfRect faces = new MatOfRect();
        Rect bestFace = null;
        double bestArea = 0;

        // Safer detection parameters to avoid scale assertion error
        double[] scaleFactors = {1.1, 1.15, 1.2}; // Increased minimum scale factor
        int[] minNeighbors = {3, 4, 5}; // Standard neighbor requirements
        
        // Calculate minimum face size based on image dimensions
        int minDim = Math.min(gray.cols(), gray.rows());
        int minFaceSize = (int) (minDim * 0.1); // 10% of the smaller image dimension
        Size minSize = new Size(minFaceSize, minFaceSize);
        Size maxSize = new Size(gray.cols() * 0.9, gray.rows() * 0.9); // 90% of image dimensions

        try {
            for (CascadeClassifier detector : faceDetectors) {
                if (detector == null || detector.empty()) {
                    logger.warn("Skipping empty/null detector");
                    continue;
                }

                for (double scaleFactor : scaleFactors) {
                    for (int neighbors : minNeighbors) {
                        try {
                            // Ensure image dimensions are valid
                            if (brightGray.cols() <= 0 || brightGray.rows() <= 0) {
                                logger.warn("Invalid image dimensions: {}x{}", brightGray.cols(), brightGray.rows());
                                continue;
                            }

                            // Ensure scale factor is valid
                            if (scaleFactor <= 1.0) {
                                logger.warn("Invalid scale factor: {}", scaleFactor);
                                continue;
                            }

                            detector.detectMultiScale(
                                brightGray,      // Input image
                                faces,           // Output faces
                                scaleFactor,     // Scale factor
                                neighbors,       // Min neighbors
                                0,              // Flags
                                minSize,        // Min size
                                maxSize         // Max size
                            );

                            if (!faces.empty()) {
                                Rect[] facesArray = faces.toArray();
                                for (Rect face : facesArray) {
                                    // Validate face rectangle
                                    if (face.x >= 0 && face.y >= 0 && 
                                        face.width > 0 && face.height > 0 &&
                                        face.x + face.width <= brightGray.cols() &&
                                        face.y + face.height <= brightGray.rows()) {
                                        
                                        double area = face.area();
                                        if (area > bestArea) {
                                            bestArea = area;
                                            bestFace = face.clone();
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Error during face detection with parameters: scale={}, neighbors={}: {}", 
                                      scaleFactor, neighbors, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during face detection process: {}", e.getMessage());
        }

        if (bestFace == null) {
            logger.debug("No faces detected after trying all classifiers and parameters");
            gray.release();
            brightGray.release();
            faces.release();
            return null;
        }

        try {
            // Add padding to the face region (20%)
            int padding = (int) (Math.min(bestFace.width, bestFace.height) * 0.2);
            Rect paddedFace = new Rect(
                Math.max(0, bestFace.x - padding),
                Math.max(0, bestFace.y - padding),
                Math.min(gray.cols() - (bestFace.x - padding), bestFace.width + 2 * padding),
                Math.min(gray.rows() - (bestFace.y - padding), bestFace.height + 2 * padding)
            );

            // Extract and process face region
            Mat face = new Mat(gray, paddedFace);
            Mat normalizedFace = new Mat();
            
            // Resize to standard size
            Size standardSize = new Size(150, 150);
            Imgproc.resize(face, normalizedFace, standardSize);
            
            // Enhance image quality
            Imgproc.GaussianBlur(normalizedFace, normalizedFace, new Size(3, 3), 0);
            Core.normalize(normalizedFace, normalizedFace, 0, 255, Core.NORM_MINMAX);
            
            // Clean up
            gray.release();
            brightGray.release();
            face.release();
            faces.release();

            logger.debug("Successfully detected and processed face");
            return normalizedFace;
            
        } catch (Exception e) {
            logger.error("Error processing detected face: {}", e.getMessage());
            gray.release();
            brightGray.release();
            faces.release();
            return null;
        }
    }

    public double compareFacesWithScore(Mat face1, Mat face2) {
        if (face1.empty() || face2.empty()) {
            return 0.0;
        }

        try {
            // Ensure both faces are the same size
            if (face1.size() != face2.size()) {
                Mat resized = new Mat();
                Imgproc.resize(face2, resized, face1.size());
                face2 = resized;
            }

            // Calculate similarity using normalized correlation coefficient
            Mat result = new Mat();
            Imgproc.matchTemplate(face1, face2, result, Imgproc.TM_CCOEFF_NORMED);
            
            Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(result);
            double similarity = minMaxResult.maxVal;
            
            result.release();
            
            logger.debug("Face comparison similarity score: {}", similarity);
            return similarity;
        } catch (Exception e) {
            logger.error("Error comparing faces: ", e);
            return 0.0;
        }
    }

    public boolean compareFaces(Mat face1, Mat face2) {
        return compareFacesWithScore(face1, face2) >= SIMILARITY_THRESHOLD;
    }

    private boolean isGoodQualityFace(Mat face) {
        // Check brightness
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(face, mean, stddev);
        double brightness = mean.get(0, 0)[0];
        double contrast = stddev.get(0, 0)[0];
        
        // Check blur
        Mat laplacian = new Mat();
        Imgproc.Laplacian(face, laplacian, CvType.CV_64F);
        MatOfDouble lapMean = new MatOfDouble();
        Core.meanStdDev(laplacian, lapMean, new MatOfDouble());
        double sharpness = lapMean.get(0, 0)[0];
        
        return brightness > 40 && brightness < 250 && 
               contrast > 20 && 
               sharpness > 100;
    }
}