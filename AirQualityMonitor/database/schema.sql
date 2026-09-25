-- ====================================================
-- Air Quality Monitor Database Schema
-- Database: air_quality_db
-- ====================================================

CREATE DATABASE IF NOT EXISTS air_quality_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE air_quality_db;

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Air quality history table
CREATE TABLE IF NOT EXISTS air_quality_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    aqi INT NOT NULL,
    co DOUBLE NOT NULL,
    no DOUBLE NOT NULL,
    no2 DOUBLE NOT NULL,
    o3 DOUBLE NOT NULL,
    so2 DOUBLE NOT NULL,
    pm2_5 DOUBLE NOT NULL,
    pm10 DOUBLE NOT NULL,
    nh3 DOUBLE NOT NULL,
    searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_air_quality_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Create index for high performance history lookups by user and date
CREATE INDEX idx_user_searched ON air_quality_history(user_id, searched_at DESC);
