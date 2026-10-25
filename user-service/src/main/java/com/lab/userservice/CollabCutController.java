package com.lab.userservice;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CollabCutController {

    private final ClipRepository clipRepository;
    private final TimecodedNoteRepository noteRepository;

    public CollabCutController(ClipRepository clipRepository, TimecodedNoteRepository noteRepository) {
        this.clipRepository = clipRepository;
        this.noteRepository = noteRepository;
    }

    @GetMapping("/clips")
    public List<Clip> getAllClips() {
        return clipRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/clips/{id}")
    public ResponseEntity<Clip> getClipById(@PathVariable Long id) {
        return clipRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/clips")
    public ResponseEntity<?> ingestClip(@RequestBody Clip clip) {
        if (clip.getTitle() == null || clip.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Clip title is required"));
        }
        if (clip.getScene() == null || clip.getScene().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Scene designation is required (e.g. Scene 14B)"));
        }
        if (clip.getTake() == null) {
            clip.setTake(1);
        }
        if (clip.getCameraRoll() == null || clip.getCameraRoll().trim().isEmpty()) {
            clip.setCameraRoll("Roll A-01");
        }
        if (clip.getStatus() == null || clip.getStatus().trim().isEmpty()) {
            clip.setStatus("INGESTED");
        }

        Clip savedClip = clipRepository.save(clip);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedClip);
    }

    @PostMapping("/clips/{id}/burn-in")
    public ResponseEntity<?> triggerScriptBurnIn(@PathVariable Long id) {
        return clipRepository.findById(id).map(clip -> {
            // Simulate the FFmpeg script burn-in processing stage
            clip.setStatus("IN_REVIEW");
            clipRepository.save(clip);
            return ResponseEntity.ok(Map.of(
                "message", "Script dialogue successfully burned into video proxy file. Moved to IN_REVIEW.",
                "clip", clip
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/clips/{id}/status")
    public ResponseEntity<?> updateClipStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status field is required"));
        }

        return clipRepository.findById(id).map(clip -> {
            clip.setStatus(newStatus.toUpperCase());
            Clip updated = clipRepository.save(clip);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/clips/{id}/notes")
    public ResponseEntity<?> addTimecodedNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String timecode = body.get("timecode");
        String author = body.get("author");
        String department = body.get("department");
        String comment = body.get("comment");

        if (timecode == null || timecode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Timecode is required (e.g. 00:01:24:12)"));
        }
        if (comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment message is required"));
        }
        if (author == null || author.trim().isEmpty()) {
            author = "Director";
        }
        if (department == null || department.trim().isEmpty()) {
            department = "EDITORIAL";
        }

        final String finalAuthor = author;
        final String finalDepartment = department;

        return clipRepository.findById(id).map(clip -> {
            TimecodedNote note = new TimecodedNote(clip, timecode, finalAuthor, finalDepartment, comment);
            TimecodedNote saved = noteRepository.save(note);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/clips/{id}")
    public ResponseEntity<?> deleteClip(@PathVariable Long id) {
        if (!clipRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clipRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public Map<String, Object> getPipelineStats() {
        long totalClips = clipRepository.count();
        long ingested = clipRepository.countByStatus("INGESTED");
        long burnIn = clipRepository.countByStatus("SCRIPT_BURN_IN");
        long inReview = clipRepository.countByStatus("IN_REVIEW");
        long approved = clipRepository.countByStatus("APPROVED");
        long totalNotes = noteRepository.count();

        return Map.of(
            "totalClips", totalClips,
            "ingestedCount", ingested,
            "burnInCount", burnIn,
            "inReviewCount", inReview,
            "approvedCount", approved,
            "totalNotes", totalNotes,
            "pipelineEngine", "CollabCut FFmpeg + Spring Data JPA",
            "databaseStatus", "Connected (PostgreSQL 16)"
        );
    }
}
