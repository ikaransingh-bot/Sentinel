-- MySQL dump 10.13  Distrib 9.7.2, for Win64 (x86_64)
--
-- Host: localhost    Database: sentinel
-- ------------------------------------------------------
-- Server version	9.7.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `sentinel`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sentinel` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `sentinel`;

--
-- Table structure for table `activity_logs`
--

DROP TABLE IF EXISTS `activity_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_logs` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `action` varchar(80) NOT NULL,
  `details` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `fk_activity_logs_user` (`user_id`),
  CONSTRAINT `fk_activity_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity_logs`
--

LOCK TABLES `activity_logs` WRITE;
/*!40000 ALTER TABLE `activity_logs` DISABLE KEYS */;
INSERT INTO `activity_logs` VALUES (1,3,'TICKET_CREATED','Ticket #1 aangemaakt.','2026-10-09 13:00:10'),(2,3,'TECHNICIAN_REQUIRED','Ticket #1 wacht op een technician.','2026-10-09 13:00:20'),(3,3,'TICKET_CREATED','Ticket #2 aangemaakt.','2026-10-09 13:01:59'),(4,3,'TECHNICIAN_REQUIRED','Ticket #2 wacht op een technician.','2026-10-09 13:02:13'),(5,3,'TICKET_CREATED','Ticket #3 aangemaakt.','2026-10-09 13:16:29'),(6,3,'TECHNICIAN_REQUIRED','Ticket #3 wacht op een technician.','2026-10-09 13:16:31');
/*!40000 ALTER TABLE `activity_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `devices`
--

DROP TABLE IF EXISTS `devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `devices` (
  `device_id` int NOT NULL AUTO_INCREMENT,
  `device_name` varchar(100) NOT NULL,
  `device_type` varchar(50) NOT NULL,
  `brand` varchar(50) DEFAULT NULL,
  `model` varchar(100) DEFAULT NULL,
  `operating_system` varchar(100) DEFAULT NULL,
  `serial_number` varchar(100) DEFAULT NULL,
  `assigned_user_id` int DEFAULT NULL,
  `status` varchar(20) NOT NULL COMMENT 'ACTIVE, INACTIVE or REPAIR',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `serial_number` (`serial_number`),
  KEY `assigned_user_id` (`assigned_user_id`),
  CONSTRAINT `fk_device_user` FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `devices`
--

LOCK TABLES `devices` WRITE;
/*!40000 ALTER TABLE `devices` DISABLE KEYS */;
INSERT INTO `devices` VALUES (2,'Dell latitude 5420','LAPTOP','Lenovo','Thinkpad L14','Windows 11 pro plus','xxccbncjm1123',NULL,'ACTIVE','2026-10-08 15:40:45');
/*!40000 ALTER TABLE `devices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `diagnostic_results`
--

