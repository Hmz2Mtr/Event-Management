//package com.example.Event_Management.services;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.opencv.core.*;
//import org.opencv.dnn.Net;
//import org.opencv.dnn.Dnn;
//import org.opencv.imgcodecs.Imgcodecs;
//import org.opencv.imgproc.Imgproc;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import jakarta.annotation.PostConstruct;
//import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.StandardCopyOption;
//import java.util.Base64;
//import java.util.List;
//import java.io.File;
//import com.example.Event_Management.repository.FaceRepository;
//import com.example.Event_Management.entities.FaceEntity;
//
//@Service
//public class FaceService {
//    private static final Logger logger = LoggerFactory.getLogger(FaceService.class);
//    private Net faceNet;
//    @Autowired
//    private FaceRepository faceRepository;
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @PostConstruct
//    public void init() throws Exception {
//        logger.info("Initializing FaceService...");
//        // Load the model file from the classpath
//        URL modelUrl = getClass().getResource("/models/openface.nn4.small2.v1.t7");
//        if (modelUrl == null) {
//            logger.error("Model file not found at: /models/openface.nn4.small2.v1.t7");
//            throw new RuntimeException("Model file not found: /models/openface.nn4.small2.v1.t7");
//        }
//        logger.info("Model file found at: {}", modelUrl);
//        File tempFile = File.createTempFile("openface", ".t7");
//        Files.copy(modelUrl.openStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//        logger.info("Copied model to temporary file: {}", tempFile.getAbsolutePath());
//        faceNet = Dnn.readNetFromTorch(tempFile.getAbsolutePath());
//        tempFile.deleteOnExit();
//        logger.info("OpenFace model loaded successfully.");
//    }
//
//    public void storeFace(String base64Image, String name) {
//        byte[] imageData = Base64.getDecoder().decode(base64Image);
//        Mat image = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
//        Mat embedding = extractEmbedding(image);
//        FaceEntity existing = faceRepository.findByName(name);
//        if (existing != null) {
//            existing.setImage(imageData);
//            existing.setEmbedding(matToJson(embedding));
//            faceRepository.save(existing);
//        } else {
//            FaceEntity face = new FaceEntity();
//            face.setImage(imageData);
//            face.setEmbedding(matToJson(embedding));
//            face.setName(name);
//            faceRepository.save(face);
//        }
//    }
//
//    public String recognizeFace(String base64Image) {
//        byte[] imageData = Base64.getDecoder().decode(base64Image);
//        Mat image = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
//        Mat queryEmbedding = extractEmbedding(image);
//        List<FaceEntity> faces = faceRepository.findAll();
//        FaceEntity bestMatch = null;
//        double minDist = Double.MAX_VALUE;
//
//        for (FaceEntity face : faces) {
//            Mat storedEmbedding = jsonToMat(face.getEmbedding());
//            double dist = Core.norm(queryEmbedding, storedEmbedding, Core.NORM_L2);
//            if (dist < minDist) {
//                minDist = dist;
//                bestMatch = face;
//            }
//        }
//
//        if (minDist < 0.5 && bestMatch != null) {
//            return "Found: " + bestMatch.getName();
//        } else {
//            return "Not found";
//        }
//    }
//
//    private Mat extractEmbedding(Mat image) {
//        Mat resized = new Mat();
//        Imgproc.resize(image, resized, new Size(96, 96));
//        Mat rgb = new Mat();
//        Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);
//        Mat blob = Dnn.blobFromImage(rgb, 1.0 / 255, new Size(96, 96), new Scalar(0), true, false);
//        faceNet.setInput(blob);
//        Mat output = faceNet.forward();
//        Core.normalize(output.reshape(1, 1), output, 1.0, 0, Core.NORM_L2);
//        return output.reshape(1, 1);
//    }
//
//    private String matToJson(Mat mat) {
//        float[] data = new float[(int) (mat.total() * mat.channels())];
//        mat.get(0, 0, data);
//        try {
//            return objectMapper.writeValueAsString(data);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Failed to serialize embedding", e);
//        }
//    }
//
//    private Mat jsonToMat(String json) {
//        try {
//            float[] data = objectMapper.readValue(json, float[].class);
//            Mat mat = new Mat(1, data.length, CvType.CV_32FC1);
//            mat.put(0, 0, data);
//            return mat;
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Failed to deserialize embedding", e);
//        }
//    }
//}