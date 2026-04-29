package com.example.inventory_salesanalytics_pointofsale_system.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryLogAdapter extends RecyclerView.Adapter<InventoryLogAdapter.ViewHolder> {

    private List<InventoryLog> logList;

    public InventoryLogAdapter(List<InventoryLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryLog log = logList.get(position);
        holder.tvItemName.setText(log.getItemName());
        holder.tvReason.setText("Remarks: " + log.getReason());
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(log.getDate());
            if (date != null) {
                holder.tvDate.setText(outputFormat.format(date));
            } else {
                holder.tvDate.setText(log.getDate());
            }
        } catch (Exception e) {
            holder.tvDate.setText(log.getDate());
        }
        
        boolean isStockIn = "STOCK_IN".equals(log.getType());
        String prefix = isStockIn ? "+" : "-";
        
        // Show whole number for Cheese and Dough (if quantity has no decimals)
        double qty = log.getQuantity();
        String qtyStr;
        if (qty == (long) qty) {
            qtyStr = String.format(Locale.getDefault(), "%d", (long) qty);
        } else {
            qtyStr = String.format(Locale.getDefault(), "%.2f", qty);
        }
        
        holder.tvQty.setText(prefix + qtyStr);
        holder.tvType.setText(isStockIn ? "IN" : "OUT");

        if (isStockIn) {
            holder.tvQty.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvType.setBackgroundColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvQty.setTextColor(Color.parseColor("#C62828"));
            holder.tvType.setBackgroundColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public int getItemCount() {
        return logList == null ? 0 : logList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvItemName, tvReason, tvDate, tvQty, tvType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvLogItemName);
            tvReason = itemView.findViewById(R.id.tvLogReason);
            tvDate = itemView.findViewById(R.id.tvLogDate);
            tvQty = itemView.findViewById(R.id.tvLogQty);
            tvType = itemView.findViewById(R.id.tvLogType);
        }
    }
}
