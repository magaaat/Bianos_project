package com.example.inventory_salesanalytics_pointofsale_system.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.models.InventoryItem;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.util.Locale;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryItem> inventoryList;
    private OnInventoryClickListener listener;

    public interface OnInventoryClickListener {
        void onManageClick(InventoryItem item, boolean isQuickAdd);
        void onDeleteClick(InventoryItem item);
    }

    public InventoryAdapter(List<InventoryItem> inventoryList, OnInventoryClickListener listener) {
        this.inventoryList = inventoryList;
        this.listener = listener;
    }

    public void updateData(List<InventoryItem> newList) {
        this.inventoryList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = inventoryList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvUnit.setText(item.getUnit());

        // Handle whole number display (No .0 if it's a whole number)
        double qty = item.getQuantity();
        if (qty == (long) qty) {
            holder.tvStock.setText(String.format(Locale.getDefault(), "%d", (long) qty));
        } else {
            holder.tvStock.setText(String.format(Locale.getDefault(), "%.2f", qty));
        }

        if (qty <= item.getMinimumStock()) {
            holder.tvStatus.setText("LOW STOCK");
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
        } else {
            holder.tvStatus.setText("IN STOCK");
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        }

        holder.itemView.setOnClickListener(v -> listener.onManageClick(item, false));
        holder.btnQuickAdd.setOnClickListener(v -> listener.onManageClick(item, true));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUnit, tvStock, tvStatus;
        MaterialCardView cvStatus;
        ImageButton btnQuickAdd, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvUnit = itemView.findViewById(R.id.tvItemUnit);
            tvStock = itemView.findViewById(R.id.tvStockCount);
            tvStatus = itemView.findViewById(R.id.tvStockStatus);
            cvStatus = itemView.findViewById(R.id.cvStatusBadge);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAdd);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
