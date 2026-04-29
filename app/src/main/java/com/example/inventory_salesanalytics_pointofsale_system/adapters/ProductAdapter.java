package com.example.inventory_salesanalytics_pointofsale_system.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.example.inventory_salesanalytics_pointofsale_system.models.Product;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PRODUCT = 0;
    private static final int TYPE_ADD_FLAVOR = 1;

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onAddClick(Product product);
        void onEditClick(Product product);
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (productList.get(position).getId() == -1) {
            return TYPE_ADD_FLAVOR;
        }
        return TYPE_PRODUCT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_FLAVOR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_flavor, parent, false);
            return new AddFlavorViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Product product = productList.get(position);
        
        if (holder instanceof ProductViewHolder) {
            ProductViewHolder productHolder = (ProductViewHolder) holder;
            Context context = holder.itemView.getContext();
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            
            productHolder.tvName.setText(product.getName());
            
            boolean isAnyVariantAvailable = false;
            
            if ("Pizza".equalsIgnoreCase(product.getCategory())) {
                productHolder.tvCategory.setText("Pizza");
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + 
                        " WHERE " + DatabaseHelper.KEY_PRODUCT_NAME + " = ?", 
                        new String[]{product.getName()});
                
                double p9 = 0, p11 = 0;
                while (cursor.moveToNext()) {
                    Product variant = new Product(
                        cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getDouble(3), cursor.getString(4), cursor.getString(5)
                    );
                    
                    String size = variant.getSize();
                    double price = variant.getPrice();
                    
                    if ("9\"".equals(size)) p9 = price;
                    else if ("11\"".equals(size)) p11 = price;

                    if (dbHelper.isProductAvailable(variant)) {
                        isAnyVariantAvailable = true;
                    }
                }
                cursor.close();

                if (p9 > 0 && p11 > 0) {
                    productHolder.tvPrice.setText(String.format("9 ₱%.0f • 11 ₱%.0f", p9, p11));
                } else if (p11 > 0) {
                    productHolder.tvPrice.setText(String.format("11 ₱%.0f", p11));
                } else if (p9 > 0) {
                    productHolder.tvPrice.setText(String.format("9 ₱%.0f", p9));
                } else {
                    productHolder.tvPrice.setText("Price N/A");
                }
                productHolder.tvPrice.setVisibility(View.VISIBLE);
            } else {
                productHolder.tvPrice.setVisibility(View.VISIBLE);
                productHolder.tvPrice.setText(String.format("₱ %.2f", product.getPrice()));
                productHolder.tvCategory.setText(product.getCategory());
                isAnyVariantAvailable = dbHelper.isProductAvailable(product);
            }

            String imagePath = product.getImageUrl();
            if (imagePath != null && !imagePath.isEmpty()) {
                if (!imagePath.startsWith("http") && !imagePath.startsWith("content") && !imagePath.startsWith("file")) {
                    int resId = context.getResources().getIdentifier(imagePath, "drawable", context.getPackageName());
                    if (resId != 0) {
                        Glide.with(context).load(resId).centerCrop().into(productHolder.ivProduct);
                    } else {
                        Glide.with(context).load("file:///android_asset/" + imagePath).centerCrop().into(productHolder.ivProduct);
                    }
                } else {
                    Glide.with(context).load(imagePath).centerCrop().into(productHolder.ivProduct);
                }
            }

            productHolder.ivProduct.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(product);
                }
            });

            if (!isAnyVariantAvailable) {
                productHolder.tvStatus.setVisibility(View.VISIBLE);
                productHolder.vOverlay.setVisibility(View.VISIBLE);
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                productHolder.ivProduct.setColorFilter(filter);
                productHolder.btnAddToCart.setEnabled(false);
                productHolder.btnAddToCart.setAlpha(0.5f);
                productHolder.btnAddToCart.setOnClickListener(null);
            } else {
                productHolder.tvStatus.setVisibility(View.GONE);
                productHolder.vOverlay.setVisibility(View.GONE);
                productHolder.ivProduct.clearColorFilter();
                productHolder.btnAddToCart.setEnabled(true);
                productHolder.btnAddToCart.setAlpha(1.0f);
                
                productHolder.btnAddToCart.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddClick(product);
                    }
                });
            }
        } else if (holder instanceof AddFlavorViewHolder) {
            AddFlavorViewHolder addHolder = (AddFlavorViewHolder) holder;
            TextView tvHeading = addHolder.itemView.findViewById(R.id.tvAddHeading);
            TextView tvSubheading = addHolder.itemView.findViewById(R.id.tvAddSubheading);
            boolean isPizza = "Pizza".equalsIgnoreCase(product.getCategory());
            if (tvHeading != null) tvHeading.setText(isPizza ? "Add New Pizza Flavor" : "Add New Drink Item");
            if (tvSubheading != null) tvSubheading.setText(isPizza ? "Create special pizza flavor" : "Add a new beverage to the menu");
            addHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(product);
                }
            });
            View card = addHolder.itemView.findViewById(R.id.cardAddFlavor);
            if (card != null) {
                card.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(product);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvStatus, tvCategory;
        View vOverlay;
        ImageButton btnAddToCart;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProductImage);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStatus = itemView.findViewById(R.id.tvProductStatus);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            vOverlay = itemView.findViewById(R.id.vOutStockOverlay);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }

    public static class AddFlavorViewHolder extends RecyclerView.ViewHolder {
        public AddFlavorViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
