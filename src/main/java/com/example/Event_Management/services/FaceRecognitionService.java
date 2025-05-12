package com.example.Event_Management.services;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.springframework.stereotype.Service;

@Service
public class FaceRecognitionService {

//    private final Net yoloNet;
//    private final Net faceNet;

//    public FaceRecognitionService() {
//        // Load YOLOv8-Face model
//        yoloNet = Dnn.readNetFromONNX("path/to/yolov8-face.onnx");
//        // Load FaceNet model
//        faceNet = Dnn.readNetFromTensorflow("path/to/facenet.pb");
//    }
//
//    public double[] generateEmbedding(Mat image) {
//        // Detect face using YOLOv8-Face
//        Mat blob = Dnn.blobFromImage(image, 1.0 / 255, new org.opencv.core.Size(640, 640), new org.opencv.core.Scalar(0, 0, 0), true, false);
//        yoloNet.setInput(blob);
//        Mat detections = yoloNet.forward();
//
//        // Assume single face detection for simplicity
//        Rect faceRect = extractFaceRect(detections);
//        if (faceRect == null) {
//            throw new RuntimeException("No face detected in the image");
//        }
//
//        // Crop face
//        Mat faceImage = new Mat(image, faceRect);
//        Mat preprocessed = preprocessFace(faceImage);
//
//        // Generate embedding using FaceNet
//        Mat faceBlob = Dnn.blobFromImage(preprocessed, 1.0, new org.opencv.core.Size(160, 160), new org.opencv.core.Scalar(0, 0, 0), false, false);
//        faceNet.setInput(faceBlob);
//        Mat embeddingMat = faceNet.forward();
//
//        double[] embedding = new double[512]; // FaceNet outputs 512-dimensional embeddings
//        embeddingMat.get(0, 0, embedding);
//
//        return embedding;
//    }
//
//    public double computeDistance(double[] embedding1, double[] embedding2) {
//        // Euclidean distance
//        double sum = 0.0;
//        for (int i = 0; i < embedding1.length; i++) {
//            sum += Math.pow(embedding1[i] - embedding2[i], 2);
//        }
//        return Math.sqrt(sum);
//    }
//
//    private Rect extractFaceRect(Mat detections) {
//        // Parse YOLO detections to extract the most confident face bounding box
//        // Simplified: return first detected face
//        for (int i = 0; i < detections.rows(); i++) {
//            float confidence = (float) detections.get(i, 4)[0];
//            if (confidence > 0.5) {
//                int x = (int) (detections.get(i, 0)[0] * 640);
//                int y = (int) (detections.get(i, 1)[0] * 640);
//                int width = (int) (detections.get(i, 2)[0] * 640 - x);
//                int height = (int) (detections.get(i, 3)[0] * 640 - y);
//                return new Rect(x, y, width, height);
//            }
//        }
//        return null;
//    }
//
//    private Mat preprocessFace(Mat faceImage) {
//        // Resize to 160x160 (FaceNet input size), normalize, etc.
//        Mat resized = new Mat();
//        org.opencv.imgproc.Imgproc.resize(faceImage, resized, new org.opencv.core.Size(160, 160));
//        return resized;
//    }
}