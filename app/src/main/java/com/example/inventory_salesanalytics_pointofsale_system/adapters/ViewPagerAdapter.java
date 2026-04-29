package com.example.inventory_salesanalytics_pointofsale_system.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.inventory_salesanalytics_pointofsale_system.fragments.DashboardFragment;
import com.example.inventory_salesanalytics_pointofsale_system.fragments.InventoryFragment;
import com.example.inventory_salesanalytics_pointofsale_system.fragments.POSFragment;
import com.example.inventory_salesanalytics_pointofsale_system.fragments.SalesFragment;
import com.example.inventory_salesanalytics_pointofsale_system.fragments.SettingsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private int itemCount = 4; // Default to 4 swipeable pages (Home, POS, Inventory, Sales)

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setItemCount(int count) {
        if (this.itemCount != count) {
            this.itemCount = count;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new DashboardFragment();
            case 1: return new POSFragment();
            case 2: return new InventoryFragment();
            case 3: return new SalesFragment();
            case 4: return new SettingsFragment();
            default: return new DashboardFragment();
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }
}
