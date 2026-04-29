package com.example.inventory_salesanalytics_pointofsale_system.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.models.SaleOrder;
import java.util.List;

public class SaleOrderAdapter extends RecyclerView.Adapter<SaleOrderAdapter.ViewHolder> {

    private List<SaleOrder> orderList;

    public SaleOrderAdapter(List<SaleOrder> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SaleOrder order = orderList.get(position);

        // Numbering from oldest to newest for the day (list is sorted DESC, so we reverse index)
        int orderNum = orderList.size() - position;
        holder.tvOrderNumber.setText("#" + orderNum);

        holder.tvCustomer.setText(order.getCustomerName());
        holder.tvAmount.setText(String.format("₱ %.2f", order.getTotalAmount()));
        holder.tvSummary.setText(order.getOrderSummary());
        
        String time = order.getSaleDate();
        if (time != null && time.length() > 11) {
            time = time.substring(11, 16); 
        }
        holder.tvTime.setText(time);

        // Click to toggle details
        holder.itemView.setOnClickListener(v -> {
            boolean isVisible = holder.llOrderDetails.getVisibility() == View.VISIBLE;
            holder.llOrderDetails.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        // Reset visibility to GONE to prevent issues when recycling views
        holder.llOrderDetails.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomer, tvTime, tvAmount, tvSummary, tvOrderNumber;
        LinearLayout llOrderDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNumber = itemView.findViewById(R.id.tvOrderNumber);
            tvCustomer = itemView.findViewById(R.id.tvOrderCustomer);
            tvTime = itemView.findViewById(R.id.tvOrderTime);
            tvAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvSummary = itemView.findViewById(R.id.tvOrderSummary);
            llOrderDetails = itemView.findViewById(R.id.llOrderDetails);
        }
    }
}
