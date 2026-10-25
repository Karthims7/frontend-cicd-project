# CollabCut Database Schema & Initialization

This directory contains the database initialization scripts for the CollabCut Film Post-Production Tracker.

## Tables
- `clips`: Raw and proxy footage metadata (Scene, Take, Camera Roll, Status, Duration, FPS, Dialogue Script).
- `timecoded_notes`: Frame-accurate director, editor, sound supervisor, and colorist review feedback (Clip ID, Timecode, Author, Department, Comment).

## Manual Execution / Re-Seeding
To execute or re-seed the PostgreSQL database running in Kubernetes:

```bash
kubectl exec -i $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb < database/init.sql
```

To query footage clips and feedback notes:
```bash
# Query all clips
kubectl exec $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb -c "SELECT id, scene, take, camera_roll, status, title FROM clips;"

# Query timecoded notes
kubectl exec $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb -c "SELECT clip_id, timecode, author, department, comment FROM timecoded_notes;"
```
