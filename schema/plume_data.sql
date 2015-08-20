-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               5.6.20 - MySQL Community Server (GPL)
-- Server OS:                    Win64
-- HeidiSQL Version:             9.2.0.4947
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping structure for table plume_data.ban
CREATE TABLE IF NOT EXISTS `ban` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(11) unsigned NOT NULL,
  `issue_time` datetime DEFAULT NULL,
  `issue_by` int(10) unsigned DEFAULT NULL,
  `server` varchar(255) DEFAULT NULL,
  `reason` text,
  `heuristic` tinyint(1) NOT NULL DEFAULT '0',
  `expire_time` datetime DEFAULT NULL,
  `pardon_by` int(10) unsigned DEFAULT NULL,
  `pardon_reason` text,
  PRIMARY KEY (`id`),
  KEY `expire_time` (`expire_time`),
  KEY `user_id` (`user_id`),
  KEY `fk_ban_issue_by` (`issue_by`),
  KEY `fk_ban_pardon_by` (`pardon_by`),
  CONSTRAINT `fk_ban_issue_by` FOREIGN KEY (`issue_by`) REFERENCES `user_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ban_pardon_by` FOREIGN KEY (`pardon_by`) REFERENCES `user_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ban_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.claim
CREATE TABLE IF NOT EXISTS `claim` (
  `server` varchar(50) NOT NULL,
  `world` varchar(50) NOT NULL,
  `x` int(11) NOT NULL,
  `z` int(11) NOT NULL,
  `owner_id` int(11) unsigned NOT NULL,
  `party_name` varchar(50) DEFAULT NULL,
  `issue_time` datetime NOT NULL,
  PRIMARY KEY (`server`,`world`,`x`,`z`),
  KEY `owner` (`owner_id`) USING HASH,
  KEY `server_world_owner` (`server`,`world`,`owner_id`) USING HASH,
  KEY `fk_claim_party_name` (`party_name`),
  CONSTRAINT `fk_claim_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `user_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_claim_party_name` FOREIGN KEY (`party_name`) REFERENCES `party` (`name`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.group
CREATE TABLE IF NOT EXISTS `group` (
  `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `permissions` TEXT NOT NULL,
  `auto_join` TINYINT(1) UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name` (`name`),
  INDEX `auto_join` (`auto_join`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.group_parent
CREATE TABLE IF NOT EXISTS `group_parent` (
  `parent_id` int(11) unsigned NOT NULL,
  `group_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`parent_id`,`group_id`),
  KEY `fk_group_parent_group` (`group_id`),
  CONSTRAINT `fk_group_parent_group_id` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_group_parent_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.party
CREATE TABLE IF NOT EXISTS `party` (
  `name` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.party_member
CREATE TABLE IF NOT EXISTS `party_member` (
  `party_name` varchar(50) NOT NULL,
  `user_id` int(11) unsigned NOT NULL,
  `rank` varchar(15) NOT NULL,
  PRIMARY KEY (`party_name`,`user_id`),
  KEY `fk_party_member_user_id` (`user_id`),
  CONSTRAINT `fk_party_member_party_name` FOREIGN KEY (`party_name`) REFERENCES `party` (`name`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_party_member_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.user
CREATE TABLE IF NOT EXISTS `user` (
  `user_id` int(11) unsigned NOT NULL,
  `referrer_id` int(11) unsigned DEFAULT NULL,
  `join_date` datetime DEFAULT NULL,
  `last_online` datetime DEFAULT NULL,
  `host_key` varchar(70) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  KEY `join_date` (`join_date`),
  KEY `fk_user_referrer` (`referrer_id`),
  CONSTRAINT `fk_user_referrer_id` FOREIGN KEY (`referrer_id`) REFERENCES `user_id` (`id`),
  CONSTRAINT `fk_user_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.user_group
CREATE TABLE IF NOT EXISTS `user_group` (
  `user_id` int(11) unsigned NOT NULL,
  `group_id` int(11) unsigned NOT NULL,
  PRIMARY KEY (`user_id`,`group_id`),
  KEY `group` (`group_id`),
  CONSTRAINT `fk_user_group_group_id` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_group_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.


-- Dumping structure for table plume_data.user_id
CREATE TABLE IF NOT EXISTS `user_id` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` char(36) NOT NULL,
  `name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
