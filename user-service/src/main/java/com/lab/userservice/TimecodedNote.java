package com.lab.userservice;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "timecoded_notes")
public class TimecodedNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clip_id", nullable = false)
    @JsonBackReference
    private Clip clip;

    @Column(nullable = false, length = 20)
    private String timecode; // e.g. 00:01:24:12

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false, length = 50)
    private String department; // EDITORIAL, COLOR_GRADE, SOUND_DESIGN, VFX

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public TimecodedNote() {}

    public TimecodedNote(Clip clip, String timecode, String author, String department, String comment) {
        this.clip = clip;
        this.timecode = timecode;
        this.author = author;
        this.department = department;
        this.comment = comment;
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

    public Clip getClip() {
        return clip;
    }

    public void setClip(Clip clip) {
        this.clip = clip;
    }

    public String getTimecode() {
        return timecode;
    }

    public void setTimecode(String timecode) {
        this.timecode = timecode;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
