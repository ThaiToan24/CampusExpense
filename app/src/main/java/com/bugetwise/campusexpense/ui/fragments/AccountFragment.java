package com.bugetwise.campusexpense.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bugetwise.campusexpense.MainActivity;
import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.ui.auth.LoginActivity;

public class AccountFragment extends Fragment {
    private TextView welcomeText;
    private TextView usernameText;
    private TextView avatarText;
    private Button categoryButton;
    private Button logoutButton;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        welcomeText = view.findViewById(R.id.welcomeText);
        usernameText = view.findViewById(R.id.usernameText);
        avatarText = view.findViewById(R.id.avatarText);
        categoryButton = view.findViewById(R.id.categoryButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        sharedPreferences = getActivity().getSharedPreferences("UserSession",0);

        String username = sharedPreferences.getString("username", "");
        welcomeText.setText("Welcome");
        usernameText.setText(username);

        if (!username.isEmpty()) {
            String firstLetter = username.substring(0, 1).toUpperCase();
            avatarText.setText(firstLetter);
        }

        categoryButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCategoryFragment();
            }
        });

        logoutButton.setOnClickListener(v -> logout());
            return view;

    }
    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

}
