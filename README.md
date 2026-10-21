# BakeStock — Offline Bakery & Grocery Inventory & Production Management System

**BakeStock** is a comprehensive, production-grade, offline-first Android application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **Room Database (SQLite)**. It is specifically designed for commercial bakeries, pastry kitchens, commissaries, and food manufacturing enterprises.

---

## 🌟 Key Capabilities & Features

### 1. Role-Based Access Control (RBAC)
* **Bakery Admin**: Full system control — inventory management, purchases, supplier directory, employee master accounts, automated 7-day velocity forecasting, wastage monitoring, CSV/JSON report exports, and local database snapshot backups.
* **Kitchen Master**: Recipe book with interactive scaling (1x, 2x, 5x, 10x, custom), atomic production batch execution with automatic raw material stock deduction, ingredient shortages check, and store ingredient requests.
* **Delivery Staff**: Assigned ingredient requests, grocery procurement checklists, and store collection handovers with quantity verification.

### 2. Strict Offline-First Architecture
* **100% Local Execution**: Powered entirely by **Room SQLite Database**. Operates in kitchen basements, walk-in freezers, and areas without active Wi-Fi or cellular connections.
* **Zero Cloud Dependency**: Runs without requiring remote Firebase or third-party servers to guarantee zero downtime.

### 3. Smart Production & Atomic Stock Deduction
* **Atomic Room Transaction**: When a production batch is completed, required recipe ingredients are dynamically scaled and deducted atomically from raw material stock in SQLite, while finished bakery items are automatically incremented.
* **Insufficient Stock Guard**: Validates ingredient availability prior to execution and prevents negative stock.

### 4. Predictive 7-Day Forecasting & Reordering
* Analyzes past 7-day ingredient consumption and production history to calculate daily burn rate and estimate days until stock depletion.
* Generates proactive reorder suggestions with calculated reorder amounts.

### 5. Audit Reports & Snapshot Backups
* Generates on-demand **CSV reports** (Purchase Summaries, Low Stock & Reorders, Expiry & FEFO audits, Production Wastage).
* Full **JSON database snapshots** saved directly to device storage (`/files/reports/` and `/files/backups/`) with built-in viewer and recovery tools.

---

## 👥 Default Demo Credentials

You can select any role directly from the **Role Selection** screen or sign in with the following pre-configured credentials:

| Role | Username / Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin@2026` | Full Bakery Administration & System Control |
| **Kitchen Master** | `kitchen@bakestock.com` | `kitchen123` | Kitchen Dashboard, Recipes, Production Batches |
| **Delivery Staff** | `delivery@bakestock.com` | `delivery123` | Dispatch Requests, Item Collection Checklists |

---

## 🛠️ Technology Stack & Libraries

* **Language**: Kotlin 2.0+
* **UI Toolkit**: Jetpack Compose (Declarative Android UI)
* **Design System**: Material Design 3 (M3) with Bakery Warm Amber/Espresso color palette and accessible contrast
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Local Persistence**: Android Jetpack Room 2.7+ (SQLite with KSP compiler, Room DAOs, and Type Converters)
* **Concurrency & Flow**: Kotlin Coroutines (`Dispatchers.IO`) & `StateFlow`
* **Navigation**: Jetpack Navigation Compose with type-safe routing

---

## 📦 How to Download & Run in Android Studio

1. **Download Project ZIP**:
   * Open the project menu or export options in AI Studio and download the project archive (e.g., `BakeStock.zip`).
2. **Open in Android Studio**:
   * Extract the ZIP archive to your local drive.
   * Launch **Android Studio Ladybug (or newer)**.
   * Select **File -> Open...** and navigate to the extracted folder.
3. **Sync & Build**:
   * Let Gradle sync dependencies automatically.
   * Connect an Android device (Android 7.0 / API 24+) or launch an Android Virtual Device (AVD).
   * Click **Run (Shift + F10)**.
