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
        gray.convertTo(brightGray, -1, 1.3, 30);
        Imgproc.equalizeHist(brightGray, brightGray);

        // Try detection with different parameters
        MatOfRect faces = new MatOfRect();
        Rect bestFace = null;
        double bestArea = 0;

        // More conservative detection parameters
        double[] scaleFactors = {1.2, 1.3, 1.4}; // Increased scale factors
        int[] minNeighbors = {5, 6, 7}; // Increased minimum neighbors
        
        // Calculate minimum and maximum face sizes
        int minDim = Math.min(gray.cols(), gray.rows());
        int minFaceSize = (int) (minDim * 0.2); // Increased from 0.1 to 0.2
        int maxFaceSize = (int) (minDim * 0.8); // Decreased from 0.9 to 0.8
        Size minSize = new Size(minFaceSize, minFaceSize);
        Size maxSize = new Size(maxFaceSize, maxFaceSize);

        try {
            for (CascadeClassifier detector : faceDetectors) {
                if (detector == null || detector.empty()) continue;

                for (double scaleFactor : scaleFactors) {
                    for (int neighbors : minNeighbors) {
                        try {
                            detector.detectMultiScale(
                                brightGray,
                                faces,
                                scaleFactor,
                                neighbors,
                                0,
                                minSize,
                                maxSize
                            );

                            if (!faces.empty()) {
                                Rect[] facesArray = faces.toArray();
                                for (Rect face : facesArray) {
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
                            logger.warn("Detection failed with scale={}, neighbors={}: {}", 
                                      scaleFactor, neighbors, e.getMessage());
                            // Continue with next parameters
                        }
                    }
                }
            }

            if (bestFace == null) {
                logger.debug("No faces detected");
                gray.release();
                brightGray.release();
                faces.release();
                return null;
            }

            // Use smaller padding to reduce boundary issues
            int desiredPadding = (int) (Math.min(bestFace.width, bestFace.height) * 0.1); // Reduced from 0.2 to 0.1
            int leftPadding = Math.min(desiredPadding, bestFace.x);
            int topPadding = Math.min(desiredPadding, bestFace.y);
            int rightPadding = Math.min(desiredPadding, gray.cols() - (bestFace.x + bestFace.width));
            int bottomPadding = Math.min(desiredPadding, gray.rows() - (bestFace.y + bestFace.height));

            // Create padded rectangle with validated boundaries
            Rect paddedFace = new Rect(
                bestFace.x - leftPadding,
                bestFace.y - topPadding,
                bestFace.width + leftPadding + rightPadding,
                bestFace.height + topPadding + bottomPadding
            );

            // Strict boundary check
            if (paddedFace.x < 0 || paddedFace.y < 0 ||
                paddedFace.x + paddedFace.width > gray.cols() ||
                paddedFace.y + paddedFace.height > gray.rows() ||
                paddedFace.width <= 0 || paddedFace.height <= 0) {
                logger.error("Invalid padded face rectangle: " + paddedFace);
                gray.release();
                brightGray.release();
                faces.release();
                return null;
            }

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

            return normalizedFace;
            
        } catch (Exception e) {
            logger.error("Error in face detection/processing: {}", e.getMessage(), e);
            if (gray != null) gray.release();
            if (brightGray != null) brightGray.release();
            if (faces != null) faces.release();
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