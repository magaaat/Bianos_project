package com.example.inventory_salesanalytics_pointofsale_system.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.CartAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.ProductAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.example.inventory_salesanalytics_pointofsale_system.models.CartItem;
import com.example.inventory_salesanalytics_pointofsale_system.models.Product;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class POSActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private List<Product> displayedProducts = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private TextView tvTotalAmount, tvCartItems, btnCatPizza, btnCatDrinks;
    private Button btnConfirmOrder;

    private List<CartItem> cart = new ArrayList<>();
    private double totalAmount = 0;
    private String currentCategory = "Pizza";
    
    // Constant types for feedback to replace SweetAlert constants
    public static final int SUCCESS_TYPE = 0;
    public static final int ERROR_TYPE = 1;
    public static final int WARNING_TYPE = 2;

    private String selectedImageUri = "pizza_placeholder.png";
    private ImageView currentDialogImageView;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedImageUri = uri.toString();
                        if (currentDialogImageView != null) {
                            Glide.with(this).load(uri).centerCrop().into(currentDialogImageView);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pos);

        dbHelper = new DatabaseHelper(this);
        
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvCartItems = findViewById(R.id.tvCartItems);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        
        btnCatPizza = findViewById(R.id.btnCatPizza);
        btnCatDrinks = findViewById(R.id.btnCatDrinks);

        btnCatPizza.setOnClickListener(v -> switchCategory("Pizza"));
        btnCatDrinks.setOnClickListener(v -> switchCategory("Drinks"));

        btnConfirmOrder.setOnClickListener(v -> {
            if (cart.isEmpty()) {
                showFeedback("Empty Cart", "Please add some items first!", WARNING_TYPE);
            } else {
                showCartDialog();
            }
        });
        
        switchCategory("Pizza");
    }

    private void showFeedback(String title, String message, int type) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feedback, null);
        ImageView ivIcon = dialogView.findViewById(R.id.ivFeedbackIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvFeedbackTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFeedbackMessage);
        Button btnAction = dialogView.findViewById(R.id.btnFeedbackAction);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (type == SUCCESS_TYPE) {
            ivIcon.setImageResource(R.drawable.ic_radio_selected);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_success_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_success_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_success_text));
        } else if (type == ERROR_TYPE) {
            ivIcon.setImageResource(R.drawable.ic_close_thin);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_error_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_error_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_error_text));
        } else if (type == WARNING_TYPE) {
            ivIcon.setImageResource(R.drawable.ic_settings_info);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_warning_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_warning_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_warning_text));
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        btnAction.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void switchCategory(String category) {
        currentCategory = category;
        loadProducts(category);

        int orange = ContextCompat.getColor(this, R.color.accent_orange);
        int grey = ContextCompat.getColor(this, R.color.text_grey);
        
        updateTabUI(btnCatPizza, category.equals("Pizza"), orange, grey);
        updateTabUI(btnCatDrinks, category.equals("Drinks"), orange, grey);
    }

    private void updateTabUI(TextView view, boolean isSelected, int activeColor, int inactiveColor) {
        if (view == null) return;
        view.setTextColor(isSelected ? activeColor : inactiveColor);
        view.setBackgroundResource(isSelected ? R.drawable.category_selector_underlined : 0);
    }

    private void loadProducts(String category) {
        displayedProducts.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + 
                " WHERE " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = ? GROUP BY " + DatabaseHelper.KEY_PRODUCT_NAME, 
                new String[]{category});
                
        if (cursor.moveToFirst()) {
            do {
                displayedProducts.add(new Product(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getDouble(3),
                        cursor.getString(4),
                        cursor.getString(5)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (productAdapter == null) {
            productAdapter = new ProductAdapter(displayedProducts, new ProductAdapter.OnProductClickListener() {
                @Override
                public void onAddClick(Product product) {
                    proceedToOrder(product);
                }

                @Override
                public void onEditClick(Product product) {
                    showEditProductDialog(product);
                }
            });
            rvProducts.setAdapter(productAdapter);
        } else {
            productAdapter.notifyDataSetChanged();
        }
    }

    private void proceedToOrder(Product product) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + 
                " WHERE " + DatabaseHelper.KEY_PRODUCT_NAME + " = ? AND " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = ?", 
                new String[]{product.getName(), product.getCategory()});

        List<Product> variants = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                variants.add(new Product(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getDouble(3),
                        cursor.getString(4),
                        cursor.getString(5)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (variants.size() > 1 || "Pizza".equalsIgnoreCase(product.getCategory())) {
            showSizeSelectionDialog(product.getName(), variants, product.getCategory());
        } else if (!variants.isEmpty()) {
            if (dbHelper.isProductAvailable(variants.get(0))) {
                addToCart(variants.get(0));
            } else {
                showFeedback("Out of Stock", "This item is currently unavailable.", ERROR_TYPE);
            }
        }
    }

    private void showEditProductDialog(Product product) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_product, null);
        
        View imageContainer = dialogView.findViewById(R.id.editImageContainer);
        currentDialogImageView = dialogView.findViewById(R.id.ivEditProductImage);
        TextInputEditText etName = dialogView.findViewById(R.id.etEditFlavorName);
        CheckBox cbFixedSize = dialogView.findViewById(R.id.cbEditFixedSize);
        LinearLayout llMultiSize = dialogView.findViewById(R.id.llEditMultiSize);
        TextInputLayout tilFixedPrice = dialogView.findViewById(R.id.tilEditFixedPrice);
        TextInputLayout tilToppings = dialogView.findViewById(R.id.tilEditToppings);
        TextInputEditText etPrice9 = dialogView.findViewById(R.id.etEditPrice9);
        TextInputEditText etPrice11 = dialogView.findViewById(R.id.etEditPrice11);
        TextInputEditText etPriceFixed = dialogView.findViewById(R.id.etEditPriceFixed);
        TextInputEditText etToppings = dialogView.findViewById(R.id.etEditToppings);
        Button btnSave = dialogView.findViewById(R.id.btnEditProductSave);
        Button btnDelete = dialogView.findViewById(R.id.btnEditProductDelete);
        Button btnCancel = dialogView.findViewById(R.id.btnEditProductCancel);

        etName.setText(product.getName());
        selectedImageUri = product.getImageUrl();
        String path = selectedImageUri;
        if (!path.startsWith("http") && !path.startsWith("content") && !path.startsWith("file")) path = "file:///android_asset/" + path;
        Glide.with(this).load(path).centerCrop().into(currentDialogImageView);
        
        View.OnClickListener pickImage = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };
        imageContainer.setOnClickListener(pickImage);
        currentDialogImageView.setOnClickListener(pickImage);

        boolean isPizza = "Pizza".equalsIgnoreCase(product.getCategory());
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_NAME + " = ?", new String[]{product.getName()});
        
        boolean isFixed;
        if (isPizza) {
            isFixed = false;
            cbFixedSize.setChecked(false);
            cbFixedSize.setVisibility(View.GONE);
            llMultiSize.setVisibility(View.VISIBLE);
            tilFixedPrice.setVisibility(View.GONE);
            tilToppings.setVisibility(View.VISIBLE);
            
            while (cursor.moveToNext()) {
                String size = cursor.getString(4);
                double price = cursor.getDouble(3);
                if ("9\"".equals(size)) etPrice9.setText(String.valueOf(price));
                else if ("11\"".equals(size)) etPrice11.setText(String.valueOf(price));
            }
        } else {
            isFixed = true;
            cbFixedSize.setChecked(true);
            cbFixedSize.setVisibility(View.GONE);
            llMultiSize.setVisibility(View.GONE);
            tilFixedPrice.setVisibility(View.VISIBLE);
            tilToppings.setVisibility(View.GONE);
            
            if (cursor.moveToFirst()) {
                etPriceFixed.setText(String.valueOf(cursor.getDouble(3)));
            }
        }
        cursor.close();

        if (isPizza) {
            Cursor topCursor = db.rawQuery("SELECT " + DatabaseHelper.KEY_PI_INGREDIENT_NAME + " FROM " + DatabaseHelper.TABLE_PRODUCT_INGREDIENTS + " WHERE " + DatabaseHelper.KEY_PI_PRODUCT_ID + " = ?", new String[]{String.valueOf(product.getId())});
            StringBuilder tops = new StringBuilder();
            while (topCursor.moveToNext()) {
                if (tops.length() > 0) tops.append(",");
                tops.append(topCursor.getString(0));
            }
            topCursor.close();
            etToppings.setText(tops.toString());
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Remove " + product.getName() + "?")
                .setPositiveButton("Delete", (d, which) -> {
                    dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_PRODUCTS, DatabaseHelper.KEY_PRODUCT_NAME + " = ?", new String[]{product.getName()});
                    dialog.dismiss();
                    loadProducts(currentCategory);
                    showFeedback("Deleted", product.getName() + " has been removed.", SUCCESS_TYPE);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) return;
            dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_PRODUCTS, DatabaseHelper.KEY_PRODUCT_NAME + " = ?", new String[]{product.getName()});
            try {
                List<String> toppings = new ArrayList<>();
                if (isPizza) {
                    String[] toppingArray = etToppings.getText().toString().split(",");
                    for (String t : toppingArray) {
                        if (!t.trim().isEmpty()) toppings.add(t.trim());
                    }
                    dbHelper.addProduct(new Product(0, name, product.getCategory(), Double.parseDouble(etPrice9.getText().toString()), "9\"", selectedImageUri), toppings);
                    dbHelper.addProduct(new Product(0, name, product.getCategory(), Double.parseDouble(etPrice11.getText().toString()), "11\"", selectedImageUri), toppings);
                } else {
                    dbHelper.addProduct(new Product(0, name, product.getCategory(), Double.parseDouble(etPriceFixed.getText().toString()), "11\"", selectedImageUri), toppings);
                }
                loadProducts(currentCategory);
                dialog.dismiss();
                showFeedback("Saved", name + " updated successfully.", SUCCESS_TYPE);
            } catch (Exception ignored) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showSizeSelectionDialog(String productName, List<Product> variants, String category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product_options, null);
        ImageView ivBg = dialogView.findViewById(R.id.ivDialogBg);
        ImageView ivProductSmall = dialogView.findViewById(R.id.ivProductSmall);
        ImageButton btnBack = dialogView.findViewById(R.id.btnDialogBack);
        TextView tvName = dialogView.findViewById(R.id.tvDialogProductName);
        LinearLayout llSizes = dialogView.findViewById(R.id.llSizes);
        LinearLayout llAddons = dialogView.findViewById(R.id.llAddons);
        View layoutAddons = dialogView.findViewById(R.id.layoutAddons);
        Button btnAdd = dialogView.findViewById(R.id.btnDialogAddToCart);

        tvName.setText(productName);
        if (!variants.isEmpty()) {
            String imagePath = variants.get(0).getImageUrl();
            if (imagePath != null && !imagePath.isEmpty()) {
                if (!imagePath.startsWith("http") && !imagePath.startsWith("content") && !imagePath.startsWith("file")) {
                    imagePath = "file:///android_asset/" + imagePath;
                }
                Glide.with(this).load(imagePath).centerCrop().into(ivBg);
                Glide.with(this).load(imagePath).centerCrop().into(ivProductSmall);
            }
        }

        boolean isPizza = "Pizza".equalsIgnoreCase(category);
        if (layoutAddons != null) {
            layoutAddons.setVisibility(isPizza ? View.VISIBLE : View.GONE);
        }

        List<Product> addonList = isPizza ? loadAddons() : new ArrayList<>();
        List<Product> selectedAddons = new ArrayList<>();
        for (Product addon : addonList) {
            View addonView = LayoutInflater.from(this).inflate(R.layout.item_addon_option, llAddons, false);
            MaterialCardView card = addonView.findViewById(R.id.rootCard);
            TextView tvAddonName = addonView.findViewById(R.id.tvAddonName);
            TextView tvAddonPrice = addonView.findViewById(R.id.tvAddonPrice);
            ImageView ivCheck = addonView.findViewById(R.id.ivCheck);

            tvAddonName.setText(addon.getName());
            tvAddonPrice.setText(String.format("+ ₱ %.2f", addon.getPrice()));

            card.setOnClickListener(v -> {
                if (selectedAddons.contains(addon)) {
                    selectedAddons.remove(addon);
                    card.setStrokeColor(Color.parseColor("#333333"));
                    card.setCardBackgroundColor(Color.parseColor("#262626"));
                    ivCheck.setImageResource(R.drawable.ic_radio_unselected);
                } else {
                    selectedAddons.add(addon);
                    card.setStrokeColor(Color.parseColor("#D4A056"));
                    card.setCardBackgroundColor(Color.parseColor("#33D4A056"));
                    ivCheck.setImageResource(R.drawable.ic_radio_selected);
                }
            });
            llAddons.addView(addonView);
        }

        final Product[] selectedVariant = {null};
        for (Product v : variants) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_size_option, llSizes, false);
            MaterialCardView card = itemView.findViewById(R.id.rootCard);
            TextView tvSize = itemView.findViewById(R.id.tvSize);
            TextView tvPrice = itemView.findViewById(R.id.tvPrice);
            ImageView ivRadio = itemView.findViewById(R.id.ivRadio);

            boolean isAvailable = dbHelper.isProductAvailable(v);
            
            if (isPizza) {
                tvSize.setText(v.getSize().replace("\"", "") + " inch");
            } else {
                tvSize.setText("Standard Size");
            }
            
            tvPrice.setText(String.format("₱ %.2f", v.getPrice()));

            if (!isAvailable) {
                card.setEnabled(false);
                card.setAlpha(0.4f);
                tvSize.setText(tvSize.getText() + " (Out of Stock)");
                ivRadio.setVisibility(View.GONE);
            } else {
                card.setOnClickListener(view -> {
                    selectedVariant[0] = v;
                    for (int i = 0; i < llSizes.getChildCount(); i++) {
                        View child = llSizes.getChildAt(i);
                        MaterialCardView childCard = child.findViewById(R.id.rootCard);
                        if (childCard.isEnabled()) {
                            childCard.setStrokeColor(Color.parseColor("#333333"));
                            childCard.setCardBackgroundColor(Color.parseColor("#262626"));
                            ImageView radio = child.findViewById(R.id.ivRadio);
                            if (radio != null) radio.setImageResource(R.drawable.ic_radio_unselected);
                        }
                    }
                    card.setStrokeColor(Color.parseColor("#D4A056"));
                    card.setCardBackgroundColor(Color.parseColor("#33D4A056"));
                    ivRadio.setImageResource(R.drawable.ic_radio_selected);
                });
                if (selectedVariant[0] == null) card.performClick();
            }
            llSizes.addView(itemView);
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            if (selectedVariant[0] == null) {
                Toast.makeText(this, "Please select an available size", Toast.LENGTH_SHORT).show();
                return;
            }
            CartItem item = new CartItem(selectedVariant[0], 1);
            item.setSelectedAddons(new ArrayList<>(selectedAddons));
            addToCart(item);
            dialog.dismiss();
        });
        dialog.show();
        forceDialogWidth(dialog);
    }

    private List<Product> loadAddons() {
        List<Product> addons = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = 'Addons'", null);
        if (cursor.moveToFirst()) {
            do {
                addons.add(new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return addons;
    }

    private void addToCart(Product product) {
        addToCart(new CartItem(product, 1));
    }

    private void addToCart(CartItem newItem) {
        boolean exists = false;
        for (CartItem item : cart) {
            if (item.getProduct().equals(newItem.getProduct()) && item.getSelectedAddons().equals(newItem.getSelectedAddons())) {
                item.setQuantity(item.getQuantity() + 1);
                exists = true;
                break;
            }
        }
        if (!exists) cart.add(newItem);
        updateCartUI();
        showFeedback("Added!", newItem.getProduct().getName() + " added to cart.", SUCCESS_TYPE);
    }

    private void updateCartUI() {
        totalAmount = 0; int totalQty = 0;
        for (CartItem item : cart) {
            totalAmount += item.getTotalPrice();
            totalQty += item.getQuantity();
        }
        tvCartItems.setText("Cart: " + totalQty + " items");
        tvTotalAmount.setText(String.format("₱ %.2f", totalAmount));
    }

    private void showCartDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
        RecyclerView rvCart = dialogView.findViewById(R.id.rvOrderList);
        TextView tvTotal = dialogView.findViewById(R.id.tvCartTotal);
        Button btnConfirm = dialogView.findViewById(R.id.btnCartCheckout);
        ImageButton btnBack = dialogView.findViewById(R.id.btnCartBack);

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        CartAdapter cartAdapter = new CartAdapter(cart, new CartAdapter.OnCartChangeListener() {
            @Override
            public void onRemove(CartItem item) {
                cart.remove(item); updateCartUI(); tvTotal.setText(String.format("₱ %.2f", totalAmount));
            }
            @Override
            public void onQuantityChange(CartItem item, int newQty) {
                item.setQuantity(newQty); updateCartUI(); tvTotal.setText(String.format("₱ %.2f", totalAmount));
            }
        });
        rvCart.setAdapter(cartAdapter);
        tvTotal.setText(String.format("₱ %.2f", totalAmount));

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Confirm Order")
                .setMessage("Proceed with order?")
                .setPositiveButton("Yes", (d, which) -> {
                    handleCheckout();
                    dialog.dismiss();
                })
                .setNegativeButton("No", null)
                .show();
        });
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void forceDialogWidth(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    private void handleCheckout() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues saleValues = new ContentValues();
            saleValues.put(DatabaseHelper.KEY_SALE_TOTAL_AMOUNT, totalAmount);
            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            saleValues.put(DatabaseHelper.KEY_SALE_DATE, currentDateTime);

            long saleId = db.insert(DatabaseHelper.TABLE_SALES, null, saleValues);
            Map<String, Double> inventoryDeductions = new HashMap<>();

            for (CartItem item : cart) {
                Product p = item.getProduct(); int qty = item.getQuantity();
                ContentValues odValues = new ContentValues();
                odValues.put(DatabaseHelper.KEY_OD_SALE_ID, saleId);
                odValues.put(DatabaseHelper.KEY_OD_PRODUCT_ID, p.getId());
                odValues.put(DatabaseHelper.KEY_OD_QUANTITY, qty);
                odValues.put(DatabaseHelper.KEY_OD_SUBTOTAL, item.getTotalPrice());
                db.insert(DatabaseHelper.TABLE_ORDER_DETAILS, null, odValues);

                if ("Pizza".equalsIgnoreCase(p.getCategory())) {
                    String doughName = "Dough " + p.getSize().replace("\"", "");
                    inventoryDeductions.put(doughName, inventoryDeductions.getOrDefault(doughName, 0.0) + qty);
                    Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.KEY_PI_INGREDIENT_NAME + " FROM " + DatabaseHelper.TABLE_PRODUCT_INGREDIENTS + " WHERE " + DatabaseHelper.KEY_PI_PRODUCT_ID + " = ?", new String[]{String.valueOf(p.getId())});
                    while (cursor.moveToNext()) {
                        String ingName = cursor.getString(0);
                        inventoryDeductions.put(ingName, inventoryDeductions.getOrDefault(ingName, 0.0) + qty);
                    }
                    cursor.close();
                } else {
                    inventoryDeductions.put(p.getName(), inventoryDeductions.getOrDefault(p.getName(), 0.0) + (double)qty);
                }
                for (Product addon : item.getSelectedAddons()) {
                    inventoryDeductions.put(addon.getName(), inventoryDeductions.getOrDefault(addon.getName(), 0.0) + (double)qty);
                }
            }

            for (Map.Entry<String, Double> entry : inventoryDeductions.entrySet()) {
                dbHelper.logInventoryChange(entry.getKey(), entry.getValue(), "SALE", "Customer Order");
            }

            db.setTransactionSuccessful();
            Intent intent = new Intent("com.example.inventory_salesanalytics_pointofsale_system.UPDATE_SALES");
            sendBroadcast(intent);

            showFeedback("Placed!", "Saved successfully.", SUCCESS_TYPE);
            cart.clear(); updateCartUI();
        } catch (Exception e) {
            e.printStackTrace();
            showFeedback("Error", "Transaction failed.", ERROR_TYPE);
        } finally {
            db.endTransaction();
        }
    }
}
