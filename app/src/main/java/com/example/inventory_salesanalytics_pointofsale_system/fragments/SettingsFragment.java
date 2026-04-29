package com.example.inventory_salesanalytics_pointofsale_system.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.inventory_salesanalytics_pointofsale_system.MainActivity;
import com.example.inventory_salesanalytics_pointofsale_system.R;
import com.example.inventory_salesanalytics_pointofsale_system.database.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private Uri selectedImageUri;
    private ImageView ivPreview;
    private ImageView ivAdminProfile;
    private TextView tvAdminName;
    private String loggedEmail;
    
    // Constant types for feedback
    public static final int SUCCESS_TYPE = 0;
    public static final int ERROR_TYPE = 1;
    public static final int WARNING_TYPE = 2;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    selectedImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (Exception e) {
                            Log.e("SettingsFragment", "Permission error: " + e.getMessage());
                        }
                        
                        if (ivPreview != null) {
                            Glide.with(this).load(selectedImageUri).circleCrop().into(ivPreview);
                        }
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        dbHelper = new DatabaseHelper(getContext());
        
        SharedPreferences prefs = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        loggedEmail = prefs.getString("loggedEmail", null);

        ivAdminProfile = view.findViewById(R.id.ivAdminProfile);
        tvAdminName = view.findViewById(R.id.tvAdminName);
        
        loadAdminData();
        setupClickListeners(view);
        return view;
    }

    private Cursor getCurrentAdminCursor() {
        if (loggedEmail == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                loggedEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }
        }

        if (loggedEmail != null) {
            Cursor cursor = dbHelper.getAdminDataByEmail(loggedEmail);
            if (cursor != null && cursor.moveToFirst()) return cursor;
            if (cursor != null) cursor.close();

            cursor = dbHelper.getAdminDataByUsername(loggedEmail);
            if (cursor != null && cursor.moveToFirst()) return cursor;
            if (cursor != null) cursor.close();
        }

        return dbHelper.getAdminData();
    }

    private void loadAdminData() {
        Cursor cursor = getCurrentAdminCursor();

        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(1);
            String imagePath = cursor.getString(4);
            
            if (tvAdminName != null) {
                tvAdminName.setText(username);
            }
            
            if (ivAdminProfile != null) {
                if (imagePath != null && !imagePath.isEmpty()) {
                    Glide.with(this)
                            .load(Uri.parse(imagePath))
                            .circleCrop()
                            .placeholder(R.drawable.ic_person_head)
                            .error(R.drawable.ic_person_head)
                            .into(ivAdminProfile);
                } else {
                    ivAdminProfile.setImageResource(R.drawable.ic_person_head);
                }
            }
            cursor.close();
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        
        View btnAbout = view.findViewById(R.id.btnAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> showCustomAboutDialog());
        }

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showCustomLogoutDialog());
    }

    private void showFeedback(String title, String message, int type) {
        if (getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_feedback, null);
        FrameLayout iconContainer = dialogView.findViewById(R.id.flIconContainer);
        ImageView ivIcon = dialogView.findViewById(R.id.ivFeedbackIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvFeedbackTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFeedbackMessage);
        Button btnAction = dialogView.findViewById(R.id.btnFeedbackAction);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (type == SUCCESS_TYPE) {
            iconContainer.setBackgroundResource(R.drawable.bg_circle_success);
            ivIcon.setImageResource(R.drawable.ic_radio_selected);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_success_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_success_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_success_text));
        } else if (type == ERROR_TYPE) {
            iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_error_bg)));
            ivIcon.setImageResource(R.drawable.ic_close_thin);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_error_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_error_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_error_text));
        } else if (type == WARNING_TYPE) {
            iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_warning_bg)));
            ivIcon.setImageResource(R.drawable.ic_settings_info);
            ivIcon.setColorFilter(getResources().getColor(R.color.feedback_warning_icon));
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.feedback_warning_bg)));
            btnAction.setTextColor(getResources().getColor(R.color.feedback_warning_text));
        }

        AlertDialog dialog = createTransparentDialog(dialogView);
        btnAction.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showCustomAboutDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_about, null);
        AlertDialog dialog = createTransparentDialog(dialogView);
        
        dialogView.findViewById(R.id.btnAboutOk).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showCustomLogoutDialog() {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout, null);
        AlertDialog dialog = createTransparentDialog(dialogView);

        Button btnCancel = dialogView.findViewById(R.id.btnCancelLogout);
        Button btnLogout = dialogView.findViewById(R.id.btnConfirmLogout);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).performLogout();
            }
        });

        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        ivPreview = dialogView.findViewById(R.id.ivProfilePreview);
        View btnPickImage = dialogView.findViewById(R.id.btnPickImage);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdateProfile);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnCancelEdit);

        Cursor cursor = getCurrentAdminCursor();

        String currentUsername = "";
        String currentPassword = "";
        String currentEmail = "";
        String currentImagePath = "";

        if (cursor != null && cursor.moveToFirst()) {
            currentUsername = cursor.getString(1);
            currentPassword = cursor.getString(2);
            currentEmail = cursor.getString(3);
            currentImagePath = cursor.getString(4);
            
            etUsername.setText(currentUsername);
            etEmail.setText(currentEmail);
            
            if (currentImagePath != null && !currentImagePath.isEmpty()) {
                Glide.with(this).load(Uri.parse(currentImagePath)).circleCrop().into(ivPreview);
            }
            cursor.close();
        }

        selectedImageUri = (currentImagePath != null && !currentImagePath.isEmpty()) ? Uri.parse(currentImagePath) : null;

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        AlertDialog dialog = createTransparentDialog(dialogView);

        final String finalCurrentUsername = currentUsername;
        final String finalCurrentPassword = currentPassword;
        btnUpdate.setOnClickListener(v -> {
            String newUsername = etUsername.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String imagePath = (selectedImageUri != null) ? selectedImageUri.toString() : "";

            if (newUsername.isEmpty() || newEmail.isEmpty()) {
                showFeedback("Error", "All fields are required", ERROR_TYPE);
                return;
            }

            if (dbHelper.updateAdmin(finalCurrentUsername, newUsername, newEmail, finalCurrentPassword, imagePath)) {
                dialog.dismiss();
                showFeedback("Success", "Profile updated successfully!", SUCCESS_TYPE);
                
                if (!newEmail.equals(loggedEmail)) {
                    loggedEmail = newEmail;
                    SharedPreferences.Editor editor = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE).edit();
                    editor.putString("loggedEmail", newEmail);
                    editor.apply();
                }
                loadAdminData();
            } else {
                showFeedback("Error", "Failed to update profile", ERROR_TYPE);
            }
        });

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private void showChangePasswordDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        EditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNew = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdatePassword);
        ImageButton btnCancel = dialogView.findViewById(R.id.btnCancelChange);

        AlertDialog dialog = createTransparentDialog(dialogView);

        btnUpdate.setOnClickListener(v -> {
            String currentInput = etCurrent.getText().toString().trim();
            String newPass = etNew.getText().toString().trim();
            String confirmPass = etConfirm.getText().toString().trim();

            if (currentInput.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                showFeedback("Error", "All fields are required", ERROR_TYPE);
                return;
            }

            Cursor cursor = getCurrentAdminCursor();

            if (cursor != null && cursor.moveToFirst()) {
                String storedPassword = cursor.getString(2).trim();
                String username = cursor.getString(1);

                if (!currentInput.equals(storedPassword)) {
                    showFeedback("Error", "Current password incorrect", ERROR_TYPE);
                } else if (newPass.length() < 6) {
                    showFeedback("Error", "New password must be at least 6 characters", ERROR_TYPE);
                } else if (!newPass.equals(confirmPass)) {
                    showFeedback("Error", "New passwords do not match", ERROR_TYPE);
                } else {
                    if (dbHelper.updateAdminPassword(username, newPass)) {
                        dialog.dismiss();
                        showFeedback("Success", "Password changed successfully", SUCCESS_TYPE);
                    } else {
                        showFeedback("Error", "Failed to update database", ERROR_TYPE);
                    }
                }
                cursor.close();
            } else {
                if (cursor != null) cursor.close();
                showFeedback("Error", "Account record not found", ERROR_TYPE);
            }
        });

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        forceDialogWidth(dialog);
    }

    private AlertDialog createTransparentDialog(View view) {
        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
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
