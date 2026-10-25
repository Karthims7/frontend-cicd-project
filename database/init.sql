-- Database initialization and seed script for Kubernetes Lab

-- Create users table if not exists
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial test users
INSERT INTO users (username, email, role) 
VALUES 
    ('karthi', 'karthi@lab.internal', 'ADMIN'),
    ('devops_lead', 'devops@lab.internal', 'ENGINEER'),
    ('test_user', 'user@lab.internal', 'USER')
ON CONFLICT (username) DO NOTHING;
