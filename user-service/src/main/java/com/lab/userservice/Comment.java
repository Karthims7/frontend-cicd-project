package com.lab.userservice;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @JsonBackReference
    private Asset asset;

    @Column(name = "timestamp_seconds", nullable = false)
    private Integer timestampSeconds;

    @Column(nullable = false, length = 20)
    private String timecode; // e.g. 00:01:24:12

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId; // Author (e.g. Director Nolan)

    @Column(nullable = false, length = 50)
    private String department; // EDITORIAL, COLOR_GRADE, SOUND_DESIGN, VFX

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Comment() {}

    public Comment(Asset asset, Integer timestampSeconds, String timecode, String userId, String department, String commentText) {
        this.asset = asset;
        this.timestampSeconds = timestampSeconds;
        this.timecode = timecode;
        this.userId = userId;
        this.department = department;
        this.commentText = commentText;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Integer getTimestampSeconds() {
        return timestampSeconds;
    }

    public void setTimestampSeconds(Integer timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    public String getTimecode() {
        return timecode;
    }

    public void setTimecode(String timecode) {
        this.timecode = timecode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
