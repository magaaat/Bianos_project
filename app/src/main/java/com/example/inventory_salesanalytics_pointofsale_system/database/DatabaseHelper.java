package com.example.inventory_salesanalytics_pointofsale_system.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.inventory_salesanalytics_pointofsale_system.models.Product;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryLog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "BianosPizza.db";
    private static final int DATABASE_VERSION = 27; 

    public static final String TABLE_ADMIN = "admin";
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_INVENTORY = "inventory";
    public static final String TABLE_SALES = "sales";
    public static final String TABLE_ORDER_DETAILS = "order_details";
    public static final String TABLE_PRODUCT_INGREDIENTS = "product_ingredients";
    public static final String TABLE_INVENTORY_LOGS = "inventory_logs";

    public static final String KEY_ID = "id";
    public static final String KEY_ADMIN_USERNAME = "username";
    public static final String KEY_ADMIN_PASSWORD = "password";
    public static final String KEY_ADMIN_EMAIL = "email";
    public static final String KEY_ADMIN_IMAGE = "profile_image";

    public static final String KEY_PRODUCT_NAME = "product_name";
    public static final String KEY_PRODUCT_CATEGORY = "category";
    public static final String KEY_PRODUCT_PRICE = "price";
    public static final String KEY_PRODUCT_SIZE = "size";
    public static final String KEY_PRODUCT_IMAGE = "image_url";

    public static final String KEY_INV_ITEM_NAME = "item_name";
    public static final String KEY_INV_QUANTITY = "quantity"; 
    public static final String KEY_INV_UNIT = "unit";
    public static final String KEY_INV_MIN_STOCK = "minimum_stock";

    public static final String KEY_SALE_DATE = "sale_date";
    public static final String KEY_SALE_TOTAL_AMOUNT = "total_amount";
    public static final String KEY_SALE_CUSTOMER_NAME = "customer_name";

    public static final String KEY_OD_SALE_ID = "sale_id";
    public static final String KEY_OD_PRODUCT_ID = "product_id";
    public static final String KEY_OD_QUANTITY = "quantity";
    public static final String KEY_OD_SUBTOTAL = "subtotal";

    public static final String KEY_PI_PRODUCT_ID = "product_id";
    public static final String KEY_PI_INGREDIENT_NAME = "ingredient_name";

    public static final String KEY_LOG_ITEM_ID = "item_id";
    public static final String KEY_LOG_TYPE = "log_type"; 
    public static final String KEY_LOG_QUANTITY = "log_quantity";
    public static final String KEY_LOG_REASON = "reason";
    public static final String KEY_LOG_DATE = "log_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ADMIN + "(" + 
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
                KEY_ADMIN_USERNAME + " TEXT," + 
                KEY_ADMIN_PASSWORD + " TEXT," + 
                KEY_ADMIN_EMAIL + " TEXT," + 
                KEY_ADMIN_IMAGE + " TEXT)");
        
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_PRODUCT_NAME + " TEXT," + KEY_PRODUCT_CATEGORY + " TEXT," + KEY_PRODUCT_PRICE + " REAL," + KEY_PRODUCT_SIZE + " TEXT," + KEY_PRODUCT_IMAGE + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_INVENTORY + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_INV_ITEM_NAME + " TEXT," + KEY_INV_QUANTITY + " REAL," + KEY_INV_UNIT + " TEXT," + KEY_INV_MIN_STOCK + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_SALES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_SALE_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP," + KEY_SALE_TOTAL_AMOUNT + " REAL," + KEY_SALE_CUSTOMER_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_ORDER_DETAILS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_OD_SALE_ID + " INTEGER," + KEY_OD_PRODUCT_ID + " INTEGER," + KEY_OD_QUANTITY + " INTEGER," + KEY_OD_SUBTOTAL + " REAL)");
        db.execSQL("CREATE TABLE " + TABLE_PRODUCT_INGREDIENTS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_PI_PRODUCT_ID + " INTEGER," + KEY_PI_INGREDIENT_NAME + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_INVENTORY_LOGS + "(" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_LOG_ITEM_ID + " INTEGER," +
                KEY_LOG_TYPE + " TEXT," +
                KEY_LOG_QUANTITY + " REAL," +
                KEY_LOG_REASON + " TEXT," +
                KEY_LOG_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP)");

        ContentValues values = new ContentValues();
        values.put(KEY_ADMIN_USERNAME, "admin");
        values.put(KEY_ADMIN_PASSWORD, "admin123");
        values.put(KEY_ADMIN_EMAIL, "admin@example.com");
        db.insert(TABLE_ADMIN, null, values);
    }

    public boolean isProductAvailable(Product product) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        if (!"Pizza".equalsIgnoreCase(product.getCategory())) {
            Cursor cursor = db.rawQuery("SELECT " + KEY_INV_QUANTITY + " FROM " + TABLE_INVENTORY + 
                    " WHERE " + KEY_INV_ITEM_NAME + " = ?", new String[]{product.getName()});
            boolean available = true; // Default to true if not found in inventory
            if (cursor.moveToFirst()) {
                available = cursor.getDouble(0) > 0.01; // Using a small threshold instead of 1.0
            }
            cursor.close();
            return available;
        }

        // Check Dough (Size specific)
        String doughName = "Dough " + product.getSize().replace("\"", "");
        Cursor doughCursor = db.rawQuery("SELECT " + KEY_INV_QUANTITY + " FROM " + TABLE_INVENTORY + 
                " WHERE " + KEY_INV_ITEM_NAME + " = ?", new String[]{doughName});
        if (doughCursor.moveToFirst() && doughCursor.getDouble(0) <= 0.01) {
            doughCursor.close();
            return false;
        }
        doughCursor.close();

        // Check Cheese
        Cursor cheeseCursor = db.rawQuery("SELECT " + KEY_INV_QUANTITY + " FROM " + TABLE_INVENTORY + 
                " WHERE " + KEY_INV_ITEM_NAME + " = 'Cheese'", null);
        if (cheeseCursor.moveToFirst() && cheeseCursor.getDouble(0) <= 0.01) {
            cheeseCursor.close();
            return false;
        }
        cheeseCursor.close();

        // Check additional ingredients/toppings
        Cursor ingCursor = db.rawQuery("SELECT " + KEY_PI_INGREDIENT_NAME + " FROM " + TABLE_PRODUCT_INGREDIENTS + 
                " WHERE " + KEY_PI_PRODUCT_ID + " = ?", new String[]{String.valueOf(product.getId())});
        
        while (ingCursor.moveToNext()) {
            String ingName = ingCursor.getString(0);
            Cursor invCursor = db.rawQuery("SELECT " + KEY_INV_QUANTITY + " FROM " + TABLE_INVENTORY + 
                    " WHERE " + KEY_INV_ITEM_NAME + " = ?", new String[]{ingName});
            
            if (invCursor.moveToFirst() && invCursor.getDouble(0) <= 0.01) {
                invCursor.close();
                ingCursor.close();
                return false;
            }
            invCursor.close();
        }
        ingCursor.close();

        return true;
    }

    public void logInventoryChange(String itemName, double qty, String type, String reason) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        int itemId = -1;
        Cursor c = db.rawQuery("SELECT " + KEY_ID + " FROM " + TABLE_INVENTORY + " WHERE " + KEY_INV_ITEM_NAME + " = ?", new String[]{itemName});
        if (c.moveToFirst()) {
            itemId = c.getInt(0);
        }
        c.close();

        if (itemId == -1) return;

        // Ensure rounding for whole units if needed, but allow decimals for others
        if (itemName.equalsIgnoreCase("Cheese") || itemName.toLowerCase().contains("dough")) {
            // If they are pieces, keep them as is. If they are grams/kg, might need adjustment.
            // For now, let's just use the quantity passed.
        }

        if (type.equals("STOCK_IN")) {
            db.execSQL("UPDATE " + TABLE_INVENTORY + " SET " + KEY_INV_QUANTITY + " = " + KEY_INV_QUANTITY + " + ? WHERE " + KEY_ID + " = ?", new Object[]{qty, itemId});
        } else {
            db.execSQL("UPDATE " + TABLE_INVENTORY + " SET " + KEY_INV_QUANTITY + " = MAX(0, " + KEY_INV_QUANTITY + " - ?) WHERE " + KEY_ID + " = ?", new Object[]{qty, itemId});
        }

        if (itemId != -1 && (type.equals("SALE") || type.equals("STOCK_IN") || type.equals("WITHDRAW") || type.equals("SPOILAGE"))) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Cursor logCursor = db.rawQuery("SELECT " + KEY_ID + ", " + KEY_LOG_QUANTITY + " FROM " + TABLE_INVENTORY_LOGS + 
                    " WHERE " + KEY_LOG_ITEM_ID + " = ? AND " + KEY_LOG_TYPE + " = ? AND date(" + KEY_LOG_DATE + ") = date(?) LIMIT 1", 
                    new String[]{String.valueOf(itemId), type, today});
            
            if (logCursor.moveToFirst()) {
                int logId = logCursor.getInt(0);
                double currentLogQty = logCursor.getDouble(1);
                ContentValues v = new ContentValues();
                v.put(KEY_LOG_QUANTITY, currentLogQty + qty);
                v.put(KEY_LOG_REASON, reason);
                v.put(KEY_LOG_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                db.update(TABLE_INVENTORY_LOGS, v, KEY_ID + " = ?", new String[]{String.valueOf(logId)});
                logCursor.close();
                return;
            }
            logCursor.close();
        }

        ContentValues v = new ContentValues();
        v.put(KEY_LOG_ITEM_ID, itemId);
        v.put(KEY_LOG_TYPE, type);
        v.put(KEY_LOG_QUANTITY, qty);
        v.put(KEY_LOG_REASON, reason);
        v.put(KEY_LOG_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        db.insert(TABLE_INVENTORY_LOGS, null, v);
    }

    public List<InventoryLog> getAllInventoryLogs() {
        List<InventoryLog> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT l.*, i." + KEY_INV_ITEM_NAME + " FROM " + TABLE_INVENTORY_LOGS + " l LEFT JOIN " + TABLE_INVENTORY + " i ON l." + KEY_LOG_ITEM_ID + " = i." + KEY_ID + " ORDER BY l." + KEY_LOG_DATE + " DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_INV_ITEM_NAME));
                if (itemName == null) itemName = "Deleted Item";
                logs.add(new InventoryLog(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)), itemName, cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOG_TYPE)), cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LOG_QUANTITY)), cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOG_REASON)), cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOG_DATE))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return logs;
    }

    public void clearInventoryLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_INVENTORY_LOGS, null, null);
    }

    public long addProduct(Product product, List<String> ingredients) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PRODUCT_NAME, product.getName());
        values.put(KEY_PRODUCT_CATEGORY, product.getCategory());
        values.put(KEY_PRODUCT_PRICE, product.getPrice());
        values.put(KEY_PRODUCT_SIZE, product.getSize());
        values.put(KEY_PRODUCT_IMAGE, product.getImageUrl());
        long id = db.insert(TABLE_PRODUCTS, null, values);
        if (id != -1 && ingredients != null) {
            for (String ing : ingredients) {
                ContentValues iv = new ContentValues();
                iv.put(KEY_PI_PRODUCT_ID, id);
                iv.put(KEY_PI_INGREDIENT_NAME, ing);
                db.insert(DatabaseHelper.TABLE_PRODUCT_INGREDIENTS, null, iv);
            }
        }
        return id;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 27) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_DETAILS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCT_INGREDIENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY_LOGS);
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public boolean validateAdmin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ADMIN + " WHERE " + KEY_ADMIN_USERNAME + "=? AND " + KEY_ADMIN_PASSWORD + "=?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String getEmailByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_ADMIN_EMAIL + " FROM " + TABLE_ADMIN + " WHERE " + KEY_ADMIN_USERNAME + " = ?", new String[]{username});
        String email = null;
        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }
        cursor.close();
        return email;
    }

    public boolean validateAdminByEmail(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ADMIN + " WHERE " + KEY_ADMIN_EMAIL + "=? AND " + KEY_ADMIN_PASSWORD + "=?", new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ADMIN + " WHERE " + KEY_ADMIN_EMAIL + "=?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean updatePasswordByEmail(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ADMIN_PASSWORD, newPassword);
        int rows = db.update(TABLE_ADMIN, values, KEY_ADMIN_EMAIL + " = ?", new String[]{email});
        return rows > 0;
    }

    public boolean updateAdmin(String oldUsername, String newUsername, String newEmail, String newPassword, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ADMIN_USERNAME, newUsername);
        values.put(KEY_ADMIN_EMAIL, newEmail);
        values.put(KEY_ADMIN_PASSWORD, newPassword);
        if (imageUri != null) values.put(KEY_ADMIN_IMAGE, imageUri);
        int rows = db.update(TABLE_ADMIN, values, KEY_ADMIN_USERNAME + " = ?", new String[]{oldUsername});
        return rows > 0;
    }

    public boolean updateAdminPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ADMIN_PASSWORD, newPassword);
        int rows = db.update(TABLE_ADMIN, values, KEY_ADMIN_USERNAME + " = ?", new String[]{username});
        return rows > 0;
    }

    public Cursor getAdminData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_ADMIN + " LIMIT 1", null);
    }

    public Cursor getAdminDataByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_ADMIN + " WHERE " + KEY_ADMIN_EMAIL + " = ?", new String[]{email});
    }

    public Cursor getAdminDataByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_ADMIN + " WHERE " + KEY_ADMIN_USERNAME + " = ?", new String[]{username});
    }
}
