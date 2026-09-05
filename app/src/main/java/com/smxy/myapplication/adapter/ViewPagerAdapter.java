package com.smxy.myapplication.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();
    private final List<String> titleList = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addFragment(Fragment fragment, String title) {
        if (fragment != null) {
            fragmentList.add(fragment);
            titleList.add(ErrorHandler.getSafeString(title, ""));
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            return fragmentList.get(position);
        }
        return new Fragment();
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    public String getPageTitle(int position) {
        if (position >= 0 && position < titleList.size()) {
            return titleList.get(position);
        }
        return "";
    }

    public void clear() {
        fragmentList.clear();
        titleList.clear();
        notifyDataSetChanged();
    }
}