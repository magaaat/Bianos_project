package com.example.inventory_salesanalytics_pointofsale_system.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.models.CartItem;
import com.example.inventory_salesanalytics_pointofsale_system.models.Product;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartItem> cartItems;
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onRemove(CartItem item);
        void onQuantityChange(CartItem item, int newQty);
    }

    public CartAdapter(List<CartItem> cartItems, OnCartChangeListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        Product product = item.getProduct();
        int quantity = item.getQuantity();

        holder.tvCartProductName.setText(product.getName());
        
        String size = product.getSize().replace("\"", "");
        StringBuilder details = new StringBuilder("Size: " + (size.isEmpty() ? "Standard" : size + " inch"));

        if (!item.getSelectedAddons().isEmpty()) {
            details.append("\n+ ");
            for (int i = 0; i < item.getSelectedAddons().size(); i++) {
                details.append(item.getSelectedAddons().get(i).getName());
                if (i < item.getSelectedAddons().size() - 1) details.append(", ");
            }
        }
        holder.tvCartProductSize.setText(details.toString());

        holder.tvCartProductQty.setText(String.valueOf(quantity));
        holder.tvCartProductPrice.setText(String.format("₱ %.2f", item.getTotalPrice()));

        holder.btnMinusQty.setOnClickListener(v -> {
            if (quantity > 1) {
                listener.onQuantityChange(item, quantity - 1);
                notifyDataSetChanged();
            } else {
                listener.onRemove(item);
                notifyDataSetChanged();
            }
        });

        holder.btnPlusQty.setOnClickListener(v -> {
            listener.onQuantityChange(item, quantity + 1);
            notifyDataSetChanged();
        });

        holder.btnRemoveItem.setOnClickListener(v -> {
            listener.onRemove(item);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCartProductName, tvCartProductSize, tvCartProductQty, tvCartProductPrice;
        ImageButton btnRemoveItem, btnMinusQty, btnPlusQty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCartProductName = itemView.findViewById(R.id.tvCartProductName);
            tvCartProductSize = itemView.findViewById(R.id.tvCartProductSize);
            tvCartProductQty = itemView.findViewById(R.id.tvCartProductQty);
            tvCartProductPrice = itemView.findViewById(R.id.tvCartProductPrice);
            btnRemoveItem = itemView.findViewById(R.id.btnRemoveItem);
            btnMinusQty = itemView.findViewById(R.id.btnMinusQty);
            btnPlusQty = itemView.findViewById(R.id.btnPlusQty);
        }
    }
}
