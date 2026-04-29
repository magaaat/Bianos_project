package com.example.inventory_salesanalytics_pointofsale_system.fragments;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.InventoryAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.InventoryLogAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryItem;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryLog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryFragment extends Fragment {

    private RecyclerView rvInventory, rvInventoryLogs;
    private TextView tvEmpty;
    private InventoryAdapter adapter;
    private InventoryLogAdapter logAdapter;
    private List<InventoryItem> inventoryList;
    private DatabaseHelper dbHelper;
    private View btnAddNewItem, btnClearLogs, btnWithdraw;

    public static final int SUCCESS_TYPE = 0;
    public static final int ERROR_TYPE = 1;
    public static final int WARNING_TYPE = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        dbHelper = new DatabaseHelper(getContext());
        rvInventory = view.findViewById(R.id.rvInventory);
        rvInventoryLogs = view.findViewById(R.id.rvInventoryLogs);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnAddNewItem = view.findViewById(R.id.btnAddStock);
        btnWithdraw = view.findViewById(R.id.btnWithdrawStock);
        btnClearLogs = view.findViewById(R.id.btnClearLogs);

        rvInventory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvInventoryLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        
        btnAddNewItem.setOnClickListener(v -> showAddNewInventoryDialog());
        
        if (btnWithdraw != null) {
            btnWithdraw.setVisibility(View.VISIBLE);
            btnWithdraw.setOnClickListener(v -> showGeneralWithdrawDialog());
        }
        
        if (btnClearLogs != null) {
            btnClearLogs.setOnClickListener(v -> showClearLogsConfirmation());
        }

        return view;
    }

    private void showFeedback(String title, String message, int type) {
        if (getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_feedback, null);
        FrameLayout iconContainer = dialogView.findViewById(R.id.flIconContainer);
        ImageView ivIcon = dialogView.findViewById(R.id.ivFeedbackIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvFeedbackTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFeedbackMessage);
        Button btnAction = dialogView.findViewById(R.id.btnFeedbackAction);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (type == SUCCESS_TYPE) {
            iconContainer.setBackgroundResource(R.drawable.bg_circle_success);
            ivIcon.setImageResource(R.drawable.ic_radio_selected);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_success_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_success_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_success_text));
        } else if (type == ERROR_TYPE) {
            iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_error_bg)));
            ivIcon.setImageResource(R.drawable.ic_close_thin);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_error_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_error_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_error_text));
        } else if (type == WARNING_TYPE) {
            iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_warning_bg)));
            ivIcon.setImageResource(R.drawable.ic_settings_info);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_warning_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_warning_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_warning_text));
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        btnAction.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showClearLogsConfirmation() {
        new AlertDialog.Builder(getContext())
            .setTitle("Clear History?")
            .setMessage("Dili na mabalik ang tanang logs. Confirm?")
            .setPositiveButton("Clear All", (dialog, which) -> {
                dbHelper.clearInventoryLogs();
                loadInventoryLogs();
                showFeedback("Cleared", "Inventory history has been cleared.", SUCCESS_TYPE);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInventory();
        loadInventoryLogs();
    }

    private void loadInventory() {
        inventoryList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_INVENTORY, null);
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.KEY_ID);
                int nameIndex = cursor.getColumnIndex(DatabaseHelper.KEY_INV_ITEM_NAME);
                int qtyIndex = cursor.getColumnIndex(DatabaseHelper.KEY_INV_QUANTITY);
                int unitIndex = cursor.getColumnIndex(DatabaseHelper.KEY_INV_UNIT);
                int minStockIndex = cursor.getColumnIndex(DatabaseHelper.KEY_INV_MIN_STOCK);

                if (cursor.moveToFirst()) {
                    do {
                        inventoryList.add(new InventoryItem(
                                idIndex != -1 ? cursor.getInt(idIndex) : 0,
                                nameIndex != -1 ? cursor.getString(nameIndex) : "Unknown",
                                qtyIndex != -1 ? cursor.getDouble(qtyIndex) : 0.0,
                                unitIndex != -1 ? cursor.getString(unitIndex) : "",
                                minStockIndex != -1 ? cursor.getInt(minStockIndex) : 10
                        ));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Exception e) {
            showFeedback("Database Error", "Failed to load inventory.", ERROR_TYPE);
        }
        updateUI();
    }

    private void loadInventoryLogs() {
        List<InventoryLog> logs = dbHelper.getAllInventoryLogs();
        logAdapter = new InventoryLogAdapter(logs);
        rvInventoryLogs.setAdapter(logAdapter);
    }

    private void updateUI() {
        if (inventoryList.isEmpty()) {
            rvInventory.setVisibility(View.GONE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvInventory.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            
            if (adapter == null) {
                adapter = new InventoryAdapter(inventoryList, new InventoryAdapter.OnInventoryClickListener() {
                    @Override
                    public void onManageClick(InventoryItem item, boolean isQuickAdd) {
                        showManageStockDialog(item, isQuickAdd);
                    }
                    @Override
                    public void onDeleteClick(InventoryItem item) {
                        showDeleteConfirmationDialog(item);
                    }
                });
                rvInventory.setAdapter(adapter);
            } else {
                adapter.updateData(inventoryList);
            }
        }
    }

    private void showManageStockDialog(InventoryItem item, boolean isQuickAdd) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manage_inventory, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvManageItemName);
        TextView tvCurrent = dialogView.findViewById(R.id.tvCurrentStock);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroupType);
        TextInputEditText etQty = dialogView.findViewById(R.id.etManageQty);
        TextInputEditText etReason = dialogView.findViewById(R.id.etManageReason);
        Button btnSave = dialogView.findViewById(R.id.btnManageSave);
        Button btnCancel = dialogView.findViewById(R.id.btnManageCancel);

        tvTitle.setText(item.getName());
        tvCurrent.setText(String.format(Locale.getDefault(), "Current: %.2f %s", item.getQuantity(), item.getUnit()));

        if (toggleGroup != null) toggleGroup.check(R.id.btnTypeIn);

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnSave.setOnClickListener(v -> {
            String qtyStr = etQty.getText().toString().trim();
            String reason = etReason.getText().toString().trim();
            if (qtyStr.isEmpty()) { etQty.setError("Required"); return; }
            if (reason.isEmpty()) { etReason.setError("Remarks required"); return; }
            try {
                double qty = Double.parseDouble(qtyStr);
                String type = (toggleGroup.getCheckedButtonId() == R.id.btnTypeIn) ? "STOCK_IN" : "WITHDRAW";
                if (type.equals("WITHDRAW") && qty > item.getQuantity()) {
                    etQty.setError("Insufficient stock");
                    return;
                }
                dbHelper.logInventoryChange(item.getName(), qty, type, reason);
                loadInventory(); loadInventoryLogs();
                dialog.dismiss();
                showFeedback("Success", item.getName() + " updated.", SUCCESS_TYPE);
            } catch (Exception e) { etQty.setError("Invalid number"); }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showGeneralWithdrawDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_new_inventory, null);
        TextView tvHeader = dialogView.findViewById(R.id.tvTitle);
        AutoCompleteTextView actvName = dialogView.findViewById(R.id.actvNewItemName);
        AutoCompleteTextView actvUnit = dialogView.findViewById(R.id.actvUnit);
        TextInputEditText etRemarks = dialogView.findViewById(R.id.etRemarks);
        TextInputEditText etQty = dialogView.findViewById(R.id.etNewItemQty);
        Button btnAction = dialogView.findViewById(R.id.btnSave);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvHeader.setText("Withdraw Stock (Manual)");
        btnAction.setText("DEDUCT STOCK");
        
        List<String> names = new ArrayList<>();
        for (InventoryItem i : inventoryList) names.add(i.getName());
        actvName.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, names));

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnAction.setOnClickListener(v -> {
            String name = actvName.getText().toString().trim();
            String qtyStr = etQty.getText().toString().trim();
            String remarks = etRemarks.getText().toString().trim();
            if (name.isEmpty() || qtyStr.isEmpty() || remarks.isEmpty()) { Toast.makeText(getContext(), "All fields required", Toast.LENGTH_SHORT).show(); return; }

            InventoryItem selectedItem = null;
            for (InventoryItem i : inventoryList) { if (i.getName().equalsIgnoreCase(name)) { selectedItem = i; break; } }
            if (selectedItem == null) return;

            try {
                double qty = Double.parseDouble(qtyStr);
                if (qty > selectedItem.getQuantity()) { etQty.setError("Insufficient stock"); return; }
                dbHelper.logInventoryChange(selectedItem.getName(), qty, "WITHDRAW", remarks);
                loadInventory(); loadInventoryLogs();
                dialog.dismiss();
                showFeedback("Stock Deducted", selectedItem.getName() + " has been reduced.", SUCCESS_TYPE);
            } catch (Exception e) { etQty.setError("Invalid number"); }
        });

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showDeleteConfirmationDialog(InventoryItem item) {
        new AlertDialog.Builder(getContext())
            .setTitle("Are you sure?")
            .setMessage(item.getName() + " will be deleted.")
            .setPositiveButton("Delete", (dialog, which) -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(DatabaseHelper.TABLE_INVENTORY, DatabaseHelper.KEY_ID + " = ?", new String[]{String.valueOf(item.getId())});
                loadInventory(); loadInventoryLogs();
                showFeedback("Deleted", item.getName() + " removed from inventory.", SUCCESS_TYPE);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddNewInventoryDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_new_inventory, null);
        AutoCompleteTextView actvName = dialogView.findViewById(R.id.actvNewItemName);
        AutoCompleteTextView actvUnit = dialogView.findViewById(R.id.actvUnit);
        TextInputEditText etQty = dialogView.findViewById(R.id.etNewItemQty);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        String[] units = {"pcs", "pack", "block", "kg", "grams", "ml", "can"};
        actvUnit.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, units));

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnSave.setOnClickListener(v -> {
            String name = actvName.getText().toString().trim();
            String unit = actvUnit.getText().toString().trim();
            String qtyStr = etQty.getText().toString().trim();
            if (name.isEmpty() || unit.isEmpty() || qtyStr.isEmpty()) return;
            try {
                double qty = Double.parseDouble(qtyStr);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.KEY_INV_ITEM_NAME, name);
                values.put(DatabaseHelper.KEY_INV_UNIT, unit);
                values.put(DatabaseHelper.KEY_INV_QUANTITY, 0.0);
                values.put(DatabaseHelper.KEY_INV_MIN_STOCK, 10);
                db.insert(DatabaseHelper.TABLE_INVENTORY, null, values);
                dbHelper.logInventoryChange(name, qty, "STOCK_IN", "Initial registration");
                loadInventory(); loadInventoryLogs();
                dialog.dismiss();
                showFeedback("Success", name + " added.", SUCCESS_TYPE);
            } catch (Exception e) { etQty.setError("Invalid number"); }
        });

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void forceDialogWidth(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            window.setAttributes(lp);
        }
    }
}
