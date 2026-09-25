-- ====================================================
-- Database: air_quality_db
-- Run this script in MySQL Workbench or mysql CLI
-- ====================================================

CREATE DATABASE IF NOT EXISTS air_quality_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE air_quality_db;

-- 1. Users Table (Email as UNIQUE, Passwords hashed with BCrypt)
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Air Quality History Table (Foreign key links to users.id)
CREATE TABLE IF NOT EXISTS air_quality_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    aqi INT,
    co DOUBLE,
    no DOUBLE,
    no2 DOUBLE,
    o3 DOUBLE,
    so2 DOUBLE,
    pm2_5 DOUBLE,
    pm10 DOUBLE,
    nh3 DOUBLE,
    searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_history
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Fast index for user search history ordered by timestamp
CREATE INDEX idx_user_searched ON air_quality_history(user_id, searched_at DESC);
