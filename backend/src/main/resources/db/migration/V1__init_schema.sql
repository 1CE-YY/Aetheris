-- Aetheris RAG System - Database Schema Initialization
-- Version: 1.0
-- Date: 2025-12-26
-- Description: Creates all tables for the RAG recommendation system

-- =============================================
-- Table: users (用户表)
-- =============================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'User ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'Username',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'Email address',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Password hash (BCrypt)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Account creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    last_active_at TIMESTAMP NULL COMMENT 'Last active timestamp',
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts';

-- =============================================
-- Table: resources (学习资源表)
-- =============================================
CREATE TABLE resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Resource ID',
    title VARCHAR(200) NOT NULL COMMENT 'Resource title',
    tags VARCHAR(500) COMMENT 'Tags/course directions (comma-separated)',
    file_type VARCHAR(20) NOT NULL COMMENT 'File type (PDF/MARKDOWN)',
    file_path VARCHAR(500) NOT NULL COMMENT 'File storage path',
    file_size BIGINT COMMENT 'File size in bytes',
    description TEXT COMMENT 'Resource description',
    content_hash VARCHAR(64) NOT NULL UNIQUE COMMENT 'Content hash (SHA-256) for deduplication',
    uploaded_by BIGINT NOT NULL COMMENT 'Uploader user ID',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
    chunk_count INT DEFAULT 0 COMMENT 'Number of chunks',
    vectorized BOOLEAN DEFAULT FALSE COMMENT 'Whether vectorization is complete',
    INDEX idx_content_hash (content_hash),
    INDEX idx_uploaded_by (uploaded_by),
    INDEX idx_tags (tags(100)),
    INDEX idx_upload_time (upload_time),
    INDEX idx_file_type (file_type),
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Learning resources';

-- =============================================
-- Table: resource_chunks (资源切片表)
-- =============================================
CREATE TABLE resource_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Chunk ID',
    resource_id BIGINT NOT NULL COMMENT 'Resource ID',
    chunk_index INT NOT NULL COMMENT 'Chunk index (starts from 0)',
    chunk_text TEXT NOT NULL COMMENT 'Chunk text content',
    location_info VARCHAR(500) COMMENT 'Location info (PDF page range/MD chapter path)',
    page_start INT COMMENT 'PDF start page (optional)',
    page_end INT COMMENT 'PDF end page (optional)',
    chapter_path VARCHAR(500) COMMENT 'Markdown chapter path (e.g., Chapter 1>1.1)',
    text_hash VARCHAR(64) NOT NULL COMMENT 'Text hash (SHA-256) for embedding cache',
    vectorized BOOLEAN DEFAULT FALSE COMMENT 'Whether vectorization is complete',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    UNIQUE KEY uk_resource_chunk (resource_id, chunk_index),
    INDEX idx_resource_id (resource_id),
    INDEX idx_text_hash (text_hash),
    INDEX idx_vectorized (vectorized),
    INDEX idx_chunk_index (chunk_index),
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document chunks';

-- =============================================
-- Table: user_behaviors (用户行为记录表)
-- =============================================
CREATE TABLE user_behaviors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Behavior ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    behavior_type ENUM('QUERY', 'CLICK', 'FAVORITE') NOT NULL COMMENT 'Behavior type',
    resource_id BIGINT NULL COMMENT 'Resource ID (null for QUERY behavior)',
    query_text VARCHAR(500) COMMENT 'Query text (for QUERY behavior)',
    behavior_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Behavior timestamp',
    session_id VARCHAR(100) COMMENT 'Session identifier for behavior analysis',
    weight DECIMAL(3,1) DEFAULT 1.0 COMMENT 'Weight for user profiling (query=1.0, click=2.0, favorite=3.0)',
    INDEX idx_user_id (user_id),
    INDEX idx_behavior_type (behavior_type),
    INDEX idx_resource_id (resource_id),
    INDEX idx_behavior_time (behavior_time),
    INDEX idx_session_id (session_id),
    INDEX idx_user_time (user_id, behavior_time),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User behavior records';

-- =============================================
-- Table: user_profiles (用户画像表)
-- =============================================
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Profile ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT 'User ID',
    profile_vector JSON COMMENT 'User interest vector (JSON array)',
    window_size INT DEFAULT 10 COMMENT 'Behavior window size for profiling',
    query_count INT DEFAULT 0 COMMENT 'Total query count',
    click_count INT DEFAULT 0 COMMENT 'Total click count',
    favorite_count INT DEFAULT 0 COMMENT 'Total favorite count',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User profiles';

-- =============================================
-- Table: eval_queries (评测查询表)
-- =============================================
CREATE TABLE eval_queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Query ID',
    query_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'Query identifier (e.g., q001)',
    query_text VARCHAR(500) NOT NULL COMMENT 'Query text',
    relevant_resources JSON NOT NULL COMMENT 'List of relevant resource IDs (JSON array)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    INDEX idx_query_id (query_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evaluation test queries';

-- =============================================
-- Table: eval_runs (评测运行记录表)
-- =============================================
CREATE TABLE eval_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Run ID',
    run_name VARCHAR(200) NOT NULL COMMENT 'Run name',
    run_config JSON NOT NULL COMMENT 'Run configuration (chunkSize, topK, embeddingModel, etc.)',
    use_profile BOOLEAN DEFAULT FALSE COMMENT 'Whether user profiling is enabled',
    metrics JSON COMMENT 'Evaluation metrics (Precision@K, Recall@K, latency)',
    run_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Run timestamp',
    comparison_with_run_id BIGINT NULL COMMENT 'Compare with baseline run ID',
    INDEX idx_run_name (run_name),
    INDEX idx_run_time (run_time),
    INDEX idx_use_profile (use_profile),
    FOREIGN KEY (comparison_with_run_id) REFERENCES eval_runs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evaluation run records';

-- =============================================
-- Initial data for testing
-- =============================================

-- Insert a default admin user (password: admin123, BCrypt hash)
INSERT INTO users (username, email, password_hash) VALUES
('admin', 'admin@aetheris.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1Ik50pcgpBPJXKwOQnPpYGXJKrkLKk6');

-- =============================================
-- Create vector index in Redis (via application, not SQL)
-- =============================================
-- Note: The Redis vector index will be created by VectorService during application startup
-- Index name: chunk_vector_index
-- Vector dimension: 2048 (Zhipu AI embedding-3)
-- Distance metric: COSINE
-- HNSW parameters: M=16, ef_construction=128
