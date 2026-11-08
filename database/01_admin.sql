-- Database Initialization & Admin Seeding Script
CREATE DATABASE IF NOT EXISTS travel_booking_db;
USE travel_booking_db;

-- 1. Safely drop existing tables in reverse order of foreign key dependencies
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS hotels;
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. User authentication table
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Pre-seeded default admin account (REQUIRED)
INSERT INTO users (username, password, role)
VALUES ('admin', 'admin123', 'ADMIN');

-- 3. Flight listings table
CREATE TABLE flights (
    flight_id VARCHAR(20) PRIMARY KEY,
    departure VARCHAR(100) NOT NULL,
    departure_time VARCHAR(5) NOT NULL,
    arrival VARCHAR(100) NOT NULL,
    arrival_time VARCHAR(5) NOT NULL,
    price DOUBLE NOT NULL
);

-- 4. Hotel listings table
CREATE TABLE hotels (
    hotel_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    rating DOUBLE NOT NULL,
    price_per_night DOUBLE NOT NULL
);

-- 5. Bookings table
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    flight_id VARCHAR(20),
    hotel_id VARCHAR(20),
    total_price DOUBLE NOT NULL,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(flight_id) ON DELETE SET NULL,
    FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE SET NULL
);