DROP TABLE IF EXISTS `diagnostic_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diagnostic_results` (
  `diagnostic_id` int NOT NULL AUTO_INCREMENT,
  `ticket_id` int NOT NULL,
  `diagnostic_type` varchar(50) NOT NULL COMMENT 'NETWORK, PRINTER, SYSTEM, STORAGE, SECURITY, UPDATE, OFFICE, OTHER',
  `result` varchar(30) NOT NULL COMMENT 'OK, WARNING, ERROR, TECHNICIAN_REQUIRED',
  `details` text,
  `recommendation` text,
  `performed_at` datetime NOT NULL,
  PRIMARY KEY (`diagnostic_id`),
  KEY `ticket_id` (`ticket_id`),
  CONSTRAINT `diagnostic_results_ibfk_1` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`ticket_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diagnostic_results`
--

LOCK TABLES `diagnostic_results` WRITE;
/*!40000 ALTER TABLE `diagnostic_results` DISABLE KEYS */;
INSERT INTO `diagnostic_results` VALUES (1,1,'STORAGE','OK','De vrije opslagruimte lijkt normaal. | Totaal: 929 GB, vrij: 823 GB, vrij percentage: 88.5%.','Geen directe opslagactie nodig.','2026-10-09 13:00:20'),(2,1,'NETWORK','OK','Basis netwerkverbinding is bereikbaar. | IPv4-adres: 172.31.240.1. Externe bereikbaarheidstest is gelukt.','Geen duidelijke netwerkfout gevonden.','2026-10-09 13:00:20'),(3,1,'PRINTER','OK','Windows ziet een of meer printers. | Printers: OneNote (Desktop), Microsoft Print to PDF, \\\\QSR-VPN-SERVER\\DBA-Pagewide','Als printen toch niet werkt moet een technician wachtrij, driver of verbinding controleren.','2026-10-09 13:00:20'),(4,1,'SYSTEM','OK','De gemeten systeemresources lijken normaal. | Vrij RAM: 55.9 procent. CPU load: 5 procent.','Geen duidelijke resourcefout gevonden.','2026-10-09 13:00:20'),(5,1,'SECURITY','WARNING','Een beveiligingsonderdeel lijkt niet actief. | AntivirusEnabled=True, RealTimeProtectionEnabled=False','Laat een technician controleren waarom de bescherming niet volledig actief is.','2026-10-09 13:00:20'),(6,1,'UPDATE','WARNING','Windows meldt dat een herstart nodig kan zijn. | Windows Update service: Stopped. Reboot pending=True.','Sla werk op en plan een herstart. SENTINEL installeert geen updates.','2026-10-09 13:00:20'),(7,1,'OFFICE','WARNING','Office meldt geen duidelijke LICENSED status. | De lokale Office licentiecontrole gaf geen LICENSED resultaat.','Laat een technician de Office activatie en licentie controleren.','2026-10-09 13:00:20'),(8,2,'STORAGE','OK','De vrije opslagruimte lijkt normaal. | Totaal: 929 GB, vrij: 823 GB, vrij percentage: 88.5%.','Geen directe opslagactie nodig.','2026-10-09 13:02:01'),(9,2,'SYSTEM','OK','De gemeten systeemresources lijken normaal. | Vrij RAM: 55.9 procent. CPU load: 7 procent.','Geen duidelijke resourcefout gevonden.','2026-10-09 13:02:01'),(10,3,'SYSTEM','OK','De gemeten systeemresources lijken normaal. | Vrij RAM: 55.3 procent. CPU load: 0 procent.','Geen duidelijke resourcefout gevonden.','2026-10-09 13:16:31'),(11,3,'OTHER','TECHNICIAN_REQUIRED','Geen betrouwbare automatische diagnose beschikbaar. | SENTINEL heeft alleen een algemene systeemcontrole uitgevoerd.','Een technician moet dit probleem verder onderzoeken.','2026-10-09 13:16:31');
/*!40000 ALTER TABLE `diagnostic_results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(30) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Example roles: TECHNICIAN, EMPLOYEE, ADMIN';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'ADMIN'),(3,'EMPLOYEE'),(2,'TECHNICIAN');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tickets`
--

DROP TABLE IF EXISTS `tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tickets` (
  `ticket_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT 'User who created/reported the ticket',
  `device_id` int DEFAULT NULL,
  `assigned_to` int DEFAULT NULL COMMENT 'Technician assigned to the ticket',
  `title` varchar(150) NOT NULL,
  `description` text NOT NULL,
  `category` varchar(50) NOT NULL COMMENT 'NETWORK, PRINTER, SLOW, STORAGE, WINDOWS_UPDATE, SECURITY, OFFICE, OTHER, FULL_CHECK',
  `priority` varchar(20) NOT NULL COMMENT 'LOW, MEDIUM, HIGH',
  `status` varchar(30) NOT NULL COMMENT 'OPEN, IN_PROGRESS, TECHNICIAN_REQUIRED, RESOLVED',
  `resolution` text,
  `created_at` datetime NOT NULL,
  `closed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`ticket_id`),
  KEY `user_id` (`user_id`),
  KEY `assigned_to` (`assigned_to`),
  KEY `device_id` (`device_id`),
  CONSTRAINT `tickets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `tickets_ibfk_2` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`user_id`),
  CONSTRAINT `tickets_ibfk_3` FOREIGN KEY (`device_id`) REFERENCES `devices` (`device_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tickets`
--

LOCK TABLES `tickets` WRITE;
/*!40000 ALTER TABLE `tickets` DISABLE KEYS */;
INSERT INTO `tickets` VALUES (1,3,NULL,NULL,'Volledige systeemcontrole','Volledige controle gestart door Employee.','FULL_CHECK','LOW','TECHNICIAN_REQUIRED',NULL,'2026-10-09 13:00:10',NULL),(2,3,NULL,1,'Trage laptop','Waneer ik windows start load hij echt traag terwijl hij voorheen snel was','SLOW','MEDIUM','RESOLVED','TEMP FILES CLEANED','2026-10-09 13:01:59','2026-10-09 13:17:47'),(3,3,NULL,NULL,'niks','niks','OTHER','LOW','TECHNICIAN_REQUIRED',NULL,'2026-10-09 13:16:29',NULL);
/*!40000 ALTER TABLE `tickets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `first_login_completed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0 = user must create own password, 1 = password already created',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,1,'Ishaan','ishaankaransingh9@gmail.com','120000:z+RgferasZLI0OifoRggrQ==:ciG2kdR0AJFO9+z//Zqs1PyKT3fxjPgcHllRFJvqhyk=','Ishaan Karansingh',1,1,'2026-10-08 14:18:06'),(2,2,'Harold','harold@sentinel.com','120000:xnUOuV44bnYseBdOyTVPlw==:WBV0Br2hXRYDSE6WRu2MQvNH25GkZzz/Kxow9eja45A=','Harold Berhinger',1,1,'2026-10-08 14:25:02'),(3,3,'Aryan','Aryan@sentinel.employee.com','120000:msVdettOWHZRFBpKiVh+Mw==:pTI7jymrZ9oYmZfE2gxBRU9qyx5fab7OESUX7Y/51Gw=','Aryan Jiawan',1,1,'2026-10-08 14:58:06');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'sentinel'
--

--
-- Dumping routines for database 'sentinel'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-10 20:36:26
