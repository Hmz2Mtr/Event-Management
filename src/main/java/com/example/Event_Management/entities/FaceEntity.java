//package com.example.Event_Management.entities;
//
//import jakarta.persistence.*;
//
//@Entity
//public class FaceEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Lob
//    private byte[] image;
//
//    @Column(columnDefinition = "JSON")
//    private String embedding;
//
//    @Column(unique = true)
//    private String name;
//
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//    public byte[] getImage() { return image; }
//    public void setImage(byte[] image) { this.image = image; }
//    public String getEmbedding() { return embedding; }
//    public void setEmbedding(String embedding) { this.embedding = embedding; }
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//}
