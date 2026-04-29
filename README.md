# Biano's Pizza Management App

Biano's Pizza Management App is an all-in-one mobile ecosystem designed for pizza-centric businesses. It streamlines operations by integrating a Point-of-Sale (POS) terminal, dynamic inventory tracking, and a sales intelligence engine.

---

## Key Features

### POS Terminal
* High-Velocity Interface: Browse categories like Pizza, Drinks, and Sides effortlessly.
* Smart Customization: Choose sizes (9" or 11"), crust types, and add-ons like extra Mozzarella.
* Real-Time Calculation: Dynamic price updates and persistent cart management.

### Smart-Sync Inventory
* Ingredient-Level Tracking: Automatically deducts raw materials (e.g., 50g cheese, 1 dough ball) for every sale.
* Status Alerts:
  * Healthy: Sufficient stock.
  * Warning: Low stock threshold.
  * Critical: Immediate restock required (prevents further sales of linked items).

### Sales Analytics
* Visual Dashboards: Bar charts showing weekly revenue trends using MPAndroidChart.
* Performance Metrics: Track top-selling flavors and daily order volumes to optimize menu and staffing.

### Digital Reports and Printing
* PDF Export: Generate professional reports for auditing via iText7.
* Receipt Printing: Bluetooth thermal printer support for customer receipts.

---

## Tech Stack

* Languages: Java (Core Logic), Kotlin (v2.2.10)
* UI: XML with Material Design 3
* Database: SQLite (Local Persistence)
* Backend: Firebase (Messaging, Analytics, Auth)
* Key Libraries:
  * Glide (Image Loading)
  * MPAndroidChart (Analytics)
  * SweetAlert and StyleableToast (UI Feedback)
  * iText7 (PDF Generation)
  * ESCPOS-ThermalPrinter (Receipts)

---

## Installation

1. Download APK: Locate Bianos.apk in the project releases.
2. Enable Unknown Sources: Go to Settings > Security and enable "Install Unknown Apps".
3. Permissions: Grant Storage and Bluetooth permissions upon launch for full functionality.

---

## User Guide

### For Cashiers
1. Login: Enter authorized credentials.
2. Order: Select flavor -> Choose size/toppings -> Add to cart.
3. Checkout: Enter customer name and tap "Complete Sale". Inventory updates automatically.

### For Managers
1. Inventory: Check the status column for alerts. Tap "Add Stock" to replenish.
2. Analytics: View revenue charts to monitor business growth.
3. Reports: Export PDF data for daily or monthly auditing.
