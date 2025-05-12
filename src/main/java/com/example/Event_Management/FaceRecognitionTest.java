package com.example.Event_Management;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognitionTest {

    private static final double MATCH_THRESHOLD = 0.1; // Keep for lenient matching

    public static void main(String[] args) {
        // Load OpenCV native library
        try {
            // Replace with absolute path to opencv_java455.dll if needed
            // System.load("H:/Studies/EMSI-S2/PFA/Project/Event_Management/lib/opencv_java455.dll");
            nu.pattern.OpenCV.loadLocally(); // Fallback for Java >= 12
        } catch (Exception e) {
            System.err.println("Error loading OpenCV native library: " + e.getMessage());
            return;
        }

        try {
            // Load images from filesystem
            String image1Path = "src/main/java/com/example/Event_Management/2.png";
            String image2Path = "src/main/java/com/example/Event_Management/3.png";
            // Recommended: Move images to images/ and update paths
            // String image1Path = "images/1.png";
            // String image2Path = "images/2.png";

            File image1File = new File(image1Path);
            File image2File = new File(image2Path);

            if (!image1File.exists() || !image2File.exists()) {
                System.err.println("Error: Could not find one or both images at specified paths.");
                System.err.println("Image1 path: " + image1File.getAbsolutePath());
                System.err.println("Image2 path: " + image2File.getAbsolutePath());
                return;
            }

            Mat image1 = Imgcodecs.imread(image1File.getAbsolutePath());
            Mat image2 = Imgcodecs.imread(image2File.getAbsolutePath());

            if (image1.empty() || image2.empty()) {
                System.err.println("Error: Could not load one or both images.");
                return;
            }

            // Debug: Log image sizes
            System.out.println("Image1 size: " + image1.cols() + "x" + image1.rows());
            System.out.println("Image2 size: " + image2.cols() + "x" + image2.rows());

            // Initialize face recognition service
            FaceRecognitionService faceRecognitionService = new FaceRecognitionService();

            // Compare faces
            boolean isMatch = faceRecognitionService.compareFaces(image1, image2);
            System.out.println("Faces match: " + isMatch);

            // Release images
            image1.release();
            image2.release();

        } catch (Exception e) {
            System.err.println("Error during face recognition: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class FaceRecognitionService {
        private final CascadeClassifier faceDetector;
        private final ORB orb;

        public FaceRecognitionService() {
            // Initialize Haar Cascade for face detection
            String cascadePath = "haarcascades/haarcascade_frontalface_default.xml";
            // Alternative: Try haarcascade_frontalface_alt.xml
            // String cascadePath = "haarcascades/haarcascade_frontalface_alt.xml";
            File cascadeFile = new File(cascadePath);
            if (!cascadeFile.exists()) {
                System.err.println("Haar Cascade file not found at: " + cascadeFile.getAbsolutePath());
                System.err.println("Please download 'haarcascade_frontalface_default.xml' from:");
                System.err.println("https://github.com/opencv/opencv/blob/4.x/data/haarcascades/haarcascade_frontalface_default.xml");
                System.err.println("Place it in 'H:/Studies/EMSI-S2/PFA/Project/Event_Management/haarcascades/' or update the path.");
                throw new RuntimeException("Haar Cascade file not found at: " + cascadeFile.getAbsolutePath());
            }

            faceDetector = new CascadeClassifier();
            if (!faceDetector.load(cascadeFile.getAbsolutePath())) {
                throw new RuntimeException("Error loading Haar Cascade classifier: " + cascadeFile.getAbsolutePath());
            }

            // Initialize ORB detector
            orb = ORB.create(1000, 1.2f, 8, 31, 0, 2, ORB.FAST_SCORE, 31, 20);
        }

        public boolean compareFaces(Mat image1, Mat image2) {
            // Convert images to grayscale
            Mat gray1 = new Mat();
            Mat gray2 = new Mat();
            Imgproc.cvtColor(image1, gray1, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(image2, gray2, Imgproc.COLOR_BGR2GRAY);

            // Apply CLAHE for better contrast
            Imgproc.createCLAHE(2.0, new Size(8, 8)).apply(gray1, gray1);
            Imgproc.createCLAHE(2.0, new Size(8, 8)).apply(gray2, gray2);

            // Detect faces with relaxed parameters
            MatOfRect faces1 = new MatOfRect();
            MatOfRect faces2 = new MatOfRect();
            faceDetector.detectMultiScale(gray1, faces1, 1.1, 3, 0, new Size(100, 100), new Size(800, 800));
            faceDetector.detectMultiScale(gray2, faces2, 1.1, 3, 0, new Size(100, 100), new Size(800, 800));

            // Debug: Save images with detected faces
            Mat debug1 = image1.clone();
            Mat debug2 = image2.clone();
            for (Rect rect : faces1.toArray()) {
                Imgproc.rectangle(debug1, rect, new Scalar(0, 255, 0), 2);
            }
            for (Rect rect : faces2.toArray()) {
                Imgproc.rectangle(debug2, rect, new Scalar(0, 255, 0), 2);
            }
            Imgcodecs.imwrite("debug1.png", debug1);
            Imgcodecs.imwrite("debug2.png", debug2);
            debug1.release();
            debug2.release();

            if (faces1.empty() || faces2.empty()) {
                System.err.println("Warning: No face detected in one or both images. Ensure images have clear, frontal faces.");
                gray1.release();
                gray2.release();
                faces1.release();
                faces2.release();
                return false;
            }

            // Get the first detected face
            Rect face1Rect = faces1.toArray()[0];
            Rect face2Rect = faces2.toArray()[0];

            // Add padding to face regions (20%)
            face1Rect = addPadding(face1Rect, gray1.cols(), gray1.rows());
            face2Rect = addPadding(face2Rect, gray2.cols(), gray2.rows());

            // Debug: Log face region sizes
            System.out.println("Face1 region: " + face1Rect.width + "x" + face1Rect.height + " at (" + face1Rect.x + "," + face1Rect.y + ")");
            System.out.println("Face2 region: " + face2Rect.width + "x" + face2Rect.height + " at (" + face2Rect.x + "," + face2Rect.y + ")");

            // Crop face regions
            Mat face1 = new Mat(gray1, face1Rect);
            Mat face2 = new Mat(gray2, face2Rect);

            // Save cropped faces for debugging
            Imgcodecs.imwrite("face1.png", face1);
            Imgcodecs.imwrite("face2.png", face2);

            // Apply Gaussian blur
            Imgproc.GaussianBlur(face1, face1, new Size(5, 5), 0);
            Imgproc.GaussianBlur(face2, face2, new Size(5, 5), 0);

            // Resize faces to a consistent size
            Mat face1Resized = new Mat();
            Mat face2Resized = new Mat();
            Size size = new Size(300, 300);
            Imgproc.resize(face1, face1Resized, size);
            Imgproc.resize(face2, face2Resized, size);

            // Detect ORB keypoints and descriptors
            MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
            MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
            Mat descriptors1 = new Mat();
            Mat descriptors2 = new Mat();
            orb.detectAndCompute(face1Resized, new Mat(), keypoints1, descriptors1);
            orb.detectAndCompute(face2Resized, new Mat(), keypoints2, descriptors2);

            // Debug: Log keypoint counts
            System.out.println("Keypoints in face1: " + keypoints1.toArray().length);
            System.out.println("Keypoints in face2: " + keypoints2.toArray().length);

            if (descriptors1.empty() || descriptors2.empty()) {
                System.err.println("Warning: Could not extract ORB features from one or both faces. Ensure images have clear, frontal faces with sufficient resolution.");
                gray1.release();
                gray2.release();
                faces1.release();
                faces2.release();
                face1.release();
                face2.release();
                face1Resized.release();
                face2Resized.release();
                keypoints1.release();
                keypoints2.release();
                descriptors1.release();
                descriptors2.release();
                return false;
            }

            // Match descriptors using BFMatcher
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            List<MatOfDMatch> matches = new ArrayList<>();
            matcher.knnMatch(descriptors1, descriptors2, matches, 2);

            // Filter good matches (less strict)
            List<DMatch> goodMatches = new ArrayList<>();
            for (MatOfDMatch match : matches) {
                DMatch[] dMatches = match.toArray();
                if (dMatches.length > 1 && dMatches[0].distance < 0.85 * dMatches[1].distance) {
                    goodMatches.add(dMatches[0]);
                }
            }

            // Debug: Log match count and score
            System.out.println("Good matches: " + goodMatches.size());
            double matchScore = (double) goodMatches.size() / Math.max(keypoints1.toArray().length, keypoints2.toArray().length);
            System.out.println("Match score: " + matchScore);

            boolean isMatch = matchScore > MATCH_THRESHOLD;

            // Clean up
            gray1.release();
            gray2.release();
            faces1.release();
            faces2.release();
            face1.release();
            face2.release();
            face1Resized.release();
            face2Resized.release();
            keypoints1.release();
            keypoints2.release();
            descriptors1.release();
            descriptors2.release();

            return isMatch;
        }

        private Rect addPadding(Rect rect, int imgWidth, int imgHeight) {
            int paddingX = (int) (rect.width * 0.2);
            int paddingY = (int) (rect.height * 0.2);
            int x = Math.max(0, rect.x - paddingX);
            int y = Math.max(0, rect.y - paddingY);
            int width = Math.min(imgWidth - x, rect.width + 2 * paddingX);
            int height = Math.min(imgHeight - y, rect.height + 2 * paddingY);
            return new Rect(x, y, width, height);
        }
    }
}