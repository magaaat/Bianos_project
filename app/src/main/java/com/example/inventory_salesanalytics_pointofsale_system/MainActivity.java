package com.example.inventory_salesanalytics_pointofsale_system;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.inventory_salesanalytics_pointofsale_system.activities.LoginActivity;
import com.example.inventory_salesanalytics_pointofsale_system.adapters.ViewPagerAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private ViewPagerAdapter adapter;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onStart() {
        super.onStart();
        checkUserLogin();
    }

    private void checkUserLogin() {
        boolean isFirebaseLoggedIn = mAuth.getCurrentUser() != null;
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean isLocalLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (!isFirebaseLoggedIn && !isLocalLoggedIn) {
            goToLogin();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);
        
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (mAuth.getCurrentUser() == null && !prefs.getBoolean("isLoggedIn", false)) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        askNotificationPermission();

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_navigation);
        
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Enable swiping for the modules
        viewPager.setUserInputEnabled(true);

        bottomNav.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.TRANSPARENT));
        bottomNav.setItemBackgroundResource(android.R.color.transparent);
        bottomNav.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        bottomNav.setOnItemSelectedListener(item -> {
            // If coming from Settings, reset itemCount to 4 to restrict swiping
            if (adapter.getItemCount() == 5) {
                adapter.setItemCount(4);
                viewPager.setUserInputEnabled(true);
            }
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, true);
            } else if (itemId == R.id.nav_pos) {
                viewPager.setCurrentItem(1, true);
            } else if (itemId == R.id.nav_inventory) {
                viewPager.setCurrentItem(2, true);
            } else if (itemId == R.id.nav_sales) {
                viewPager.setCurrentItem(3, true);
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Scroll the fragment to top when selected
                scrollToTopOfFragment(position);

                int itemId;
                switch (position) {
                    case 0: itemId = R.id.nav_home; break;
                    case 1: itemId = R.id.nav_pos; break;
                    case 2: itemId = R.id.nav_inventory; break;
                    case 3: itemId = R.id.nav_sales; break;
                    default: itemId = -1; 
                }
                
                if (itemId != -1) {
                    bottomNav.setSelectedItemId(itemId);
                } else {
                    uncheckBottomNavItems();
                }
            }
        });
        
        loadAdminData();

        // Handle back button with customized Material Design confirmation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Exit Application")
                        .setMessage("Are you sure you want to exit Biano's Pizza Management App?")
                        .setPositiveButton("Exit", (d, which) -> finish())
                        .setNegativeButton("Cancel", null)
                        .create();
                
                dialog.show();
                
                // Set Exit button to Red
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                // Set Cancel button to Black
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            }
        });
    }

    private void scrollToTopOfFragment(int position) {
        // Find the currently displayed fragment through the ViewPager's adapter
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
        if (fragment != null && fragment.getView() != null) {
            // Find NestedScrollView in the fragment layout
            View scrollView = fragment.getView().findViewWithTag("scroll_view_tag");
            if (scrollView == null) {
                // Fallback: search for any NestedScrollView or ScrollView
                scrollView = findScrollView(fragment.getView());
            }

            if (scrollView != null) {
                final View finalScrollView = scrollView;
                finalScrollView.post(() -> finalScrollView.scrollTo(0, 0));
            }
        }
    }

    private View findScrollView(View view) {
        if (view instanceof NestedScrollView || view instanceof android.widget.ScrollView) {
            return view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = findScrollView(group.getChildAt(i));
                if (child != null) return child;
            }
        }
        return null;
    }

    private void uncheckBottomNavItems() {
        bottomNav.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }
        bottomNav.getMenu().setGroupCheckable(0, true, true);
    }

    public void loadAdminData() {
        Cursor cursor = dbHelper.getAdminData();
        if (cursor != null) {
            cursor.close();
        }
    }

    public void navigateToSettings() {
        if (viewPager != null) {
            // Set itemCount to 5 to allow navigation to Settings, then lock swiping
            adapter.setItemCount(5);
            viewPager.setCurrentItem(4, true);
            viewPager.setUserInputEnabled(false);
        }
    }

    public void setCurrentTab(int position) {
        if (viewPager != null) {
            viewPager.setCurrentItem(position, true);
        }
    }

    public void performLogout() {
        mAuth.signOut();
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();
        goToLogin();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
