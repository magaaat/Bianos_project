package com.example.inventory_salesanalytics_pointofsale_system.fragments;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.SaleOrderAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.example.inventory_salesanalytics_pointofsale_system.models.SaleOrder;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private TextView tvDailySales, tvMonthlySales, tvViewAllOrders;
    private BarChart barChartDaily;
    private LineChart lineChartMonthly;
    private PieChart pieChartBestSellers;
    private Button btnExportDaily, btnExportMonthly, btnExportReceipt;

    private final BroadcastReceiver salesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAllData();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        dbHelper = new DatabaseHelper(getContext());
        tvDailySales = view.findViewById(R.id.tvDailySales);
        tvMonthlySales = view.findViewById(R.id.tvMonthlySales);
        tvViewAllOrders = view.findViewById(R.id.tvViewAllOrders);
        barChartDaily = view.findViewById(R.id.barChartDaily);
        lineChartMonthly = view.findViewById(R.id.lineChartMonthly);
        pieChartBestSellers = view.findViewById(R.id.pieChartBestSellers);
        
        btnExportDaily = view.findViewById(R.id.btnExportDaily);
        btnExportMonthly = view.findViewById(R.id.btnExportMonthly);
        btnExportReceipt = view.findViewById(R.id.btnExportReceipt);

        if (tvViewAllOrders != null) {
            tvViewAllOrders.setOnClickListener(v -> showAllOrdersDialog());
        }

        View cvRecentOrdersContainer = view.findViewById(R.id.cvRecentOrdersContainer);
        if (cvRecentOrdersContainer != null) {
            cvRecentOrdersContainer.setOnClickListener(v -> showAllOrdersDialog());
        }

        refreshAllData();

        btnExportDaily.setOnClickListener(v -> generateDailyReportPDF());
        btnExportMonthly.setOnClickListener(v -> generateMonthlyReportPDF());
        btnExportReceipt.setOnClickListener(v -> generateReceiptPDF());

        return view;
    }

    private void showAllOrdersDialog() {
        if (getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_inventory_logs, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvLogTitle);
        RecyclerView rvAllOrders = dialogView.findViewById(R.id.rvInventoryLogs);
        View btnClose = dialogView.findViewById(R.id.btnLogsClose);

        if (tvTitle != null) tvTitle.setText("Recent Orders (Today)");
        
        rvAllOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<SaleOrder> allOrders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, customer_name, total_amount, sale_date FROM sales WHERE date(sale_date) = date('now', 'localtime') ORDER BY sale_date DESC", null);
        
        if (cursor.moveToFirst()) {
            do {
                int saleId = cursor.getInt(0);
                String customerName = cursor.getString(1);
                double amount = cursor.getDouble(2);
                String date = cursor.getString(3);
                
                StringBuilder summary = new StringBuilder();
                Cursor cItems = db.rawQuery("SELECT p.product_name, p.size, od.quantity, od.subtotal FROM order_details od JOIN products p ON od.product_id = p.id WHERE od.sale_id = ?", new String[]{String.valueOf(saleId)});
                while (cItems.moveToNext()) {
                    summary.append("• ").append(cItems.getString(0)).append(" x").append(cItems.getInt(2)).append("\n");
                }
                cItems.close();
                allOrders.add(new SaleOrder(saleId, customerName, amount, date, summary.toString().trim()));
            } while (cursor.moveToNext());
        }
        cursor.close();

        SaleOrderAdapter allOrderAdapter = new SaleOrderAdapter(allOrders);
        rvAllOrders.setAdapter(allOrderAdapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
        
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.80);
            window.setAttributes(lp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAllData();
        if (getContext() != null) {
            getContext().registerReceiver(salesUpdateReceiver, 
                new IntentFilter("com.example.inventory_salesanalytics_pointofsale_system.UPDATE_SALES"), 
                Context.RECEIVER_EXPORTED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unregisterReceiver(salesUpdateReceiver);
        }
    }

    private void refreshAllData() {
        loadSummaryData();
        updateRecentOrdersEntry();
        loadDailySalesChart();
        loadMonthlySalesChart();
        loadBestSellingChart();
    }

    private void loadSummaryData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c1 = db.rawQuery("SELECT SUM(total_amount) FROM sales WHERE date(sale_date) = date('now', 'localtime')", null);
        if (c1.moveToFirst()) tvDailySales.setText(String.format("₱ %.2f", c1.getDouble(0)));
        c1.close();
        
        Cursor c3 = db.rawQuery("SELECT SUM(total_amount) FROM sales WHERE strftime('%Y-%m', sale_date) = strftime('%Y-%m', 'now', 'localtime')", null);
        if (c3.moveToFirst()) tvMonthlySales.setText(String.format("₱ %.2f", c3.getDouble(0)));
        c3.close();
    }

    private void updateRecentOrdersEntry() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sales WHERE date(sale_date) = date('now', 'localtime')", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        
        if (tvViewAllOrders != null) {
            tvViewAllOrders.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void generateDailyReportPDF() {
        String dateStr = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date());
        String fileName = "daily_report_" + dateStr + ".pdf";
        File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Biaño’s Pizza").setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Daily Sales Report").setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Date: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())).setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("\n"));

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            Cursor c1 = db.rawQuery("SELECT SUM(total_amount), COUNT(*) FROM sales WHERE date(sale_date) = date('now', 'localtime')", null);
            double totalSales = 0;
            int totalOrders = 0;
            if (c1.moveToFirst()) {
                totalSales = c1.getDouble(0);
                totalOrders = c1.getInt(1);
            }
            c1.close();

            String bestSeller = "N/A";
            Cursor c2 = db.rawQuery("SELECT p.product_name FROM order_details od JOIN products p ON od.product_id = p.id JOIN sales s ON od.sale_id = s.id WHERE date(s.sale_date) = date('now', 'localtime') GROUP BY od.product_id ORDER BY SUM(od.quantity) DESC LIMIT 1", null);
            if (c2.moveToFirst()) bestSeller = c2.getString(0);
            c2.close();

            document.add(new Paragraph("Total Sales: ₱" + String.format("%.2f", totalSales)));
            document.add(new Paragraph("Total Orders: " + totalOrders));
            document.add(new Paragraph("Best Seller: " + bestSeller));
            document.add(new Paragraph("\nOrder Summary:").setBold());

            Table table = new Table(UnitValue.createPointArray(new float[]{3, 1, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Product");
            table.addHeaderCell("Qty");
            table.addHeaderCell("Subtotal");

            Cursor c3 = db.rawQuery("SELECT p.product_name, SUM(od.quantity), SUM(od.subtotal) FROM order_details od JOIN products p ON od.product_id = p.id JOIN sales s ON od.sale_id = s.id WHERE date(s.sale_date) = date('now', 'localtime') GROUP BY od.product_id", null);
            if (c3.moveToFirst()) {
                do {
                    table.addCell(c3.getString(0));
                    table.addCell(String.valueOf(c3.getInt(1)));
                    table.addCell("₱" + String.format("%.2f", c3.getDouble(2)));
                } while (c3.moveToNext());
            }
            c3.close();
            document.add(table);

            document.add(new Paragraph("\nGenerated by Biaño’s Pizza POS System").setFontSize(10).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(128, 128, 128)));
            document.close();
            Toast.makeText(getContext(), "PDF Saved: " + fileName, Toast.LENGTH_LONG).show();
            openPDF(file);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateMonthlyReportPDF() {
        String dateStr = new SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(new Date());
        String fileName = "monthly_report_" + dateStr + ".pdf";
        File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Biaño’s Pizza").setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Monthly Sales Report").setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Month: " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date())).setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("\n"));

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            Cursor c1 = db.rawQuery("SELECT SUM(total_amount), COUNT(*) FROM sales WHERE strftime('%Y-%m', sale_date) = strftime('%Y-%m', 'now', 'localtime')", null);
            double totalSales = 0;
            int totalOrders = 0;
            if (c1.moveToFirst()) {
                totalSales = c1.getDouble(0);
                totalOrders = c1.getInt(1);
            }
            c1.close();

            document.add(new Paragraph("Total Sales: ₱" + String.format("%.2f", totalSales)));
            document.add(new Paragraph("Total Orders: " + totalOrders));
            document.add(new Paragraph("\nProduct Breakdown:").setBold());

            Table table = new Table(UnitValue.createPointArray(new float[]{3, 1, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Product");
            table.addHeaderCell("Total Qty");
            table.addHeaderCell("Revenue");

            Cursor c3 = db.rawQuery("SELECT p.product_name, SUM(od.quantity), SUM(od.subtotal) FROM order_details od JOIN products p ON od.product_id = p.id JOIN sales s ON od.sale_id = s.id WHERE strftime('%Y-%m', s.sale_date) = strftime('%Y-%m', 'now', 'localtime') GROUP BY od.product_id ORDER BY SUM(od.subtotal) DESC", null);
            if (c3.moveToFirst()) {
                do {
                    table.addCell(c3.getString(0));
                    table.addCell(String.valueOf(c3.getInt(1)));
                    table.addCell("₱" + String.format("%.2f", c3.getDouble(2)));
                } while (c3.moveToNext());
            }
            c3.close();
            document.add(table);

            document.add(new Paragraph("\nGenerated by Biaño’s Pizza POS System").setFontSize(10).setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(128, 128, 128)));
            document.close();
            Toast.makeText(getContext(), "Monthly PDF Saved", Toast.LENGTH_LONG).show();
            openPDF(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateReceiptPDF() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cSale = db.rawQuery("SELECT id, sale_date, total_amount FROM sales ORDER BY id DESC LIMIT 1", null);
        if (!cSale.moveToFirst()) {
            Toast.makeText(getContext(), "No sales found to export receipt", Toast.LENGTH_SHORT).show();
            cSale.close();
            return;
        }
        int saleId = cSale.getInt(0);
        String saleDate = cSale.getString(1);
        double total = cSale.getDouble(2);
        cSale.close();

        String fileName = "receipt_" + saleId + ".pdf";
        File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Biaño’s Pizza").setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("OFFICIAL RECEIPT").setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Order #" + saleId).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Date: " + saleDate).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("--------------------------------------------------").setTextAlignment(TextAlignment.CENTER));

            Cursor cItems = db.rawQuery("SELECT p.product_name, od.quantity, od.subtotal FROM order_details od JOIN products p ON od.product_id = p.id WHERE od.sale_id = ?", new String[]{String.valueOf(saleId)});
            if (cItems.moveToFirst()) {
                do {
                    String line = String.format("%-20s x%d   ₱%.2f", cItems.getString(0), cItems.getInt(1), cItems.getDouble(2));
                    document.add(new Paragraph(line).setFontSize(10));
                } while (cItems.moveToNext());
            }
            cItems.close();

            document.add(new Paragraph("--------------------------------------------------").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("TOTAL: ₱" + String.format("%.2f", total)).setBold().setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("\nThank you for choosing Biaño’s Pizza!").setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            Toast.makeText(getContext(), "Receipt PDF Saved", Toast.LENGTH_LONG).show();
            openPDF(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openPDF(File file) {
        Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDailySalesChart() {
        if (barChartDaily == null) return;
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT date(sale_date) as day, SUM(total_amount) as total FROM sales WHERE date(sale_date) >= date('now', 'localtime', '-7 days') GROUP BY day ORDER BY day ASC";
        Cursor cursor = db.rawQuery(query, null);
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                entries.add(new BarEntry(i, (float) cursor.getDouble(1)));
                String date = cursor.getString(0);
                labels.add(date.substring(5));
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        if (entries.isEmpty()) { barChartDaily.setNoDataText("No sales data"); barChartDaily.invalidate(); return; }
        BarDataSet dataSet = new BarDataSet(entries, "Daily Sales");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.DKGRAY);
        BarData barData = new BarData(dataSet);
        barChartDaily.setData(barData);
        barChartDaily.getDescription().setEnabled(false);
        XAxis xAxis = barChartDaily.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        barChartDaily.getAxisLeft().setTextColor(Color.BLACK);
        barChartDaily.getAxisRight().setEnabled(false);
        barChartDaily.getLegend().setTextColor(Color.BLACK);
        barChartDaily.invalidate();
    }

    private void loadMonthlySalesChart() {
        if (lineChartMonthly == null) return;
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT strftime('%m', sale_date) as month, SUM(total_amount) as total FROM sales WHERE strftime('%Y', sale_date) = strftime('%Y', 'now', 'localtime') GROUP BY month ORDER BY month ASC";
        Cursor cursor = db.rawQuery(query, null);
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                entries.add(new Entry(i, (float) cursor.getDouble(1)));
                labels.add(cursor.getString(0));
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        if (entries.isEmpty()) { lineChartMonthly.setNoDataText("No sales data"); lineChartMonthly.invalidate(); return; }
        LineDataSet dataSet = new LineDataSet(entries, "Monthly Trend");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setValueTextColor(Color.BLACK);
        LineData lineData = new LineData(dataSet);
        lineChartMonthly.setData(lineData);
        XAxis xAxis = lineChartMonthly.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.BLACK);
        lineChartMonthly.getAxisLeft().setTextColor(Color.BLACK);
        lineChartMonthly.getAxisRight().setEnabled(false);
        lineChartMonthly.getLegend().setTextColor(Color.BLACK);
        lineChartMonthly.invalidate();
    }

    private void loadBestSellingChart() {
        if (pieChartBestSellers == null) return;
        List<PieEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT p.product_name, SUM(od.quantity) as total_qty FROM order_details od JOIN products p ON od.product_id = p.id GROUP BY od.product_id ORDER BY total_qty DESC LIMIT 5";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do { entries.add(new PieEntry((float) cursor.getInt(1), cursor.getString(0))); } while (cursor.moveToNext());
        }
        cursor.close();
        if (entries.isEmpty()) { pieChartBestSellers.setNoDataText("No order data"); pieChartBestSellers.invalidate(); return; }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        
        pieChartBestSellers.setDrawEntryLabels(true);
        pieChartBestSellers.setEntryLabelColor(Color.BLACK);
        pieChartBestSellers.setEntryLabelTextSize(11f);

        PieData pieData = new PieData(dataSet);
        pieChartBestSellers.setData(pieData);
        pieChartBestSellers.setHoleColor(Color.WHITE);
        pieChartBestSellers.getLegend().setTextColor(Color.BLACK);
        pieChartBestSellers.getDescription().setEnabled(false);
        pieChartBestSellers.invalidate();
    }
}
