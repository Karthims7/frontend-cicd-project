package com.lab.userservice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CollabCutBlueprintController {

    private final AssetRepository assetRepository;
    private final CommentRepository commentRepository;
    private final EventLogRepository eventLogRepository;

    public CollabCutBlueprintController(
            AssetRepository assetRepository,
            CommentRepository commentRepository,
            EventLogRepository eventLogRepository) {
        this.assetRepository = assetRepository;
        this.commentRepository = commentRepository;
        this.eventLogRepository = eventLogRepository;
    }

    // ==========================================
    // 1. INGEST API (Service 1)
    // ==========================================

    @GetMapping("/assets")
    public List<Asset> getAllAssets() {
        return assetRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/assets/{id}")
    public ResponseEntity<Asset> getAssetById(@PathVariable Long id) {
        return assetRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/assets")
    public ResponseEntity<?> uploadAssetMetadata(@RequestBody Asset asset) {
        if (asset.getTitle() == null || asset.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Asset title is required"));
        }
        if (asset.getSceneId() == null || asset.getSceneId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "scene_id is required (e.g. Scene 14B)"));
        }
        if (asset.getTakeNumber() == null) {
            asset.setTakeNumber(1);
        }
        if (asset.getCameraRoll() == null || asset.getCameraRoll().trim().isEmpty()) {
            asset.setCameraRoll("Roll A-01");
        }
        if (asset.getS3BucketUrl() == null || asset.getS3BucketUrl().trim().isEmpty()) {
            // Default object storage path mapping
            String cleanScene = asset.getSceneId().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            asset.setS3BucketUrl("s3://collabcut-raw/" + cleanScene + "_tk" + asset.getTakeNumber() + ".mp4");
        }

        asset.setStatus("UPLOADED");
        Asset saved = assetRepository.save(asset);

        // Publish event: footage.uploaded
        String eventPayload = String.format(
            "{\"event\":\"footage.uploaded\",\"asset_id\":%d,\"scene\":\"%s\",\"take\":%d,\"s3_url\":\"%s\",\"timestamp\":\"%s\"}",
            saved.getId(), saved.getSceneId(), saved.getTakeNumber(), saved.getS3BucketUrl(), Instant.now().toString()
        );
        eventLogRepository.save(new EventLog("footage.uploaded", eventPayload));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/assets/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        if (!assetRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        assetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 2. SCRIPT BURN-IN WORKER (Service 2 Pipeline)
    // ==========================================

    @PostMapping("/assets/{id}/burn-in")
    public ResponseEntity<?> processScriptBurnIn(@PathVariable Long id) {
        return assetRepository.findById(id).map(asset -> {
            // Simulates Worker: 
            // 1. Download proxy from s3_bucket_url
            // 2. Execute FFmpeg subtitle overlay
            // 3. Upload to s3://collabcut-burned/...
            // 4. Publish footage.processed
            String rawUrl = asset.getS3BucketUrl();
            String burnedUrl = rawUrl.replace(".mp4", "_burned.mp4").replace("collabcut-raw", "collabcut-burned");

            asset.setProcessedS3Url(burnedUrl);
            asset.setStatus("READY_FOR_REVIEW");
            Asset updated = assetRepository.save(asset);

            String eventPayload = String.format(
                "{\"event\":\"footage.processed\",\"asset_id\":%d,\"scene\":\"%s\",\"burned_url\":\"%s\",\"worker\":\"ffmpeg-worker-pod\",\"timestamp\":\"%s\"}",
                updated.getId(), updated.getSceneId(), burnedUrl, Instant.now().toString()
            );
            eventLogRepository.save(new EventLog("footage.processed", eventPayload));

            return ResponseEntity.ok(Map.of(
                "message", "FFmpeg worker burned script dialogue into proxy. Event 'footage.processed' published.",
                "asset", updated
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // 3. REVIEW & FEEDBACK API (Service 3)
    // ==========================================

    @GetMapping("/comments/{assetId}")
    public List<Comment> getCommentsForAsset(@PathVariable Long assetId) {
        return commentRepository.findByAssetIdOrderByTimestampSecondsAsc(assetId);
    }

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(@RequestBody Map<String, Object> body) {
        Long assetId = body.get("asset_id") != null ? Long.valueOf(body.get("asset_id").toString()) : null;
        Integer timestampSeconds = body.get("timestamp_seconds") != null ? Integer.valueOf(body.get("timestamp_seconds").toString()) : 0;
        String timecode = body.get("timecode") != null ? body.get("timecode").toString() : "00:00:00:00";
        String userId = body.get("user_id") != null ? body.get("user_id").toString() : "Director";
        String department = body.get("department") != null ? body.get("department").toString() : "EDITORIAL";
        String commentText = body.get("comment_text") != null ? body.get("comment_text").toString() : "";

        if (assetId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "asset_id is required"));
        }
        if (commentText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "comment_text cannot be empty"));
        }

        return assetRepository.findById(assetId).map(asset -> {
            Comment comment = new Comment(asset, timestampSeconds, timecode, userId, department, commentText);
            Comment saved = commentRepository.save(comment);

            // Publish event: comment.created
            String eventPayload = String.format(
                "{\"event\":\"comment.created\",\"asset_id\":%d,\"timecode\":\"%s\",\"author\":\"%s\",\"dept\":\"%s\",\"timestamp\":\"%s\"}",
                assetId, timecode, userId, department, Instant.now().toString()
            );
            eventLogRepository.save(new EventLog("comment.created", eventPayload));

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        if (!commentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        commentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/assets/{id}/status")
    public ResponseEntity<?> updateAssetStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }

        return assetRepository.findById(id).map(asset -> {
            asset.setStatus(newStatus.toUpperCase());
            Asset saved = assetRepository.save(asset);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // 4. EVENT BROKER & PIPELINE METRICS
    // ==========================================

    @GetMapping("/events")
    public List<EventLog> getEventLogs() {
        return eventLogRepository.findTop20ByOrderByIdDesc();
    }

    @GetMapping("/stats")
    public Map<String, Object> getPipelineStats() {
        long totalAssets = assetRepository.count();
        long uploaded = assetRepository.countByStatus("UPLOADED");
        long readyForReview = assetRepository.countByStatus("READY_FOR_REVIEW");
        long approved = assetRepository.countByStatus("APPROVED");
        long totalComments = commentRepository.count();
        long totalEvents = eventLogRepository.count();

        return Map.of(
            "totalAssets", totalAssets,
            "uploadedCount", uploaded,
            "readyForReviewCount", readyForReview,
            "approvedCount", approved,
            "totalComments", totalComments,
            "totalEventsEmitted", totalEvents,
            "eventBroker", "RabbitMQ AMQP",
            "objectStorage", "MinIO / S3",
            "databaseStatus", "PostgreSQL 16 (Connected)"
        );
    }
}
