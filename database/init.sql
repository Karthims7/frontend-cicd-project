-- CollabCut: Definitive Blueprint Database Schema

DROP TABLE IF EXISTS event_logs;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS assets;

-- 1. Assets Table (Ingest API)
CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,
    scene_id VARCHAR(50) NOT NULL,
    take_number INTEGER NOT NULL,
    camera_roll VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    s3_bucket_url VARCHAR(255) NOT NULL,
    processed_s3_url VARCHAR(255),
    script_dialogue TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED', -- UPLOADED, PROCESSING_BURN_IN, READY_FOR_REVIEW, APPROVED
    duration_seconds INTEGER NOT NULL DEFAULT 150,
    fps VARCHAR(10) NOT NULL DEFAULT '24.00',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Comments Table (Review & Feedback API)
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    timestamp_seconds INTEGER NOT NULL,
    timecode VARCHAR(20) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL DEFAULT 'EDITORIAL',
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Event Logs Table (Asynchronous Event Broker Feed)
CREATE TABLE event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Initial Film Production Assets
INSERT INTO assets (scene_id, take_number, camera_roll, title, s3_bucket_url, processed_s3_url, script_dialogue, status, duration_seconds)
VALUES
    ('Scene 14B', 3, 'Roll A-04', 'Diner Alley Confrontation', 's3://collabcut-raw/scene14b_tk3.mp4', 's3://collabcut-burned/scene14b_tk3_burned.mp4', 'MARCUS: (whispering) "If we leave now, there is no turning back."', 'READY_FOR_REVIEW', 192),
    ('Scene 22A', 1, 'Roll B-12', 'Rooftop Golden Hour Escape', 's3://collabcut-raw/scene22a_tk1.mp4', NULL, 'ELENA: "Look at the horizon. The storm is already here."', 'UPLOADED', 105),
    ('Scene 08C', 5, 'Roll A-02', 'Vault Heist Extraction', 's3://collabcut-raw/scene08c_tk5.mp4', 's3://collabcut-burned/scene08c_tk5_burned.mp4', 'COOPER: "Code accepted. 30 seconds to lock down."', 'APPROVED', 250),
    ('Scene 03', 2, 'Roll C-01', 'Interrogation Two-Shot', 's3://collabcut-raw/scene03_tk2.mp4', NULL, 'DETECTIVE: "You were spotted on 5th avenue at 11 PM."', 'UPLOADED', 125);

-- Seed Timecoded Notes
INSERT INTO comments (asset_id, timestamp_seconds, timecode, user_id, department, comment_text)
VALUES
    (1, 74, '00:01:14:08', 'Director Nolan', 'EDITORIAL', 'Trim 6 frames off head of take. The pause feels too prolonged before Marcus speaks.'),
    (1, 125, '00:02:05:18', 'Sound Supv. Maya', 'SOUND_DESIGN', 'Footsteps in alleyway echo too loud against brick wall. Dampen low-end EQ.'),
    (3, 198, '00:03:18:12', 'VFX Lead David', 'VFX', 'Clean up the green-screen reflection on the security keypad glass.');

-- Seed Initial Event Broker Stream Logs
INSERT INTO event_logs (event_name, payload)
VALUES
    ('footage.uploaded', '{"event": "footage.uploaded", "asset_id": 1, "s3_url": "s3://collabcut-raw/scene14b_tk3.mp4", "scene": "Scene 14B"}'),
    ('footage.processed', '{"event": "footage.processed", "asset_id": 1, "burned_url": "s3://collabcut-burned/scene14b_tk3_burned.mp4", "engine": "FFmpeg v6.1"}'),
    ('footage.uploaded', '{"event": "footage.uploaded", "asset_id": 2, "s3_url": "s3://collabcut-raw/scene22a_tk1.mp4", "scene": "Scene 22A"}'),
    ('comment.created', '{"event": "comment.created", "asset_id": 1, "author": "Director Nolan", "timecode": "00:01:14:08"}');
