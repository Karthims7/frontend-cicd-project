package com.lab.userservice;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clips")
public class Clip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 50)
    private String scene;

    @Column(nullable = false)
    private Integer take;

    @Column(name = "camera_roll", nullable = false, length = 50)
    private String cameraRoll;

    @Column(nullable = false, length = 30)
    private String status; // INGESTED, SCRIPT_BURN_IN, IN_REVIEW, APPROVED

    @Column(nullable = false, length = 20)
    private String duration;

    @Column(nullable = false, length = 10)
    private String fps;

    @Column(name = "script_dialogue", columnDefinition = "TEXT")
    private String scriptDialogue;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "clip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<TimecodedNote> notes = new ArrayList<>();

    public Clip() {}

    public Clip(String title, String scene, Integer take, String cameraRoll, String scriptDialogue) {
        this.title = title;
        this.scene = scene;
        this.take = take;
        this.cameraRoll = cameraRoll;
        this.scriptDialogue = scriptDialogue;
        this.status = "INGESTED";
        this.duration = "02:30";
        this.fps = "24.00";
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "INGESTED";
        }
        if (this.fps == null) {
            this.fps = "24.00";
        }
        if (this.duration == null) {
            this.duration = "02:30";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }

    public String getCameraRoll() {
        return cameraRoll;
    }

    public void setCameraRoll(String cameraRoll) {
        this.cameraRoll = cameraRoll;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public String getScriptDialogue() {
        return scriptDialogue;
    }

    public void setScriptDialogue(String scriptDialogue) {
        this.scriptDialogue = scriptDialogue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<TimecodedNote> getNotes() {
        return notes;
    }

    public void setNotes(List<TimecodedNote> notes) {
        this.notes = notes;
    }
}
