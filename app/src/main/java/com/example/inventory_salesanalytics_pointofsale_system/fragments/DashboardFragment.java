package com.example.inventory_salesanalytics_pointofsale_system.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.inventory_salesanalytics_pointofsale_system.MainActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.activities.SalesAnalyticsActivity;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.example.inventory_salesanalytics_pointofsale_system.notifications.NotificationHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class DashboardFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private TextView tvTodaySales, tvTodayOrders, tvLowStockAlerts;
    private TextView tvTopProductName, tvTopProductStats;
    private ImageView ivTopProduct;
    private ImageSwitcher isHeaderSwitcher;
    private EditText etSearch;

    private Handler headerHandler = new Handler();
    private int currentHeaderIndex = 0;
    private final int[] backgrounds = {
            R.drawable.br_a,
            R.drawable.br_o,
            R.drawable.bh_pizza,
            R.drawable.red_pizza
    };

    private final BroadcastReceiver salesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDashboardData();
        }
    };

    private Runnable headerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHeaderSwitcher != null && isAdded() && getContext() != null) {
                currentHeaderIndex = (currentHeaderIndex + 1) % backgrounds.length;
                isHeaderSwitcher.setImageResource(backgrounds[currentHeaderIndex]);
                headerHandler.postDelayed(this, 5000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        View scrollView = view.findViewById(R.id.dashboard_scroll_view);
        if (scrollView != null) {
            scrollView.setTag("scroll_view_tag");
        }

        dbHelper = new DatabaseHelper(getContext());
        tvTodaySales = view.findViewById(R.id.tvTodaySales);
        tvTodayOrders = view.findViewById(R.id.tvTodayOrders);
        tvLowStockAlerts = view.findViewById(R.id.tvLowStockAlerts);
        
        tvTopProductName = view.findViewById(R.id.tvTopProductName);
        tvTopProductStats = view.findViewById(R.id.tvTopProductStats);
        ivTopProduct = view.findViewById(R.id.ivTopProduct);
        isHeaderSwitcher = view.findViewById(R.id.isHeaderSwitcher);

        if (isHeaderSwitcher != null) {
            isHeaderSwitcher.setFactory(() -> {
                ImageView imageView = new ImageView(getContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return imageView;
            });
            Animation in = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
            Animation out = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_left);
            isHeaderSwitcher.setInAnimation(in);
            isHeaderSwitcher.setOutAnimation(out);
        }
        
        ImageView ivSettings = view.findViewById(R.id.ivSettings);
        etSearch = view.findViewById(R.id.etSearch);
        
        View btnCheckNotifications = view.findViewById(R.id.cvStockAlert);
        View btnSendDailyReport = view.findViewById(R.id.btnSendDailyReport);
        View btnViewRevenueDetails = view.findViewById(R.id.btnViewRevenueDetails);
        View cvRevenue = view.findViewById(R.id.cvRevenue);
        View cvTopSeller = view.findViewById(R.id.cvTopSeller);

        setupSearch();

        if (btnCheckNotifications != null) {
            btnCheckNotifications.setOnClickListener(v -> performSystemCheck());
        }

        if (btnSendDailyReport != null) {
            btnSendDailyReport.setOnClickListener(v -> showDailySummaryDialog());
        }

        View.OnClickListener revenueClickListener = v -> showRevenueDetails();
        if (btnViewRevenueDetails != null) {
            btnViewRevenueDetails.setOnClickListener(revenueClickListener);
        }
        if (cvRevenue != null) {
            cvRevenue.setOnClickListener(revenueClickListener);
        }

        if (cvTopSeller != null) {
            cvTopSeller.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setCurrentTab(3);
                }
            });
        }

        if (ivSettings != null) {
            ivSettings.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSettings();
                }
            });
        }

        // Fix: Always register the receiver when the view is created
        registerUpdateReceiver();

        return view;
    }

    private void registerUpdateReceiver() {
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter("com.example.inventory_salesanalytics_pointofsale_system.UPDATE_SALES");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(salesUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(salesUpdateReceiver, filter);
            }
        }
    }

    private void performSystemCheck() {
        if (dbHelper == null || getContext() == null) return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursorStock = db.rawQuery("SELECT " + DatabaseHelper.KEY_INV_ITEM_NAME + 
                " FROM " + DatabaseHelper.TABLE_INVENTORY + 
                " WHERE " + DatabaseHelper.KEY_INV_QUANTITY + " <= " + DatabaseHelper.KEY_INV_MIN_STOCK, null);
        
        boolean hasAlerts = false;
        if (cursorStock != null && cursorStock.moveToFirst()) {
            hasAlerts = true;
            StringBuilder items = new StringBuilder();
            int count = 0;
            do {
                items.append(cursorStock.getString(0)).append(", ");
                count++;
            } while (cursorStock.moveToNext() && count < 5);
            
            String alertMsg = "Critical Stock Level: " + items.substring(0, items.length() - 2);
            NotificationHelper.showNotification(getContext(), "Inventory Alert", alertMsg);
            cursorStock.close();
        }

        if (hasAlerts) {
            Toast.makeText(getContext(), "Alerts triggered! Check your notification bar.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "All systems normal. No pending notifications.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDailySummaryDialog() {
        if (getContext() == null || dbHelper == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_daily_report, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialogTheme)
                .setView(dialogView)
                .create();

        TextView tvReportDate = dialogView.findViewById(R.id.tvReportDate);
        TextView tvDialogTotalSales = dialogView.findViewById(R.id.tvDialogTotalSales);
        TextView tvDialogTotalOrders = dialogView.findViewById(R.id.tvDialogTotalOrders);
        TextView tvDialogInventoryStatus = dialogView.findViewById(R.id.tvDialogInventoryStatus);
        EditText etEmailRecipient = dialogView.findViewById(R.id.etEmailRecipient);
        View btnSendReport = dialogView.findViewById(R.id.btnSendReport);
        View btnCancelReport = dialogView.findViewById(R.id.btnCancelReport);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String displayDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(new Date());
        String todayMatch = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        
        tvReportDate.setText(displayDate);

        Cursor cursorSales = db.rawQuery("SELECT SUM(" + DatabaseHelper.KEY_SALE_TOTAL_AMOUNT + "), COUNT(*) FROM " + DatabaseHelper.TABLE_SALES + 
                       " WHERE " + DatabaseHelper.KEY_SALE_DATE + " LIKE ?", new String[]{todayMatch + "%"});
        
        double totalSales = 0;
        int totalOrders = 0;
        if (cursorSales != null && cursorSales.moveToFirst()) {
            totalSales = cursorSales.getDouble(0);
            totalOrders = cursorSales.getInt(1);
            cursorSales.close();
        }
        tvDialogTotalSales.setText(String.format(Locale.getDefault(), "₱ %.2f", totalSales));
        tvDialogTotalOrders.setText(String.valueOf(totalOrders));

        StringBuilder inventoryList = new StringBuilder();
        Cursor cursorInv = db.rawQuery("SELECT " + DatabaseHelper.KEY_INV_ITEM_NAME + ", " + DatabaseHelper.KEY_INV_QUANTITY + " FROM " + DatabaseHelper.TABLE_INVENTORY, null);
        if (cursorInv != null) {
            while (cursorInv.moveToNext()) {
                inventoryList.append("• ").append(cursorInv.getString(0))
                        .append(": ").append(cursorInv.getInt(1)).append(" left\n");
            }
            cursorInv.close();
        }
        tvDialogInventoryStatus.setText(inventoryList.toString().trim());

        final String finalTotalSales = String.format(Locale.getDefault(), "₱ %.2f", totalSales);
        final int finalTotalOrders = totalOrders;
        final String finalInventoryList = inventoryList.toString();

        btnSendReport.setOnClickListener(v -> {
            String recipient = etEmailRecipient.getText().toString().trim();
            if (!recipient.isEmpty()) {
                String reportBody = "BIAÑO'S PIZZA DAILY REPORT\n" +
                        "Date: " + displayDate + "\n" +
                        "--------------------------------\n" +
                        "TOTAL SALES: " + finalTotalSales + "\n" +
                        "TOTAL ORDERS: " + finalTotalOrders + "\n" +
                        "--------------------------------\n" +
                        "INVENTORY STATUS:\n" + finalInventoryList +
                        "\nGenerated at: " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                
                sendEmail(recipient, "Daily Business Report - " + displayDate, reportBody);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter an email address", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelReport.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void sendEmail(String to, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); 
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(intent, "Choose an Email Client"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "No email app installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateInitialHeader() {
        if (isHeaderSwitcher == null) return;
        currentHeaderIndex = new Random().nextInt(backgrounds.length);
        isHeaderSwitcher.setImageResource(backgrounds[currentHeaderIndex]);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
        updateInitialHeader();
        headerHandler.removeCallbacks(headerRunnable);
        headerHandler.postDelayed(headerRunnable, 5000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        headerHandler.removeCallbacks(headerRunnable);
        if (getContext() != null) {
            try {
                getContext().unregisterReceiver(salesUpdateReceiver);
            } catch (Exception ignored) {}
        }
    }

    private void loadDashboardData() {
        if (dbHelper == null) return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db == null) return;
        
        String todayMatch = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.KEY_SALE_TOTAL_AMOUNT + "), COUNT(*) FROM " + DatabaseHelper.TABLE_SALES + 
                       " WHERE " + DatabaseHelper.KEY_SALE_DATE + " LIKE ?", new String[]{todayMatch + "%"});

        if (cursor != null && cursor.moveToFirst()) {
            tvTodaySales.setText(String.format(Locale.getDefault(), "₱ %.2f", cursor.getDouble(0)));
            tvTodayOrders.setText(String.valueOf(cursor.getInt(1)));
            cursor.close();
        } else {
            tvTodaySales.setText("₱ 0.00");
            tvTodayOrders.setText("0");
        }

        Cursor cursorStock = db.rawQuery("SELECT " + DatabaseHelper.KEY_INV_ITEM_NAME + " FROM " + DatabaseHelper.TABLE_INVENTORY + " WHERE " + DatabaseHelper.KEY_INV_QUANTITY + " <= " + DatabaseHelper.KEY_INV_MIN_STOCK, null);
        if (cursorStock != null) {
            StringBuilder lowStockItems = new StringBuilder();
            int count = 0;
            while (cursorStock.moveToNext() && count < 3) {
                lowStockItems.append(cursorStock.getString(0)).append(", ");
                count++;
            }
            if (lowStockItems.length() > 0) {
                tvLowStockAlerts.setText(lowStockItems.substring(0, lowStockItems.length() - 2));
                tvLowStockAlerts.setTextColor(Color.parseColor("#E53935"));
            } else {
                tvLowStockAlerts.setText("All items in stock");
                tvLowStockAlerts.setTextColor(Color.parseColor("#43A047"));
            }
            cursorStock.close();
        }
        loadTopSalesData(db);
    }

    private void loadTopSalesData(SQLiteDatabase db) {
        if (db == null) return;
        String query = "SELECT p." + DatabaseHelper.KEY_PRODUCT_NAME + ", p." + DatabaseHelper.KEY_PRODUCT_IMAGE + ", " +
                "SUM(od." + DatabaseHelper.KEY_OD_QUANTITY + ") as total_qty " +
                "FROM " + DatabaseHelper.TABLE_ORDER_DETAILS + " od " +
                "JOIN " + DatabaseHelper.TABLE_PRODUCTS + " p ON od." + DatabaseHelper.KEY_OD_PRODUCT_ID + " = p." + DatabaseHelper.KEY_ID + " " +
                "GROUP BY p." + DatabaseHelper.KEY_PRODUCT_NAME + " " +
                "ORDER BY total_qty DESC LIMIT 1";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            tvTopProductName.setText(cursor.getString(0));
            tvTopProductStats.setText(cursor.getInt(2) + " units sold");
            ivTopProduct.setImageResource(R.drawable.bh_pizza);
        } else {
            tvTopProductName.setText("No sales data");
            tvTopProductStats.setText("Start selling to see stats");
            ivTopProduct.setImageResource(R.drawable.bh_pizza);
        }
        if (cursor != null) cursor.close();
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    performSearch(etSearch.getText().toString().trim());
                    return true;
                }
                return false;
            });
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty() || dbHelper == null || getContext() == null) return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_search_results, null);
        LinearLayout container = dialogView.findViewById(R.id.llResultsContainer);
        
        boolean found = false;

        Cursor cursorProd = db.rawQuery("SELECT " + DatabaseHelper.KEY_PRODUCT_NAME + ", " + DatabaseHelper.KEY_PRODUCT_PRICE + 
                " FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_NAME + " LIKE ?", new String[]{"%" + query + "%"});
        
        if (cursorProd != null) {
            while (cursorProd.moveToNext()) {
                found = true;
                addResultCard(container, "MENU", cursorProd.getString(0), String.format("₱ %.2f", cursorProd.getDouble(1)));
            }
            cursorProd.close();
        }

        Cursor cursorInv = db.rawQuery("SELECT " + DatabaseHelper.KEY_INV_ITEM_NAME + ", " + DatabaseHelper.KEY_INV_QUANTITY + ", " + DatabaseHelper.KEY_INV_UNIT +
                " FROM " + DatabaseHelper.TABLE_INVENTORY + " WHERE " + DatabaseHelper.KEY_INV_ITEM_NAME + " LIKE ?", new String[]{"%" + query + "%"});
        
        if (cursorInv != null) {
            while (cursorInv.moveToNext()) {
                found = true;
                addResultCard(container, "INVENTORY", cursorInv.getString(0), cursorInv.getInt(1) + " " + cursorInv.getString(2) + " left");
            }
            cursorInv.close();
        }

        if (found) {
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .create();
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            dialogView.findViewById(R.id.btnSearchOk).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } else {
            Toast.makeText(getContext(), "No pizza or ingredient found matching '" + query + "'", Toast.LENGTH_SHORT).show();
        }
    }

    private void addResultCard(LinearLayout container, String type, String title, String detail) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_search_result_card, container, false);
        ((TextView) card.findViewById(R.id.tvResultType)).setText(type);
        ((TextView) card.findViewById(R.id.tvResultTitle)).setText(title);
        ((TextView) card.findViewById(R.id.tvResultDetail)).setText(detail);
        container.addView(card);
    }

    private void showRevenueDetails() {
        Intent intent = new Intent(getActivity(), SalesAnalyticsActivity.class);
        startActivity(intent);
    }
}
