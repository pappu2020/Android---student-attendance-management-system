-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 08, 2026 at 09:41 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `student_attendance`
--

-- --------------------------------------------------------

--
-- Table structure for table `attendance`
--

CREATE TABLE `attendance` (
  `id` int(10) UNSIGNED NOT NULL,
  `student_id` int(10) UNSIGNED NOT NULL,
  `attendance_date` date NOT NULL,
  `status` enum('Present','Absent') NOT NULL DEFAULT 'Absent',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `attendance`
--

INSERT INTO `attendance` (`id`, `student_id`, `attendance_date`, `status`, `created_at`) VALUES
(1, 6, '2026-10-07', 'Present', '2026-10-07 17:59:05'),
(2, 5, '2026-10-07', 'Present', '2026-10-07 17:59:05'),
(7, 6, '2026-10-08', 'Present', '2026-10-08 15:13:16'),
(8, 20, '2026-10-08', 'Present', '2026-10-08 15:13:17'),
(9, 5, '2026-10-08', 'Absent', '2026-10-08 15:13:17'),
(10, 6, '2026-10-09', 'Present', '2026-10-08 18:31:31'),
(11, 20, '2026-10-09', 'Present', '2026-10-08 18:31:32'),
(12, 5, '2026-10-09', 'Absent', '2026-10-08 18:31:32'),
(13, 1, '2026-10-09', 'Present', '2026-10-08 19:26:19'),
(14, 2, '2026-10-09', 'Present', '2026-10-08 19:26:19'),
(15, 3, '2026-10-09', 'Present', '2026-10-08 19:26:19');

-- --------------------------------------------------------

--
-- Table structure for table `students`
--

CREATE TABLE `students` (
  `id` int(10) UNSIGNED NOT NULL,
  `student_id` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `roll_number` varchar(30) NOT NULL,
  `class_name` varchar(50) NOT NULL,
  `section` varchar(20) NOT NULL,
  `department` varchar(100) NOT NULL,
  `semester` tinyint(3) UNSIGNED NOT NULL,
  `email` varchar(150) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `photo` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `students`
--

INSERT INTO `students` (`id`, `student_id`, `name`, `roll_number`, `class_name`, `section`, `department`, `semester`, `email`, `phone`, `photo`, `created_at`) VALUES
(1, 'ST001', 'Rahul Das', '101', 'Class 9', 'Section A', 'CSE', 3, 'rahul@gmail.com', '9876543210', NULL, '2026-10-05 06:15:54'),
(2, 'ST002', 'Amit Roy', '102', 'Class 9', 'Section A', 'CSE', 3, 'amit@gmail.com', '9876543211', NULL, '2026-10-05 06:15:54'),
(3, 'ST003', 'Priya Sen', '103', 'Class 9', 'Section A', 'CSE', 3, 'priya@gmail.com', '9876543212', NULL, '2026-10-05 06:15:54'),
(4, 'ST004', 'Suman Paul', '104', 'Class 10', 'Section B', 'CSE', 3, 'suman@gmail.com', '9876543213', NULL, '2026-10-05 06:15:54'),
(5, 'ST005', 'Ananya Das', '105', 'Class 10', 'Section A', 'CSE', 3, 'ananya@gmail.com', '9876543214', NULL, '2026-10-05 06:15:54'),
(6, 'ST006', 'Pappu Saha', '003', 'Class 10', 'Section A', 'CSE', 0, 's@gmail.com', '12345678985', NULL, '2026-10-05 10:18:01'),
(19, '4545', 's', '4545', '', '', 'CSE', 1, 'd@gmail.com', '3332582555', NULL, '2026-10-07 19:06:16'),
(20, '', 'Hiya saha', '012', 'Class 10', 'Section A', '', 0, 'hiya@gmail.com', '6523256', NULL, '2026-10-07 19:19:16');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(10) UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'teacher',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `name`, `email`, `password`, `role`, `created_at`) VALUES
(1, 'Admin', 'admin@gmail.com', '$2y$10$yWjK2yvAD4t6Lmxa1cssWuDKI4NtKmSq7fSwYM4hvBFsGg/rTigdS', 'admin', '2026-10-05 06:33:52');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `attendance`
--
ALTER TABLE `attendance`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_student_attendance` (`student_id`,`attendance_date`);

--
-- Indexes for table `students`
--
ALTER TABLE `students`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `student_id` (`student_id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `attendance`
--
ALTER TABLE `attendance`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `students`
--
ALTER TABLE `students`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `attendance`
--
ALTER TABLE `attendance`
  ADD CONSTRAINT `fk_attendance_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
