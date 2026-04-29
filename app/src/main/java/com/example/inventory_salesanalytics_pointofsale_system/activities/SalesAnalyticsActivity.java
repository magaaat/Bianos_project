package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.inventory_salesanalytics_pointofsale_system.MainActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesAnalyticsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvDailySales, tvMonthlySales, tvDailyLabel;
    private BarChart barChartDaily;
    private LineChart lineChartMonthly;
    private PieChart pieChartBestSellers;
    private Button btnExportDaily, btnExportMonthly, btnExportReceipt;

    private final BroadcastReceiver salesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sales);

        dbHelper = new DatabaseHelper(this);

        tvDailySales = findViewById(R.id.tvDailySales);
        tvMonthlySales = findViewById(R.id.tvMonthlySales);
        tvDailyLabel = findViewById(R.id.tvDailyLabel);
        barChartDaily = findViewById(R.id.barChartDaily);
        lineChartMonthly = findViewById(R.id.lineChartMonthly);
        pieChartBestSellers = findViewById(R.id.pieChartBestSellers);
        
        btnExportDaily = findViewById(R.id.btnExportDaily);
        btnExportMonthly = findViewById(R.id.btnExportMonthly);
        btnExportReceipt = findViewById(R.id.btnExportReceipt);

        setupClickListeners();
        refreshData();
    }

    private void setupClickListeners() {
        if (btnExportDaily != null) btnExportDaily.setOnClickListener(v -> generateDailyReportPDF());
        if (btnExportMonthly != null) btnExportMonthly.setOnClickListener(v -> generateMonthlyReportPDF());
        if (btnExportReceipt != null) btnExportReceipt.setOnClickListener(v -> generateReceiptPDF());
        
        Button navHome = findViewById(R.id.nav_home);
        if (navHome != null) navHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    }

    private void refreshData() {
        loadSummaryData();
        loadDailySalesChart();
        loadMonthlySalesChart();
        loadBestSellingChart();
    }

    private void loadSummaryData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        tvDailyLabel.setText("Daily Sales");
        Cursor c1 = db.rawQuery("SELECT SUM(total_amount) FROM sales WHERE date(sale_date) = date('now')", null);
        if (c1.moveToFirst()) tvDailySales.setText(String.format("₱ %.2f", c1.getDouble(0)));
        c1.close();
        
        Cursor c3 = db.rawQuery("SELECT SUM(total_amount) FROM sales WHERE date(sale_date) >= date('now', 'start of month')", null);
        if (c3.moveToFirst()) tvMonthlySales.setText(String.format("₱ %.2f", c3.getDouble(0)));
        c3.close();
    }

    private void generateDailyReportPDF() {
        // Implementation for daily report PDF
    }

    private void generateMonthlyReportPDF() {
        // Implementation for monthly report PDF
    }

    private void generateReceiptPDF() {
        // Implementation for receipts PDF
    }

    private void openPDF(File file) {
        // Implementation for opening PDF
    }

    private void loadDailySalesChart() {
        if (barChartDaily == null) return;
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT date(sale_date) as day, SUM(total_amount) as total FROM sales WHERE date(sale_date) >= date('now', '-7 days') GROUP BY day ORDER BY day ASC";
        Cursor cursor = db.rawQuery(query, null);
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                entries.add(new BarEntry(i, (float) cursor.getDouble(1)));
                labels.add(cursor.getString(0).substring(5));
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        BarDataSet dataSet = new BarDataSet(entries, "Revenue");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barChartDaily.setData(new BarData(dataSet));
        barChartDaily.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartDaily.invalidate();
    }

    private void loadMonthlySalesChart() {
        if (lineChartMonthly == null) return;
        // Implementation for monthly sales chart
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
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieChartBestSellers.setData(new PieData(dataSet));
        pieChartBestSellers.invalidate();
    }
}
