package com.dreamcreators.swimgoaltracker.screens;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends ComponentActivity {

    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;
    private Button btnLogin, btnGoogleSignIn;
    private ProgressBar progressBar;
    private TextView btnRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String deviceId;
    
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ThemeManager.isDarkMode(this)) {
            setTheme(R.style.AppTheme_NoActionBar_Dark);
        } else {
            setTheme(R.style.AppTheme_NoActionBar_Light);
        }
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));
        getWindow().setStatusBarColor(getColor(R.color.dark_surface_low));
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("device_mapping");

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        try {
                            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            progressBar.setVisibility(View.GONE);
                            showMessageDialog(false, "Sign In Failed", "Google sign in failed.");
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
        );

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    ivTogglePassword.setVisibility(View.VISIBLE);
                } else {
                    ivTogglePassword.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                loginUser();
            }
        });
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                registerUser();
            }
        });
        btnGoogleSignIn.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showMessageDialog(false, "Input Error", "Please enter your email address first.");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showMessageDialog(false, "Invalid Email", "Please enter a valid email address.");
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    showMessageDialog(true, "Email Sent", "Password reset instructions have been sent to your email.");
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.";
                    showMessageDialog(false, "Reset Failed", errorMsg);
                }
            });
        });

        TextView tvSupport = findViewById(R.id.tvSupport);
        TextView tvVersion = findViewById(R.id.tvVersion);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("Version 1.0");
        }

        tvSupport.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:+919787108096"));
            startActivity(callIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkDeviceMapping();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showMessageDialog(false, "Authentication Failed", "Please check your credentials and try again.");
                    }
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showMessageDialog(false, "Input Error", "Please enter email and password to login!");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && !user.isEmailVerified()) {
                            progressBar.setVisibility(View.GONE);
                            mAuth.signOut();
                            showMessageDialog(false, "Verification Required", "Please verify your email address from your email first to login.");
                        } else {
                            checkDeviceMapping();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showMessageDialog(false, "Authentication Failed", "Please check your email or password.");
                    }
                });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showMessageDialog(false, "Input Error", "Please enter email and password to register!");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessageDialog(false, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        if (password.length() < 6) {
            showMessageDialog(false, "Weak Password", "Password must be at least 6 characters.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    showMessageDialog(true, "Registration Successful", "Please check your email to verify your account.");
                                }
                            });
                            mDatabase.child(user.getUid()).setValue(deviceId)
                                    .addOnCompleteListener(dbTask -> {
                                        progressBar.setVisibility(View.GONE);
                                        mAuth.signOut(); // Sign out until they verify
                                    });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                        showMessageDialog(false, "Registration Failed", errorMsg);
                    }
                });
    }

    private void checkDeviceMapping() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        mDatabase.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                if (dataSnapshot.exists()) {
                    String registeredDeviceId = dataSnapshot.getValue(String.class);
                    if (deviceId.equals(registeredDeviceId)) {
                        // Same device — proceed directly
                        navigateToNext();
                    } else {
                        // Different device — show transfer confirmation
                        showDeviceTransferDialog(user);
                    }
                } else {
                    // No mapping exists yet — register this device
                    mDatabase.child(user.getUid()).setValue(deviceId).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            navigateToNext();
                        } else {
                            showMessageDialog(false, "Error", "Failed to register device.");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                showMessageDialog(false, "Database Error", "An error occurred while connecting to the database.");
            }
        });
    }

    private void showDeviceTransferDialog(FirebaseUser user) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_device_transfer, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvTransferTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvTransferMessage);
        
        tvTitle.setText("Device Conflict");
        tvMessage.setText("This account is currently active on another device. Do you want to deregister the old device and register this one?");
        
        Button btnCancel = dialogView.findViewById(R.id.btnTransferCancel);
        Button btnTransfer = dialogView.findViewById(R.id.btnTransferConfirm);

        btnTransfer.setVisibility(View.VISIBLE);
        btnTransfer.setText("Deregister Old");
        btnCancel.setText("Cancel");
        
        btnCancel.setOnClickListener(v -> {
            mAuth.signOut();
            dialog.dismiss();
        });

        btnTransfer.setOnClickListener(v -> {
            dialog.dismiss();
            progressBar.setVisibility(View.VISIBLE);
            mDatabase.child(user.getUid()).setValue(deviceId).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    showMessageDialog(true, "Device Registered", "Old device deregistered. This device is now active.");
                    navigateToNext();
                } else {
                    showMessageDialog(false, "Error", "Could not register this device. Please try again.");
                    mAuth.signOut();
                }
            });
        });

        dialog.show();
    }

    private void navigateToNext() {
        Intent next = new Intent(LoginActivity.this, SwitchSwimmerActivity.class);
        next.putExtra(SwitchSwimmerActivity.EXTRA_FROM_LOGIN, true);
        next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(next);
        finish();
    }

    private void showMessageDialog(boolean isSuccess, String title, String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_message, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivIcon = dialogView.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnOk = dialogView.findViewById(R.id.btnDialogOk);

        if (isSuccess) {
            ivIcon.setImageResource(R.drawable.ic_dialog_success);
        } else {
            ivIcon.setImageResource(R.drawable.ic_dialog_error);
        }

        tvTitle.setText(title);
        tvMessage.setText(message);

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
