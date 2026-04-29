package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.InventoryAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.InventoryLogAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryItem;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryLog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryActivity extends AppCompatActivity implements InventoryAdapter.OnInventoryClickListener {

    private RecyclerView rvInventory, rvHistory;
    private InventoryAdapter adapter;
    private InventoryLogAdapter logAdapter;
    private List<InventoryItem> inventoryList;
    private List<InventoryLog> logList;
    private DatabaseHelper dbHelper;
    private Button btnAddStock, btnWithdraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_inventory);

        dbHelper = new DatabaseHelper(this);
        
        // Initialize Stock List
        rvInventory = findViewById(R.id.rvInventory);
        rvInventory.setLayoutManager(new LinearLayoutManager(this));
        rvInventory.setNestedScrollingEnabled(false);

        // Initialize History Table
        rvHistory = findViewById(R.id.rvInventoryLogs);
        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            rvHistory.setNestedScrollingEnabled(false);
        }

        btnAddStock = findViewById(R.id.btnAddStock);
        if (btnAddStock != null) {
            btnAddStock.setOnClickListener(v -> showAddNewInventoryDialog());
        }

        btnWithdraw = findViewById(R.id.btnWithdrawStock);
        if (btnWithdraw != null) {
            btnWithdraw.setVisibility(View.VISIBLE);
            btnWithdraw.setOnClickListener(v -> showGeneralWithdrawDialog());
        }

        loadInventory();
        loadTransactionHistory();
    }

    private void loadInventory() {
        inventoryList = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_INVENTORY, null);
            if (cursor.moveToFirst()) {
                do {
                    inventoryList.add(new InventoryItem(
                            cursor.getInt(0), cursor.getString(1), cursor.getDouble(2),
                            cursor.getString(3), cursor.getInt(4)
                    ));
                } while (cursor.moveToNext());
            }
            cursor.close();
            if (adapter == null) {
                adapter = new InventoryAdapter(inventoryList, this);
                rvInventory.setAdapter(adapter);
            } else {
                adapter.updateData(inventoryList);
            }
        } catch (Exception e) {
            Log.e("InventoryActivity", "Error loading inventory", e);
        }
    }

    private void loadTransactionHistory() {
        logList = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT l." + DatabaseHelper.KEY_ID + ", i." + DatabaseHelper.KEY_INV_ITEM_NAME + 
                           ", l." + DatabaseHelper.KEY_LOG_TYPE + ", l." + DatabaseHelper.KEY_LOG_QUANTITY + 
                           ", l." + DatabaseHelper.KEY_LOG_REASON + ", l." + DatabaseHelper.KEY_LOG_DATE + 
                           " FROM " + DatabaseHelper.TABLE_INVENTORY_LOGS + " l " +
                           " LEFT JOIN " + DatabaseHelper.TABLE_INVENTORY + " i ON l." + DatabaseHelper.KEY_LOG_ITEM_ID + " = i." + DatabaseHelper.KEY_ID +
                           " ORDER BY l." + DatabaseHelper.KEY_LOG_DATE + " DESC";
            
            Cursor c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                do {
                    logList.add(new InventoryLog(c.getInt(0), c.getString(1), c.getString(2), 
                               c.getDouble(3), c.getString(4), c.getString(5)));
                } while (c.moveToNext());
            }
            c.close();

            if (logAdapter == null) {
                logAdapter = new InventoryLogAdapter(logList);
                if (rvHistory != null) rvHistory.setAdapter(logAdapter);
            } else {
                logAdapter.notifyDataSetChanged(); // In a real app, use a more efficient update method
                // Re-initialize if list was updated
                logAdapter = new InventoryLogAdapter(logList);
                if (rvHistory != null) rvHistory.setAdapter(logAdapter);
            }
        } catch (Exception e) {
            Log.e("InventoryActivity", "Error loading history", e);
        }
    }

    @Override
    public void onManageClick(InventoryItem item, boolean isQuickAdd) {
        if (isQuickAdd) {
            dbHelper.logInventoryChange(item.getName(), 1.0, "STOCK_IN", "Quick Add");
            loadInventory();
            loadTransactionHistory();
            Toast.makeText(this, "Quick Added 1 " + item.getUnit(), Toast.LENGTH_SHORT).show();
        } else {
            showManageStockDialog(item);
        }
    }

    @Override
    public void onDeleteClick(InventoryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete(DatabaseHelper.TABLE_INVENTORY, DatabaseHelper.KEY_ID + " = ?", new String[]{String.valueOf(item.getId())});
                    loadInventory();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageStockDialog(InventoryItem item) {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_inventory, null);
            TextView tvTitle = dialogView.findViewById(R.id.tvManageItemName);
            TextView tvCurrent = dialogView.findViewById(R.id.tvCurrentStock);
            MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroupType);
            TextInputEditText etQty = dialogView.findViewById(R.id.etManageQty);
            TextInputEditText etReason = dialogView.findViewById(R.id.etManageReason);
            Button btnSave = dialogView.findViewById(R.id.btnManageSave);
            Button btnCancel = dialogView.findViewById(R.id.btnManageCancel);
            
            if (tvTitle != null) tvTitle.setText(item.getName());
            if (tvCurrent != null) tvCurrent.setText(String.format(Locale.getDefault(), "Current: %.2f %s", item.getQuantity(), item.getUnit()));

            AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    String qtyStr = etQty.getText().toString().trim();
                    String reason = etReason.getText().toString().trim();
                    if (qtyStr.isEmpty()) { etQty.setError("Required"); return; }
                    try {
                        double qty = Double.parseDouble(qtyStr);
                        int checkedId = toggleGroup.getCheckedButtonId();
                        String type;
                        if (checkedId == R.id.btnTypeIn) {
                            type = "STOCK_IN";
                        } else if (checkedId == R.id.btnTypeWithdraw) {
                            type = "WITHDRAW";
                        } else {
                            type = "SPOILAGE";
                        }
                        
                        dbHelper.logInventoryChange(item.getName(), qty, type, reason.isEmpty() ? "Manual " + type : reason);
                        
                        loadInventory();
                        loadTransactionHistory(); // Update history table automatically
                        dialog.dismiss();
                        Toast.makeText(this, "Inventory Updated", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { etQty.setError("Invalid number"); }
                });
            }

            if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e("InventoryActivity", "Manage Dialog error", e);
        }
    }

    private void showGeneralWithdrawDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_inventory, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvManageItemName);
        TextView tvCurrent = dialogView.findViewById(R.id.tvCurrentStock);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroupType);
        TextInputEditText etQty = dialogView.findViewById(R.id.etManageQty);
        TextInputEditText etReason = dialogView.findViewById(R.id.etManageReason);
        Button btnSave = dialogView.findViewById(R.id.btnManageSave);
        Button btnCancel = dialogView.findViewById(R.id.btnManageCancel);

        // Hide toggle group and show WITHDRAW mode only for this general dialog
        if (toggleGroup != null) {
            toggleGroup.check(R.id.btnTypeWithdraw);
            toggleGroup.setVisibility(View.GONE);
        }
        
        // Add a searchable item selection instead of just the title
        if (tvTitle != null) tvTitle.setText("Withdraw Item");
        
        // We'll reuse the logic to pick an item
        List<String> itemNames = new ArrayList<>();
        for (InventoryItem i : inventoryList) {
            itemNames.add(i.getName());
        }

        // Dynamically add a selection field if possible or just use another dialog
        // For simplicity and cleaner UI, let's first pick the item then show the manage dialog
        showItemPickerForWithdraw();
    }

    private void showItemPickerForWithdraw() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_new_inventory, null);
        TextView tvHeader = dialogView.findViewById(R.id.tvTitle);
        AutoCompleteTextView actvName = dialogView.findViewById(R.id.actvNewItemName);
        TextInputEditText etQty = dialogView.findViewById(R.id.etNewItemQty);
        Button btnWithdraw = dialogView.findViewById(R.id.btnSave);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (tvHeader != null) tvHeader.setText("Withdraw Stock");
        if (btnWithdraw != null) btnWithdraw.setText("WITHDRAW");

        // Hide size and unit fields as they aren't needed for picking an existing item
        View tilDoughSize = dialogView.findViewById(R.id.tilDoughSize);
        if (tilDoughSize != null) tilDoughSize.setVisibility(View.GONE);
        
        // Only show items that are already in inventory
        List<String> names = new ArrayList<>();
        for (InventoryItem i : inventoryList) names.add(i.getName());
        
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        actvName.setAdapter(nameAdapter);
        actvName.setHint("Select Item to Withdraw");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> {
                String name = actvName.getText().toString().trim();
                String qtyStr = etQty.getText().toString().trim();

                if (name.isEmpty()) { actvName.setError("Required"); return; }
                if (qtyStr.isEmpty()) { etQty.setError("Required"); return; }

                // Check if item exists
                InventoryItem selectedItem = null;
                for (InventoryItem i : inventoryList) {
                    if (i.getName().equalsIgnoreCase(name)) {
                        selectedItem = i;
                        break;
                    }
                }

                if (selectedItem == null) {
                    actvName.setError("Item not found");
                    return;
                }

                try {
                    double qty = Double.parseDouble(qtyStr);
                    if (qty > selectedItem.getQuantity()) {
                        etQty.setError("Insufficient stock");
                        return;
                    }
                    dbHelper.logInventoryChange(selectedItem.getName(), qty, "WITHDRAW", "Manual Withdrawal");
                    loadInventory();
                    loadTransactionHistory();
                    dialog.dismiss();
                    Toast.makeText(this, "Withdrawn " + qty + " from " + name, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    etQty.setError("Invalid number");
                }
            });
        }
        
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAddNewInventoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_new_inventory, null);
        AutoCompleteTextView actvName = dialogView.findViewById(R.id.actvNewItemName);
        TextInputLayout tilDoughSize = dialogView.findViewById(R.id.tilDoughSize);
        AutoCompleteTextView actvDoughSize = dialogView.findViewById(R.id.actvDoughSize);
        TextInputEditText etQty = dialogView.findViewById(R.id.etNewItemQty);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set specific items for the dropdown
        String[] options = {"Dough", "Cheese"};
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options);
        actvName.setAdapter(nameAdapter);

        // Dough size options
        String[] sizes = {"9", "11"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sizes);
        actvDoughSize.setAdapter(sizeAdapter);

        actvName.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if ("Dough".equalsIgnoreCase(selected)) {
                tilDoughSize.setVisibility(View.VISIBLE);
            } else {
                tilDoughSize.setVisibility(View.GONE);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = actvName.getText().toString().trim();
                String qtyStr = etQty.getText().toString().trim();
                String size = actvDoughSize.getText().toString().trim();

                if (name.isEmpty()) { actvName.setError("Required"); return; }
                if (qtyStr.isEmpty()) { etQty.setError("Required"); return; }
                if ("Dough".equalsIgnoreCase(name) && size.isEmpty()) {
                    actvDoughSize.setError("Select Size");
                    return;
                }

                try {
                    double qty = Double.parseDouble(qtyStr);
                    String finalName = name;
                    if ("Dough".equalsIgnoreCase(name)) {
                        finalName = "Dough " + size;
                    }
                    addNewInventoryItem(finalName, "pcs", qty);
                    dialog.dismiss();
                } catch (Exception e) {
                    etQty.setError("Invalid number");
                }
            });
        }
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void addNewInventoryItem(String name, String unit, double qty) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.KEY_INV_ITEM_NAME, name);
        v.put(DatabaseHelper.KEY_INV_UNIT, unit);
        v.put(DatabaseHelper.KEY_INV_QUANTITY, qty);
        v.put(DatabaseHelper.KEY_INV_MIN_STOCK, 10);
        db.insert(DatabaseHelper.TABLE_INVENTORY, null, v);
        loadInventory();
        loadTransactionHistory();
    }
}
