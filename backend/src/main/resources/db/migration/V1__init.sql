/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80300 (8.3.0)
 Source Host           : localhost:3306
 Source Schema         : ees

 Target Server Type    : MySQL
 Target Server Version : 80300 (8.3.0)
 File Encoding         : 65001

 Date: 20/12/2025 01:57:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for accounts
-- ----------------------------
DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `account_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `parent_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `hidden` tinyint(1) NULL DEFAULT 0,
  `placeholder` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_accounts_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_accounts_parent`(`parent_guid` ASC) USING BTREE,
  INDEX `idx_accounts_type`(`account_type` ASC) USING BTREE,
  INDEX `idx_accounts_code`(`code` ASC) USING BTREE,
  CONSTRAINT `fk_accounts_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_accounts_parent` FOREIGN KEY (`parent_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_accounts_type` FOREIGN KEY (`account_type`) REFERENCES `enums_account_type` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of accounts
-- ----------------------------

-- ----------------------------
-- Table structure for books
-- ----------------------------
DROP TABLE IF EXISTS `books`;
CREATE TABLE `books`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `size` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `registered_capital_num` bigint NULL DEFAULT NULL,
  `registered_capital_denom` bigint NULL DEFAULT NULL,
  `root_account_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fiscal_year_start_month` int NULL DEFAULT NULL,
  `fiscal_year_start_day` int NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_books_root_account`(`root_account_guid` ASC) USING BTREE,
  CONSTRAINT `fk_books_root_account` FOREIGN KEY (`root_account_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of books
-- ----------------------------

-- ----------------------------
-- Table structure for customers
-- ----------------------------
DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tax_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_customers_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_customers_name`(`name` ASC) USING BTREE,
  CONSTRAINT `fk_customers_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of customers
-- ----------------------------

-- ----------------------------
-- Table structure for employees
-- ----------------------------
DROP TABLE IF EXISTS `employees`;
CREATE TABLE `employees`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tax_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_employees_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_employees_name`(`name` ASC) USING BTREE,
  CONSTRAINT `fk_employees_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of employees
-- ----------------------------

-- ----------------------------
-- Table structure for entries
-- ----------------------------
DROP TABLE IF EXISTS `entries`;
CREATE TABLE `entries`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `invoice_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `job_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `date` datetime NULL DEFAULT NULL,
  `quantity_num` bigint NOT NULL,
  `quantity_denom` bigint NOT NULL,
  `price_num` bigint NOT NULL,
  `price_denom` bigint NOT NULL,
  `discount_num` bigint NULL DEFAULT NULL,
  `discount_denom` bigint NULL DEFAULT NULL,
  `account_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tax_table_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `taxable` tinyint(1) NULL DEFAULT 1,
  `tax_included` tinyint(1) NULL DEFAULT 0,
  `tax_amount_num` bigint NULL DEFAULT NULL,
  `tax_amount_denom` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_entries_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_entries_invoice`(`invoice_guid` ASC) USING BTREE,
  INDEX `idx_entries_order`(`order_guid` ASC) USING BTREE,
  INDEX `idx_entries_job`(`job_guid` ASC) USING BTREE,
  INDEX `idx_entries_account`(`account_guid` ASC) USING BTREE,
  INDEX `idx_entries_tax_table`(`tax_table_guid` ASC) USING BTREE,
  CONSTRAINT `fk_entries_account` FOREIGN KEY (`account_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_entries_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_entries_invoice` FOREIGN KEY (`invoice_guid`) REFERENCES `invoices` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_entries_job` FOREIGN KEY (`job_guid`) REFERENCES `jobs` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_entries_order` FOREIGN KEY (`order_guid`) REFERENCES `orders` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_entries_tax_table` FOREIGN KEY (`tax_table_guid`) REFERENCES `taxtables` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of entries
-- ----------------------------

-- ----------------------------
-- Table structure for enums_account_type
-- ----------------------------
DROP TABLE IF EXISTS `enums_account_type`;
CREATE TABLE `enums_account_type`  (
  `value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enums_account_type
-- ----------------------------
INSERT INTO `enums_account_type` VALUES ('ASSET');
INSERT INTO `enums_account_type` VALUES ('EQUITY');
INSERT INTO `enums_account_type` VALUES ('EXPENSE');
INSERT INTO `enums_account_type` VALUES ('INCOME');
INSERT INTO `enums_account_type` VALUES ('LIABILITY');

-- ----------------------------
-- Table structure for enums_doc_status
-- ----------------------------
DROP TABLE IF EXISTS `enums_doc_status`;
CREATE TABLE `enums_doc_status`  (
  `value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enums_doc_status
-- ----------------------------
INSERT INTO `enums_doc_status` VALUES ('APPROVED');
INSERT INTO `enums_doc_status` VALUES ('DRAFT');
INSERT INTO `enums_doc_status` VALUES ('POSTED');
INSERT INTO `enums_doc_status` VALUES ('VOID');

-- ----------------------------
-- Table structure for enums_invoice_type
-- ----------------------------
DROP TABLE IF EXISTS `enums_invoice_type`;
CREATE TABLE `enums_invoice_type`  (
  `value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enums_invoice_type
-- ----------------------------
INSERT INTO `enums_invoice_type` VALUES ('EXPENSE');
INSERT INTO `enums_invoice_type` VALUES ('PURCHASE');
INSERT INTO `enums_invoice_type` VALUES ('SALES');

-- ----------------------------
-- Table structure for enums_owner_type
-- ----------------------------
DROP TABLE IF EXISTS `enums_owner_type`;
CREATE TABLE `enums_owner_type`  (
  `value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enums_owner_type
-- ----------------------------
INSERT INTO `enums_owner_type` VALUES ('CUSTOMER');
INSERT INTO `enums_owner_type` VALUES ('EMPLOYEE');
INSERT INTO `enums_owner_type` VALUES ('VENDOR');

-- ----------------------------
-- Table structure for enums_reconcile_state
-- ----------------------------
DROP TABLE IF EXISTS `enums_reconcile_state`;
CREATE TABLE `enums_reconcile_state`  (
  `value` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enums_reconcile_state
-- ----------------------------
INSERT INTO `enums_reconcile_state` VALUES ('C');
INSERT INTO `enums_reconcile_state` VALUES ('N');
INSERT INTO `enums_reconcile_state` VALUES ('Y');

-- ----------------------------
-- Table structure for enums_tax_direction
-- ----------------------------
DROP TABLE IF EXISTS `enums_tax_direction`;
CREATE TABLE `enums_tax_direction`  (
  `value` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of enums_tax_direction
-- ----------------------------
INSERT INTO `enums_tax_direction` VALUES ('INPUT');
INSERT INTO `enums_tax_direction` VALUES ('OUTPUT');

-- ----------------------------
-- Table structure for invoices
-- ----------------------------
DROP TABLE IF EXISTS `invoices`;
CREATE TABLE `invoices`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `job_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `invoice_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `date_opened` datetime NOT NULL,
  `date_posted` datetime NULL DEFAULT NULL,
  `due_date` datetime NULL DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
  `post_txn_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `lot_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_invoices_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_invoices_owner`(`owner_guid` ASC) USING BTREE,
  INDEX `idx_invoices_job`(`job_guid` ASC) USING BTREE,
  INDEX `idx_invoices_type`(`invoice_type` ASC) USING BTREE,
  INDEX `idx_invoices_status`(`status` ASC) USING BTREE,
  INDEX `idx_invoices_opened`(`date_opened` ASC) USING BTREE,
  INDEX `idx_invoices_post_txn`(`post_txn_guid` ASC) USING BTREE,
  INDEX `idx_invoices_lot`(`lot_guid` ASC) USING BTREE,
  CONSTRAINT `fk_invoices_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_invoices_job` FOREIGN KEY (`job_guid`) REFERENCES `jobs` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_invoices_lot` FOREIGN KEY (`lot_guid`) REFERENCES `lots` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_invoices_owner` FOREIGN KEY (`owner_guid`) REFERENCES `owner` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_invoices_post_txn` FOREIGN KEY (`post_txn_guid`) REFERENCES `transactions` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_invoices_status` FOREIGN KEY (`status`) REFERENCES `enums_doc_status` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_invoices_type` FOREIGN KEY (`invoice_type`) REFERENCES `enums_invoice_type` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of invoices
-- ----------------------------

-- ----------------------------
-- Table structure for jobs
-- ----------------------------
DROP TABLE IF EXISTS `jobs`;
CREATE TABLE `jobs`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `active` tinyint(1) NULL DEFAULT 1,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_jobs_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_jobs_owner`(`owner_guid` ASC) USING BTREE,
  INDEX `idx_jobs_name`(`name` ASC) USING BTREE,
  CONSTRAINT `fk_jobs_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_jobs_owner` FOREIGN KEY (`owner_guid`) REFERENCES `owner` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of jobs
-- ----------------------------

-- ----------------------------
-- Table structure for lots
-- ----------------------------
DROP TABLE IF EXISTS `lots`;
CREATE TABLE `lots`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `account_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_closed` tinyint(1) NULL DEFAULT 0,
  `closed_date` datetime NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_lots_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_lots_account`(`account_guid` ASC) USING BTREE,
  INDEX `idx_lots_closed`(`is_closed` ASC) USING BTREE,
  CONSTRAINT `fk_lots_account` FOREIGN KEY (`account_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_lots_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lots
-- ----------------------------

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `job_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `date_opened` datetime NOT NULL,
  `date_closed` datetime NULL DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_orders_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_orders_owner`(`owner_guid` ASC) USING BTREE,
  INDEX `idx_orders_job`(`job_guid` ASC) USING BTREE,
  INDEX `idx_orders_type`(`order_type` ASC) USING BTREE,
  INDEX `idx_orders_status`(`status` ASC) USING BTREE,
  INDEX `idx_orders_opened`(`date_opened` ASC) USING BTREE,
  CONSTRAINT `fk_orders_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_job` FOREIGN KEY (`job_guid`) REFERENCES `jobs` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_owner` FOREIGN KEY (`owner_guid`) REFERENCES `owner` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_status` FOREIGN KEY (`status`) REFERENCES `enums_doc_status` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_type` FOREIGN KEY (`order_type`) REFERENCES `enums_invoice_type` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------

-- ----------------------------
-- Table structure for owner
-- ----------------------------
DROP TABLE IF EXISTS `owner`;
CREATE TABLE `owner`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `customer_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `vendor_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `employee_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_owner_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_owner_type`(`owner_type` ASC) USING BTREE,
  INDEX `fk_owner_customer`(`customer_guid` ASC) USING BTREE,
  INDEX `fk_owner_vendor`(`vendor_guid` ASC) USING BTREE,
  INDEX `fk_owner_employee`(`employee_guid` ASC) USING BTREE,
  CONSTRAINT `fk_owner_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_owner_customer` FOREIGN KEY (`customer_guid`) REFERENCES `customers` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_owner_employee` FOREIGN KEY (`employee_guid`) REFERENCES `employees` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_owner_type` FOREIGN KEY (`owner_type`) REFERENCES `enums_owner_type` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_owner_vendor` FOREIGN KEY (`vendor_guid`) REFERENCES `vendors` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of owner
-- ----------------------------

-- ----------------------------
-- Table structure for recurrences
-- ----------------------------
DROP TABLE IF EXISTS `recurrences`;
CREATE TABLE `recurrences`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sx_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `freq` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `interval_val` int NOT NULL DEFAULT 1,
  `by_day` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `by_month_day` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_recurrences_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_recurrences_sx`(`sx_guid` ASC) USING BTREE,
  CONSTRAINT `fk_recurrences_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_recurrences_sx` FOREIGN KEY (`sx_guid`) REFERENCES `schedxactions` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of recurrences
-- ----------------------------

-- ----------------------------
-- Table structure for schedxactions
-- ----------------------------
DROP TABLE IF EXISTS `schedxactions`;
CREATE TABLE `schedxactions`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `enabled` tinyint(1) NULL DEFAULT 1,
  `start_date` datetime NOT NULL,
  `end_date` datetime NULL DEFAULT NULL,
  `last_occur` datetime NULL DEFAULT NULL,
  `next_occur` datetime NULL DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_sx_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_sx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_sx_next_occur`(`next_occur` ASC) USING BTREE,
  CONSTRAINT `fk_sx_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of schedxactions
-- ----------------------------

-- ----------------------------
-- Table structure for splits
-- ----------------------------
DROP TABLE IF EXISTS `splits`;
CREATE TABLE `splits`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tx_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `account_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `value_num` bigint NOT NULL,
  `value_denom` bigint NOT NULL,
  `quantity_num` bigint NOT NULL,
  `quantity_denom` bigint NOT NULL,
  `memo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reconcile_state` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'N',
  `reconcile_date` datetime NULL DEFAULT NULL,
  `lot_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_splits_tx`(`tx_guid` ASC) USING BTREE,
  INDEX `idx_splits_account`(`account_guid` ASC) USING BTREE,
  INDEX `idx_splits_reconcile`(`reconcile_state` ASC) USING BTREE,
  INDEX `idx_splits_lot`(`lot_guid` ASC) USING BTREE,
  CONSTRAINT `fk_splits_account` FOREIGN KEY (`account_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_splits_lot` FOREIGN KEY (`lot_guid`) REFERENCES `lots` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_splits_reconcile` FOREIGN KEY (`reconcile_state`) REFERENCES `enums_reconcile_state` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_splits_tx` FOREIGN KEY (`tx_guid`) REFERENCES `transactions` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of splits
-- ----------------------------

-- ----------------------------
-- Table structure for sx_splits
-- ----------------------------
DROP TABLE IF EXISTS `sx_splits`;
CREATE TABLE `sx_splits`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sx_tx_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `account_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `value_num` bigint NOT NULL,
  `value_denom` bigint NOT NULL,
  `quantity_num` bigint NOT NULL,
  `quantity_denom` bigint NOT NULL,
  `memo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_sx_splits_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_sx_splits_tx`(`sx_tx_guid` ASC) USING BTREE,
  INDEX `idx_sx_splits_account`(`account_guid` ASC) USING BTREE,
  CONSTRAINT `fk_sx_splits_account` FOREIGN KEY (`account_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sx_splits_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sx_splits_tx` FOREIGN KEY (`sx_tx_guid`) REFERENCES `sx_transactions` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sx_splits
-- ----------------------------

-- ----------------------------
-- Table structure for sx_transactions
-- ----------------------------
DROP TABLE IF EXISTS `sx_transactions`;
CREATE TABLE `sx_transactions`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sx_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_sx_tx_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_sx_tx_sx`(`sx_guid` ASC) USING BTREE,
  CONSTRAINT `fk_sx_tx_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sx_tx_sx` FOREIGN KEY (`sx_guid`) REFERENCES `schedxactions` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sx_transactions
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_books
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_books`;
CREATE TABLE `sys_user_books`  (
  `user_id` bigint NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `book_guid`(`book_guid` ASC) USING BTREE,
  CONSTRAINT `fk_sub_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sub_user` FOREIGN KEY (`user_id`) REFERENCES `sys_users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_books
-- ----------------------------

-- ----------------------------
-- Table structure for sys_users
-- ----------------------------
DROP TABLE IF EXISTS `sys_users`;
CREATE TABLE `sys_users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_users
-- ----------------------------

-- ----------------------------
-- Table structure for taxable_entries
-- ----------------------------
DROP TABLE IF EXISTS `taxable_entries`;
CREATE TABLE `taxable_entries`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `entry_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tax_table_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tax_amount_num` bigint NOT NULL,
  `tax_amount_denom` bigint NOT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_taxable_entries_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_taxable_entries_entry`(`entry_guid` ASC) USING BTREE,
  INDEX `idx_taxable_entries_tax_table`(`tax_table_guid` ASC) USING BTREE,
  CONSTRAINT `fk_taxable_entries_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_taxable_entries_entry` FOREIGN KEY (`entry_guid`) REFERENCES `entries` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_taxable_entries_tax_table` FOREIGN KEY (`tax_table_guid`) REFERENCES `taxtables` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of taxable_entries
-- ----------------------------

-- ----------------------------
-- Table structure for taxtables
-- ----------------------------
DROP TABLE IF EXISTS `taxtables`;
CREATE TABLE `taxtables`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `direction` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `rate_num` bigint NOT NULL,
  `rate_denom` bigint NOT NULL,
  `payable_account_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `active` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_taxtables_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_taxtables_direction`(`direction` ASC) USING BTREE,
  INDEX `idx_taxtables_active`(`active` ASC) USING BTREE,
  INDEX `fk_taxtables_payable_acct`(`payable_account_guid` ASC) USING BTREE,
  CONSTRAINT `fk_taxtables_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_taxtables_direction` FOREIGN KEY (`direction`) REFERENCES `enums_tax_direction` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_taxtables_payable_acct` FOREIGN KEY (`payable_account_guid`) REFERENCES `accounts` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of taxtables
-- ----------------------------

-- ----------------------------
-- Table structure for transactions
-- ----------------------------
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `num` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `post_date` datetime NOT NULL,
  `enter_date` datetime NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `doc_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'POSTED',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_tx_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_tx_post_date`(`post_date` ASC) USING BTREE,
  INDEX `idx_tx_status`(`doc_status` ASC) USING BTREE,
  INDEX `idx_tx_source`(`source_type` ASC, `source_guid` ASC) USING BTREE,
  CONSTRAINT `fk_tx_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_tx_status` FOREIGN KEY (`doc_status`) REFERENCES `enums_doc_status` (`value`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of transactions
-- ----------------------------

-- ----------------------------
-- Table structure for vendors
-- ----------------------------
DROP TABLE IF EXISTS `vendors`;
CREATE TABLE `vendors`  (
  `guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `book_guid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tax_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`guid`) USING BTREE,
  INDEX `idx_vendors_book`(`book_guid` ASC) USING BTREE,
  INDEX `idx_vendors_name`(`name` ASC) USING BTREE,
  CONSTRAINT `fk_vendors_book` FOREIGN KEY (`book_guid`) REFERENCES `books` (`guid`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vendors
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
