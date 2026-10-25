-- CollabCut: Independent Film Post-Production Tracker Database Schema

-- Drop existing tables if needed for fresh start
DROP TABLE IF EXISTS timecoded_notes;
DROP TABLE IF EXISTS clips;

-- Footage Clips Table (Ingest & Burn-in Pipeline)
CREATE TABLE clips (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    scene VARCHAR(50) NOT NULL,
    take INTEGER NOT NULL,
    camera_roll VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INGESTED', -- INGESTED, SCRIPT_BURN_IN, IN_REVIEW, APPROVED
    duration VARCHAR(20) NOT NULL DEFAULT '02:30',
    fps VARCHAR(10) NOT NULL DEFAULT '24.00',
    script_dialogue TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Timecoded Director & Producer Feedback Notes Table
CREATE TABLE timecoded_notes (
    id BIGSERIAL PRIMARY KEY,
    clip_id BIGINT NOT NULL REFERENCES clips(id) ON DELETE CASCADE,
    timecode VARCHAR(20) NOT NULL, -- e.g. 00:01:24:12
    author VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL, -- EDITORIAL, COLOR_GRADE, SOUND_DESIGN, VFX
    comment TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Initial Film Production Assets
INSERT INTO clips (title, scene, take, camera_roll, status, duration, fps, script_dialogue)
VALUES
    ('Diner Alley Confrontation', 'Scene 14B', 3, 'Roll A-04', 'IN_REVIEW', '03:12', '24.00', 'MARCUS: (whispering) "If we leave now, there is no turning back."'),
    ('Rooftop Golden Hour Escape', 'Scene 22A', 1, 'Roll B-12', 'SCRIPT_BURN_IN', '01:45', '24.00', 'ELENA: "Look at the horizon. The storm is already here."'),
    ('Vault Heist Extraction', 'Scene 08C', 5, 'Roll A-02', 'APPROVED', '04:10', '24.00', 'COOPER: "Code accepted. 30 seconds to lock down."'),
    ('Interrogation Room Two-Shot', 'Scene 03', 2, 'Roll C-01', 'INGESTED', '02:05', '24.00', 'DETECTIVE: "You were spotted on 5th avenue at 11 PM."');

-- Seed Initial Timecoded Feedback Notes
INSERT INTO timecoded_notes (clip_id, timecode, author, department, comment)
VALUES
    (1, '00:01:14:08', 'Director Nolan', 'EDITORIAL', 'Trim 6 frames off head of take. The pause feels too prolonged before Marcus speaks.'),
    (1, '00:02:05:18', 'Sound Supv. Maya', 'SOUND_DESIGN', 'Footsteps in alleyway echo too loud against brick wall. Dampen low-end EQ.'),
    (2, '00:00:45:02', 'Colorist James', 'COLOR_GRADE', 'Highlight clipping on Elena jacket during camera tilt. Bring exposure down -0.5 stop.'),
    (3, '00:03:18:12', 'VFX Lead David', 'VFX', 'Clean up the green-screen reflection on the glass panel.');
