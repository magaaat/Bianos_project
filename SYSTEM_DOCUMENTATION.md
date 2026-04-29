# System Documentation: Biano's Pizza Management App

**Date:** October 2023  
**Project Name:** Biano's Pizza Management App  
**Document Version:** 1.0  
**Classification:** Academic / Technical Submission  

---

## 1. PROJECT OVERVIEW

### 1.1 Purpose
**Biano's Pizza Management App** is a specialized mobile solution designed to streamline the operations of pizza-centric food businesses. It serves as an all-in-one ecosystem that integrates a Point-of-Sale (POS) terminal with a dynamic inventory tracking system and a sales intelligence engine. The primary goal is to provide business owners with granular control over their resources while maintaining a fast, consumer-friendly ordering process.

### 1.2 Problem Solved
Small-to-medium pizza outlets often struggle with the disconnect between front-end sales and back-end supplies. Traditional manual ledger-keeping is prone to human error, leading to ingredient shortages (e.g., running out of dough or cheese during peak hours) and unaccounted-revenue. This application automates the "ingredient-to-order" logic, ensuring that every sale accurately reflects in the inventory stock levels.

### 1.3 Key Value Proposition
*   **Efficiency:** Reduces order processing time through an intuitive touch interface.
*   **Accuracy:** Features automated ingredient deduction (e.g., deducting specific grams of cheese per pizza size).
*   **Growth:** Provides data-driven insights through analytics, allowing managers to identify top-performing products and peak sales periods.

---

## 2. USER INTERFACE & FEATURES

### 2.1 POS Terminal (Point of Sale)
The POS interface is designed for high-velocity environments. 
*   **Product Selection:** Users can browse through categories (Pizza, Drinks, Sides) via a smooth navigation bar.
*   **Customization Workflow:** Upon selecting a flavor (e.g., Hawaiian, Pepperoni), a dialog prompts the user to select sizes (9" or 11") and crust types. 
*   **Add-on Logic:** Supports real-time addition of extra toppings (e.g., extra Mozzarella), which dynamically updates the total price and the linked inventory requirements.
*   **Cart Management:** A persistent bottom-bar tracks the number of items and the running total, allowing for quick confirmation and checkout.

### 2.2 Inventory Management
The system features a "Smart-Sync" inventory engine.
*   **Ingredient-Level Tracking:** Unlike standard POS systems, this app tracks raw materials. A single pizza sale triggers a background deduction of specific units (e.g., 1x Dough Ball, 50g Cheese, 30g Sauce).
*   **Automated Alerts:** The system uses a color-coded status indicator:
    *   **Healthy (Green):** Sufficient stock.
    *   **Warning (Orange):** Stock falling below the threshold.
    *   **Critical (Red):** Immediate restock required; system prevents further sales of linked products.

### 2.3 Sales Analytics
The Analytics module transforms raw transaction data into visual business intelligence.
*   **Visual Dashboards:** Utilizing graphical components, the app displays bar charts representing weekly revenue trends.
*   **Performance Metrics:** Displays "Top-Selling Flavors" and "Daily Order Volume" to help managers optimize their menu and staffing.

---

## 3. TECHNICAL SPECIFICATIONS

### 3.1 Tech Stack & Dependencies
*   **IDE:** Android Studio
*   **Languages:** Java (Core Logic), Kotlin (v2.2.10)
*   **UI Framework:** XML with Material Design 3 (com.google.android.material:material:1.13.0)
*   **Database:** SQLite (Local Persistence)
*   **Core Libraries:** 
    *   `androidx.appcompat:appcompat:1.7.1`
    *   `androidx.activity:activity:1.12.4`
    *   `com.github.bumptech.glide:glide:4.16.0` (Image Loading)
    *   `com.github.PhilJay:MPAndroidChart:v3.1.0` (Analytics Charts)
    *   `com.github.f0ris.sweetalert:library:1.6.2` (Feedback Dialogs)
    *   `io.github.muddz:styleabletoast:2.4.0` (UI Notifications)
    *   `com.itextpdf:itext7-core:7.2.5` (PDF Report Generation)
    *   `com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0` (Receipt Printing)
    *   `com.google.firebase:firebase-bom:33.1.1` (Messaging, Analytics, Auth)

### 3.2 Architecture
The application follows a **Modular Monolith** architecture:
1.  **UI Layer:** Handles user interactions and responsive layouts.
2.  **Business Logic Layer:** Manages the complex "Pizza-to-Ingredient" deduction algorithms.
3.  **Data Access Layer:** A robust `DatabaseHelper` class manages the SQLite lifecycle, ensuring data integrity during concurrent read/write operations.

### 3.3 Development Process
The project utilized an **Iterative Design Process**. Development focused heavily on a "Mobile-First" approach, ensuring that touch targets are optimized for fast-paced kitchen environments. Key focus areas included responsive UI design and a card-based menu system.

---

## 4. USER GUIDE

### 4.1 Cashier Instructions (Processing an Order)
1.  **Login:** Enter the authorized credentials to access the POS Terminal.
2.  **Select Item:** Tap a pizza flavor from the menu grid.
3.  **Configure Size:** Choose between 9" and 11" in the popup dialog. Add any requested toppings.
4.  **Review Cart:** Verify the items in the bottom cart bar.
5.  **Checkout:** Tap "Confirm Order." Enter the customer’s name and select "Complete Sale." The inventory will update automatically.

### 4.2 Manager Instructions (Reports & Stock)
1.  **Check Inventory:** Navigate to the "Inventory" tab. View the "Status" column for any "Critical" or "Low Stock" alerts.
2.  **Update Stock:** To restock, tap the "Add Stock" button, select the ingredient, and input the new quantity.
3.  **View Reports:** Navigate to "Sales Analytics." Use the visual charts to view revenue for specific periods.
4.  **Export Data:** Use the export feature to generate professional PDF reports for auditing.

---

## 5. INSTALLATION GUIDE

1.  **Transfer APK:** Copy the `Bianos.apk` file to your Android device’s internal storage.
2.  **Enable Unknown Sources:** Go to **Settings > Security** and enable **"Install Unknown Apps"**.
3.  **Execute Installation:** Open your File Manager, locate `Bianos.apk`, tap it, and select **"Install"**.
4.  **Permissions:** Upon launch, grant permissions for Storage and Bluetooth to enable reports and printing.

---
**End of Documentation**
