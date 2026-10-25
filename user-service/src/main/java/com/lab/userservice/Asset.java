package com.lab.userservice;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false, length = 50)
    private String sceneId;

    @Column(name = "take_number", nullable = false)
    private Integer takeNumber;

    @Column(name = "camera_roll", nullable = false, length = 50)
    private String cameraRoll;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "s3_bucket_url", nullable = false, length = 255)
    private String s3BucketUrl;

    @Column(name = "processed_s3_url", length = 255)
    private String processedS3Url;

    @Column(name = "script_dialogue", columnDefinition = "TEXT")
    private String scriptDialogue;

    @Column(nullable = false, length = 50)
    private String status; // UPLOADED, PROCESSING_BURN_IN, READY_FOR_REVIEW, APPROVED

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false, length = 10)
    private String fps;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Comment> comments = new ArrayList<>();

    public Asset() {}

    public Asset(String sceneId, Integer takeNumber, String cameraRoll, String title, String s3BucketUrl, String scriptDialogue) {
        this.sceneId = sceneId;
        this.takeNumber = takeNumber;
        this.cameraRoll = cameraRoll;
        this.title = title;
        this.s3BucketUrl = s3BucketUrl;
        this.scriptDialogue = scriptDialogue;
        this.status = "UPLOADED";
        this.durationSeconds = 150;
        this.fps = "24.00";
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "UPLOADED";
        }
        if (this.durationSeconds == null) {
            this.durationSeconds = 150;
        }
        if (this.fps == null) {
            this.fps = "24.00";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }

    public Integer getTakeNumber() {
        return takeNumber;
    }

    public void setTakeNumber(Integer takeNumber) {
        this.takeNumber = takeNumber;
    }

    public String getCameraRoll() {
        return cameraRoll;
    }

    public void setCameraRoll(String cameraRoll) {
        this.cameraRoll = cameraRoll;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getS3BucketUrl() {
        return s3BucketUrl;
    }

    public void setS3BucketUrl(String s3BucketUrl) {
        this.s3BucketUrl = s3BucketUrl;
    }

    public String getProcessedS3Url() {
        return processedS3Url;
    }

    public void setProcessedS3Url(String processedS3Url) {
        this.processedS3Url = processedS3Url;
    }

    public String getScriptDialogue() {
        return scriptDialogue;
    }

    public void setScriptDialogue(String scriptDialogue) {
        this.scriptDialogue = scriptDialogue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}
