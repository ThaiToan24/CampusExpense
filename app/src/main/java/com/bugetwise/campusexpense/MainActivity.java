package com.bugetwise.campusexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SharedMemory;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.bugetwise.campusexpense.ui.auth.LoginActivity;
import com.bugetwise.campusexpense.ui.fragments.AccountFragment;
import com.bugetwise.campusexpense.ui.fragments.AddFragment;
import com.bugetwise.campusexpense.ui.fragments.BudgetFragment;
import com.bugetwise.campusexpense.ui.fragments.CategoryFragment;
import com.bugetwise.campusexpense.ui.fragments.ExpenseFragment;
import com.bugetwise.campusexpense.ui.fragments.HomeFragment;
import com.bugetwise.campusexpense.ui.fragments.TransactionFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private BottomNavigationView bottomNavigation;
    private boolean isLoggedIn(){
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }
    private void goToLoginActivity(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        if(!isLoggedIn()){
            goToLoginActivity();
            return;
        }

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_expense) {
                selectedFragment = new ExpenseFragment();
            }  else if (itemId == R.id.nav_budget) {
                selectedFragment = new BudgetFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new AccountFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void navigateToCategoryFragment() {
        CategoryFragment categoryFragment = new CategoryFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, categoryFragment)
                .addToBackStack(null)
                .commit();
    }
}