package com.example.inventory_salesanalytics_pointofsale_system.fragments;

import static android.app.Activity.RESULT_OK;

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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.muddz.styleabletoast.StyleableToast;

public class POSFragment extends Fragment {

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String currentCategory = "Pizza";

    private TextView tvTotalAmount, tvCartItems;
    private Button btnConfirmOrder;
    private TextView btnCatPizza, btnCatDrinks;
    private View indicatorPizza, indicatorDrinks;

    private List<CartItem> cartList = new ArrayList<>();
    private double totalAmount = 0;

    private String selectedImageUri = "";
    private ImageView currentDialogImageView;

    private int drinkQty = 1;

    public static final int SUCCESS_TYPE = 0;
    public static final int ERROR_TYPE = 1;
    public static final int WARNING_TYPE = 2;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pos, container, false);
        dbHelper = new DatabaseHelper(getContext());
        rvProducts = view.findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvCartItems = view.findViewById(R.id.tvCartItems);
        btnConfirmOrder = view.findViewById(R.id.btnConfirmOrder);
        btnCatPizza = view.findViewById(R.id.btnCatPizza);
        btnCatDrinks = view.findViewById(R.id.btnCatDrinks);
        indicatorPizza = view.findViewById(R.id.indicatorPizza);
        indicatorDrinks = view.findViewById(R.id.indicatorDrinks);
        View layoutPizza = view.findViewById(R.id.layoutPizza);
        View layoutDrinks = view.findViewById(R.id.layoutDrinks);
        if (layoutPizza != null) layoutPizza.setOnClickListener(v -> switchCategory("Pizza"));
        if (layoutDrinks != null) layoutDrinks.setOnClickListener(v -> switchCategory("Drinks"));
        loadProducts(currentCategory);
        updateCartUI();
        updateCategoryUI();
        if (btnConfirmOrder != null) {
            btnConfirmOrder.setOnClickListener(v -> {
                if (cartList.isEmpty()) showFeedback("Empty Cart", "Please add items to your cart.", WARNING_TYPE);
                else showCartDialog();
            });
        }
        return view;
    }

    private void showFeedback(String title, String message, int type) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_feedback, null);
        ImageView ivIcon = dialogView.findViewById(R.id.ivFeedbackIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvFeedbackTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFeedbackMessage);
        Button btnAction = dialogView.findViewById(R.id.btnFeedbackAction);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (type == SUCCESS_TYPE) {
            ivIcon.setImageResource(R.drawable.ic_radio_selected);
            ivIcon.setColorFilter(Color.parseColor("#4ADE80"));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            btnAction.setTextColor(Color.parseColor("#166534"));
        } else if (type == ERROR_TYPE) {
            ivIcon.setImageResource(R.drawable.ic_close_thin);
            ivIcon.setColorFilter(Color.parseColor("#F87171"));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            btnAction.setTextColor(Color.parseColor("#991B1B"));
        } else if (type == WARNING_TYPE) {
            ivIcon.setImageResource(R.drawable.ic_settings_info);
            ivIcon.setColorFilter(Color.parseColor("#FBBF24"));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            btnAction.setTextColor(Color.parseColor("#92400E"));
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        btnAction.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        
        // MAKE CARD SMALLER (80% width)
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.80);
            window.setAttributes(lp);
        }
    }

    private void switchCategory(String category) {
        currentCategory = category;
        loadProducts(currentCategory);
        updateCategoryUI();
    }

    private void updateCategoryUI() {
        if (getContext() == null) return;
        boolean isPizza = "Pizza".equalsIgnoreCase(currentCategory);
        if (btnCatPizza != null) btnCatPizza.setTextColor(isPizza ? Color.parseColor("#1A1A1A") : Color.parseColor("#888888"));
        if (btnCatDrinks != null) btnCatDrinks.setTextColor(isPizza ? Color.parseColor("#888888") : Color.parseColor("#1A1A1A"));
        if (indicatorPizza != null) indicatorPizza.setVisibility(isPizza ? View.VISIBLE : View.GONE);
        if (indicatorDrinks != null) indicatorDrinks.setVisibility(isPizza ? View.GONE : View.VISIBLE);
    }

    private void loadProducts(String category) {
        productList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            if ("Drinks".equalsIgnoreCase(category)) {
                cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = ?", new String[]{category});
            } else {
                cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = ? GROUP BY " + DatabaseHelper.KEY_PRODUCT_NAME, new String[]{category});
            }
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    productList.add(new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5)));
                } while (cursor.moveToNext());
            }
        } finally { if (cursor != null) cursor.close(); }
        if ("Pizza".equalsIgnoreCase(category)) productList.add(new Product(-1, "Add Flavor", "Pizza", 0.0, "", "ic_add_flavor"));
        else if ("Drinks".equalsIgnoreCase(category)) productList.add(new Product(-1, "Add Drink", "Drinks", 0.0, "", ""));
        adapter = new ProductAdapter(productList, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onAddClick(Product product) { proceedToOrder(product); }
            @Override
            public void onEditClick(Product product) {
                if (product.getId() == -1) showAddFlavorDialog();
                else showEditProductDialog(product);
            }
        });
        rvProducts.setAdapter(adapter);
    }

    private void proceedToOrder(Product product) {
        if (product.getId() == -1) return;
        if ("Drinks".equalsIgnoreCase(product.getCategory())) { showDrinksQuantityDialog(product); return; }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_NAME + " = ? AND " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = ?", new String[]{product.getName(), product.getCategory()});
        List<Product> variants = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do { variants.add(new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5))); } while (cursor.moveToNext());
        }
        cursor.close();
        if (variants.size() > 1 || "Pizza".equalsIgnoreCase(product.getCategory())) showSizeSelectionDialog(product.getName(), variants, product.getCategory());
        else if (!variants.isEmpty()) {
            if (dbHelper.isProductAvailable(variants.get(0))) addToCart(new CartItem(variants.get(0), 1));
            else showFeedback("Out of Stock", "Insufficient ingredients.", ERROR_TYPE);
        }
    }

    private void showDrinksQuantityDialog(Product product) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_drinks_options, null);
        TextView tvName = dialogView.findViewById(R.id.tvDialogProductName);
        TextView tvPrice = dialogView.findViewById(R.id.tvDrinkPrice);
        TextView tvQtyValue = dialogView.findViewById(R.id.tvQtyValue);
        TextView tvTotalItemPrice = dialogView.findViewById(R.id.tvTotalItemPrice);
        Button btnMinus = dialogView.findViewById(R.id.btnMinusQty);
        Button btnPlus = dialogView.findViewById(R.id.btnPlusQty);
        Button btnAdd = dialogView.findViewById(R.id.btnDialogAddToCart);
        ImageView ivBg = dialogView.findViewById(R.id.ivDialogBg);
        ImageView ivProductSmall = dialogView.findViewById(R.id.ivProductSmall);
        ImageButton btnBack = dialogView.findViewById(R.id.btnDialogBack);
        drinkQty = 1;
        String displayName = product.getName() + (product.getSize().isEmpty() ? "" : " (" + product.getSize() + ")");
        tvName.setText(displayName);
        tvPrice.setText(String.format("₱ %.2f", product.getPrice()));
        tvQtyValue.setText(String.valueOf(drinkQty));
        tvTotalItemPrice.setText(String.format("Subtotal: ₱ %.2f", product.getPrice() * drinkQty));
        loadProductImage(product.getImageUrl(), ivBg);
        loadProductImage(product.getImageUrl(), ivProductSmall);
        btnMinus.setOnClickListener(v -> { if (drinkQty > 1) { drinkQty--; tvQtyValue.setText(String.valueOf(drinkQty)); tvTotalItemPrice.setText(String.format("Subtotal: ₱ %.2f", product.getPrice() * drinkQty)); } });
        btnPlus.setOnClickListener(v -> { drinkQty++; tvQtyValue.setText(String.valueOf(drinkQty)); tvTotalItemPrice.setText(String.format("Subtotal: ₱ %.2f", product.getPrice() * drinkQty)); });
        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                if (dbHelper.isProductAvailable(product)) { addToCart(new CartItem(product, drinkQty)); dialog.dismiss(); }
                else showFeedback("Out of Stock", product.getName() + " unavailable.", ERROR_TYPE);
            });
        }
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showSizeSelectionDialog(String productName, List<Product> variants, String category) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_product_options, null);
        TextView tvName = dialogView.findViewById(R.id.tvDialogProductName);
        LinearLayout llSizes = dialogView.findViewById(R.id.llSizes);
        LinearLayout llAddons = dialogView.findViewById(R.id.llAddons);
        View layoutAddons = dialogView.findViewById(R.id.layoutAddons);
        View layoutSizes = dialogView.findViewById(R.id.layoutSizes);
        Button btnAdd = dialogView.findViewById(R.id.btnDialogAddToCart);
        ImageView ivBg = dialogView.findViewById(R.id.ivDialogBg);
        ImageView ivProductSmall = dialogView.findViewById(R.id.ivProductSmall);
        ImageButton btnBack = dialogView.findViewById(R.id.btnDialogBack);
        tvName.setText(productName);
        if (!variants.isEmpty()) { loadProductImage(variants.get(0).getImageUrl(), ivBg); loadProductImage(variants.get(0).getImageUrl(), ivProductSmall); }
        boolean isPizza = "Pizza".equalsIgnoreCase(category);
        final List<Product> selectedVariants = new ArrayList<>();
        final List<Product> selectedAddons = new ArrayList<>();
        if (layoutAddons != null) {
            if (isPizza) {
                layoutAddons.setVisibility(View.VISIBLE);
                List<Product> addonList = loadAddons();
                if (llAddons != null) {
                    llAddons.removeAllViews();
                    for (Product addon : addonList) {
                        View addonView = LayoutInflater.from(getContext()).inflate(R.layout.item_addon_option, llAddons, false);
                        MaterialCardView card = addonView.findViewById(R.id.rootCard);
                        TextView tvAddonName = addonView.findViewById(R.id.tvAddonName);
                        TextView tvAddonPrice = addonView.findViewById(R.id.tvAddonPrice);
                        ImageView ivCheck = addonView.findViewById(R.id.ivCheck);
                        tvAddonName.setText(addon.getName());
                        tvAddonPrice.setText(String.format("+ ₱ %.2f", addon.getPrice()));
                        card.setOnClickListener(v -> {
                            if (selectedAddons.contains(addon)) { selectedAddons.remove(addon); card.setStrokeColor(Color.parseColor("#333333")); card.setCardBackgroundColor(Color.parseColor("#262626")); ivCheck.setImageResource(R.drawable.ic_radio_unselected); }
                            else { selectedAddons.add(addon); card.setStrokeColor(Color.parseColor("#D4A056")); card.setCardBackgroundColor(Color.parseColor("#33D4A056")); ivCheck.setImageResource(R.drawable.ic_radio_selected); }
                        });
                        llAddons.addView(addonView);
                    }
                }
            } else layoutAddons.setVisibility(View.GONE);
        }
        if (layoutSizes != null) {
            if (!variants.isEmpty()) {
                layoutSizes.setVisibility(View.VISIBLE);
                if (llSizes != null) {
                    llSizes.removeAllViews();
                    for (Product v : variants) {
                        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_size_option, llSizes, false);
                        MaterialCardView card = itemView.findViewById(R.id.rootCard);
                        TextView tvSize = itemView.findViewById(R.id.tvSize);
                        TextView tvPrice = itemView.findViewById(R.id.tvPrice);
                        ImageView ivRadio = itemView.findViewById(R.id.ivRadio);
                        tvSize.setText(v.getSize().replace("\"", "") + " inch");
                        tvPrice.setText(String.format("₱ %.2f", v.getPrice()));
                        if (!dbHelper.isProductAvailable(v)) { card.setEnabled(false); card.setAlpha(0.4f); tvSize.setText(tvSize.getText() + " (Sold Out)"); }
                        else {
                            card.setOnClickListener(view -> {
                                selectedVariants.clear(); selectedVariants.add(v);
                                for(int i=0; i<llSizes.getChildCount(); i++){
                                    View child = llSizes.getChildAt(i); MaterialCardView c = child.findViewById(R.id.rootCard); ImageView radio = child.findViewById(R.id.ivRadio);
                                    if(c != card){ c.setStrokeColor(Color.parseColor("#333333")); c.setCardBackgroundColor(Color.parseColor("#262626")); radio.setImageResource(R.drawable.ic_radio_unselected); }
                                }
                                card.setStrokeColor(Color.parseColor("#D4A056")); card.setCardBackgroundColor(Color.parseColor("#33D4A056")); ivRadio.setImageResource(R.drawable.ic_radio_selected);
                            });
                        }
                        llSizes.addView(itemView);
                    }
                }
            } else layoutSizes.setVisibility(View.GONE);
        }
        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                if (selectedVariants.isEmpty() && !variants.isEmpty()) { Toast.makeText(getContext(), "Select a size", Toast.LENGTH_SHORT).show(); return; }
                for (Product sv : selectedVariants) {
                    CartItem item = new CartItem(sv, 1);
                    if (isPizza) item.setSelectedAddons(new ArrayList<>(selectedAddons));
                    addToCart(item);
                }
                dialog.dismiss();
            });
        }
        dialog.show();
        forceDialogWidth(dialog);
    }

    private List<Product> loadAddons() {
        List<Product> addons = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_PRODUCTS + " WHERE " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = 'Addons'", null);
        if (cursor.moveToFirst()) { do { addons.add(new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5))); } while (cursor.moveToNext()); }
        cursor.close();
        return addons;
    }

    private void addToCart(CartItem newItem) {
        boolean exists = false;
        for (CartItem item : cartList) {
            if (item.getProduct().equals(newItem.getProduct()) && item.getSelectedAddons().equals(newItem.getSelectedAddons())) { item.setQuantity(item.getQuantity() + newItem.getQuantity()); exists = true; break; }
        }
        if (!exists) cartList.add(newItem);
        updateCartUI();
        StyleableToast.makeText(getContext(), newItem.getProduct().getName() + " added", R.style.SuccessToast).show();
    }

    private void updateCartUI() {
        totalAmount = 0; int totalQty = 0;
        for (CartItem item : cartList) { totalAmount += item.getTotalPrice(); totalQty += item.getQuantity(); }
        if (tvCartItems != null) tvCartItems.setText("Cart: " + totalQty + " items");
        if (tvTotalAmount != null) tvTotalAmount.setText(String.format("₱ %.2f", totalAmount));
    }

    private void showCartDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_cart, null);
        RecyclerView rvCart = dialogView.findViewById(R.id.rvOrderList);
        TextView tvTotal = dialogView.findViewById(R.id.tvCartTotal);
        Button btnConfirm = dialogView.findViewById(R.id.btnCartCheckout);
        ImageButton btnBack = dialogView.findViewById(R.id.btnCartBack);
        EditText etCustomerName = dialogView.findViewById(R.id.etCustomerName);
        rvCart.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCart.setAdapter(new CartAdapter(cartList, new CartAdapter.OnCartChangeListener() {
            @Override public void onRemove(CartItem item) { cartList.remove(item); updateCartUI(); if (tvTotal != null) tvTotal.setText(String.format("₱ %.2f", totalAmount)); }
            @Override public void onQuantityChange(CartItem item, int newQty) { item.setQuantity(newQty); updateCartUI(); if (tvTotal != null) tvTotal.setText(String.format("₱ %.2f", totalAmount)); }
        }));
        if (tvTotal != null) tvTotal.setText(String.format("₱ %.2f", totalAmount));
        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) { btnConfirm.setOnClickListener(v -> { String name = etCustomerName.getText().toString().trim(); if (name.isEmpty()) { etCustomerName.setError("Required"); return; } showReviewOrderDialog(name, dialog); }); }
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showReviewOrderDialog(String customerName, AlertDialog cartDialog) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_review_order, null);
        TextView tvCustName = dialogView.findViewById(R.id.tvReviewCustomerName);
        TextView tvSummary = dialogView.findViewById(R.id.tvReviewOrderSummary);
        TextView tvTotal = dialogView.findViewById(R.id.tvReviewTotalAmount);
        Button btnConfirm = dialogView.findViewById(R.id.btnReviewConfirm);
        Button btnEdit = dialogView.findViewById(R.id.btnReviewEdit);
        if (tvCustName != null) tvCustName.setText(customerName);
        StringBuilder orderSummary = new StringBuilder();
        for (CartItem item : cartList) {
            orderSummary.append("• ").append(item.getProduct().getName()).append(" (").append(item.getProduct().getSize()).append(")\n");
            orderSummary.append("  x").append(item.getQuantity()).append(" - ₱ ").append(String.format("%.2f", item.getTotalPrice())).append("\n");
        }
        if (tvSummary != null) tvSummary.setText(orderSummary.toString());
        if (tvTotal != null) tvTotal.setText(String.format("₱ %.2f", totalAmount));
        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (btnConfirm != null) { btnConfirm.setOnClickListener(v -> { handleCheckout(customerName); dialog.dismiss(); cartDialog.dismiss(); }); }
        if (btnEdit != null) btnEdit.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void handleCheckout(String customerName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues saleValues = new ContentValues();
            saleValues.put(DatabaseHelper.KEY_SALE_TOTAL_AMOUNT, totalAmount);
            saleValues.put(DatabaseHelper.KEY_SALE_CUSTOMER_NAME, customerName);
            saleValues.put(DatabaseHelper.KEY_SALE_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            long saleId = db.insert(DatabaseHelper.TABLE_SALES, null, saleValues);
            for (CartItem item : cartList) {
                Product p = item.getProduct(); int qty = item.getQuantity();
                ContentValues odValues = new ContentValues();
                odValues.put(DatabaseHelper.KEY_OD_SALE_ID, saleId);
                odValues.put(DatabaseHelper.KEY_OD_PRODUCT_ID, p.getId());
                odValues.put(DatabaseHelper.KEY_OD_QUANTITY, qty);
                odValues.put(DatabaseHelper.KEY_OD_SUBTOTAL, item.getTotalPrice());
                db.insert(DatabaseHelper.TABLE_ORDER_DETAILS, null, odValues);
                dbHelper.logInventoryChange(p.getName(), (double)qty, "SALE", "Order");
            }
            db.setTransactionSuccessful();
            cartList.clear(); updateCartUI();
            if (getContext() != null) getContext().sendBroadcast(new Intent("com.example.inventory_salesanalytics_pointofsale_system.UPDATE_SALES"));
            showFeedback("Order Placed!", "Your order has been saved successfully.", SUCCESS_TYPE);
        } catch (Exception e) { showFeedback("Failed", "Error saving order.", ERROR_TYPE); }
        finally { db.endTransaction(); }
    }

    private void loadProductImage(String imagePath, ImageView imageView) {
        if (getContext() == null || imageView == null || imagePath == null || imagePath.isEmpty()) { if (imageView != null) imageView.setImageResource(R.drawable.login_pizza); return; }
        Glide.with(getContext()).load(imagePath.contains("http") || imagePath.contains("/") ? imagePath : "file:///android_asset/" + imagePath).centerCrop().into(imageView);
    }

    private void showAddFlavorDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_product, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvAddFlavorTitle);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilFlavorName);
        TextInputEditText etName = dialogView.findViewById(R.id.etFlavorName);
        TextInputLayout tilFixedPrice = dialogView.findViewById(R.id.tilFixedPrice);
        TextInputEditText etPriceFixed = dialogView.findViewById(R.id.etPriceFixed);
        CheckBox cbFixedSize = dialogView.findViewById(R.id.cbFixedSize);
        View llMultiSize = dialogView.findViewById(R.id.llMultiSize);
        TextInputLayout tilToppings = dialogView.findViewById(R.id.tilToppings);
        TextInputEditText etToppings = dialogView.findViewById(R.id.etToppings);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnAddFlavorCancel);
        ImageView ivProduct = dialogView.findViewById(R.id.ivAddFlavorImage);
        Button btnSave = dialogView.findViewById(R.id.btnAddFlavorSave);
        View addImageContainer = dialogView.findViewById(R.id.addImageContainer);
        
        currentDialogImageView = ivProduct;
        selectedImageUri = "";
        
        if ("Drinks".equalsIgnoreCase(currentCategory)) { 
            tvTitle.setText("Add New Drink");
            if (tilName != null) tilName.setHint("Drink Name");
            cbFixedSize.setChecked(true); 
            cbFixedSize.setVisibility(View.GONE); 
            llMultiSize.setVisibility(View.GONE);
            if (tilFixedPrice != null) {
                tilFixedPrice.setVisibility(View.VISIBLE);
                tilFixedPrice.setHint("Price");
            }
            if (tilToppings != null) {
                tilToppings.setVisibility(View.VISIBLE);
                tilToppings.setHint("Size (e.g. 500ml, 1.5L)");
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        cbFixedSize.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                llMultiSize.setVisibility(View.GONE);
                tilFixedPrice.setVisibility(View.VISIBLE);
            } else {
                llMultiSize.setVisibility(View.VISIBLE);
                tilFixedPrice.setVisibility(View.GONE);
            }
        });

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (addImageContainer != null) {
            addImageContainer.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
            });
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim(); if (name.isEmpty()) return;
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            try {
                String finalSize = "11\"";
                if ("Drinks".equalsIgnoreCase(currentCategory)) {
                    finalSize = etToppings.getText().toString().trim();
                }

                if (cbFixedSize.isChecked()) {
                    double price = Double.parseDouble(etPriceFixed.getText().toString());
                    insertFlavor(db, name, price, finalSize, selectedImageUri, "", currentCategory);
                } else {
                    TextInputEditText et9 = dialogView.findViewById(R.id.etPrice9);
                    TextInputEditText et11 = dialogView.findViewById(R.id.etPrice11);
                    insertFlavor(db, name, Double.parseDouble(et9.getText().toString()), "9\"", selectedImageUri, "", currentCategory);
                    insertFlavor(db, name, Double.parseDouble(et11.getText().toString()), "11\"", selectedImageUri, "", currentCategory);
                }
                loadProducts(currentCategory); 
                dialog.dismiss();
                showFeedback("Product Added", name + " added.", SUCCESS_TYPE);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Please enter valid details", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        forceDialogWidth(dialog);
    }

    private void insertFlavor(SQLiteDatabase db, String name, double price, String size, String image, String toppings, String category) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.KEY_PRODUCT_NAME, name); v.put(DatabaseHelper.KEY_PRODUCT_CATEGORY, category); v.put(DatabaseHelper.KEY_PRODUCT_PRICE, price); v.put(DatabaseHelper.KEY_PRODUCT_SIZE, size); v.put(DatabaseHelper.KEY_PRODUCT_IMAGE, image);
        db.insert(DatabaseHelper.TABLE_PRODUCTS, null, v);
    }

    private void showEditProductDialog(Product product) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_product, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tvEditProductTitle);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilEditFlavorName);
        TextInputEditText etName = dialogView.findViewById(R.id.etEditFlavorName);
        CheckBox cbFixedSize = dialogView.findViewById(R.id.cbEditFixedSize);
        View llMultiSize = dialogView.findViewById(R.id.llEditMultiSize);
        TextInputLayout tilFixedPrice = dialogView.findViewById(R.id.tilEditFixedPrice);
        TextInputEditText etPriceFixed = dialogView.findViewById(R.id.etEditPriceFixed);
        TextInputLayout tilToppings = dialogView.findViewById(R.id.tilEditToppings);
        TextInputEditText etToppings = dialogView.findViewById(R.id.etEditToppings);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnEditProductCancel);
        ImageView ivProduct = dialogView.findViewById(R.id.ivEditProductImage);
        Button btnSave = dialogView.findViewById(R.id.btnEditProductSave);
        Button btnDelete = dialogView.findViewById(R.id.btnEditProductDelete);
        View imageContainer = dialogView.findViewById(R.id.editImageContainer);

        if (etName != null) etName.setText(product.getName());
        currentDialogImageView = ivProduct;
        selectedImageUri = product.getImageUrl();
        loadProductImage(selectedImageUri, ivProduct);

        if ("Drinks".equalsIgnoreCase(product.getCategory())) {
            tvTitle.setText("Edit Drink");
            if (tilName != null) tilName.setHint("Drink Name");
            cbFixedSize.setVisibility(View.GONE);
            llMultiSize.setVisibility(View.GONE);
            if (tilFixedPrice != null) {
                tilFixedPrice.setVisibility(View.VISIBLE);
                tilFixedPrice.setHint("Price");
                etPriceFixed.setText(String.valueOf(product.getPrice()));
            }
            if (tilToppings != null) {
                tilToppings.setVisibility(View.VISIBLE);
                tilToppings.setHint("Size (e.g. 500ml, 1.5L)");
                etToppings.setText(product.getSize());
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnCancel.setElevation(30 * getResources().getDisplayMetrics().density); // Forcing elevation in code too
        }

        if (imageContainer != null) {
            imageContainer.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent);
            });
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim(); if (name.isEmpty()) return;
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(DatabaseHelper.TABLE_PRODUCTS, DatabaseHelper.KEY_PRODUCT_NAME + " = ? AND " + DatabaseHelper.KEY_PRODUCT_CATEGORY + " = ?", new String[]{product.getName(), product.getCategory()});
            
            try {
                if ("Drinks".equalsIgnoreCase(product.getCategory())) {
                    double price = Double.parseDouble(etPriceFixed.getText().toString());
                    String size = etToppings.getText().toString().trim();
                    insertFlavor(db, name, price, size, selectedImageUri, "", product.getCategory());
                } else {
                    // Re-insert pizza logic (simplified for brevity)
                    if (cbFixedSize.isChecked()) {
                        insertFlavor(db, name, Double.parseDouble(etPriceFixed.getText().toString()), "11\"", selectedImageUri, "", product.getCategory());
                    } else {
                        TextInputEditText et9 = dialogView.findViewById(R.id.etEditPrice9);
                        TextInputEditText et11 = dialogView.findViewById(R.id.etEditPrice11);
                        insertFlavor(db, name, Double.parseDouble(et9.getText().toString()), "9\"", selectedImageUri, "", product.getCategory());
                        insertFlavor(db, name, Double.parseDouble(et11.getText().toString()), "11\"", selectedImageUri, "", product.getCategory());
                    }
                }
                loadProducts(currentCategory);
                dialog.dismiss();
                showFeedback("Saved", name + " updated.", SUCCESS_TYPE);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Please enter valid details", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete?")
                        .setMessage("Remove " + product.getName() + "?")
                        .setPositiveButton("Delete", (d, i) -> {
                            dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_PRODUCTS, DatabaseHelper.KEY_PRODUCT_NAME + " = ?", new String[]{product.getName()});
                            loadProducts(currentCategory); dialog.dismiss();
                            showFeedback("Deleted", product.getName() + " removed.", SUCCESS_TYPE);
                        })
                        .setNegativeButton("Cancel", null).show();
            });
        }
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
