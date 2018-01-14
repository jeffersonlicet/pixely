package net.sparkly.pixely.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.sparkly.pixely.R;
import net.sparkly.pixely.utils.PixUtils;

public class SplashActivity extends BaseActivity {

    private static final int PERMISSION_ALL = 1;
    private static final String TAG = SplashActivity.class.getSimpleName();
    private boolean canProceed;
    private MaterialDialog request, denied;

    private String[] PERMISSIONS = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildRequestDialog();
        buildDeniedPermDialog();

        askMarshMallowPermission();
    }

    private void buildRequestDialog() {
        request = new MaterialDialog.Builder(this)
            .title("Hello")
            .content("Welcome to Pixely. Next, we will ask the necessary permissions to give you the best experience.")
            .positiveText("Continue")
            .negativeText("Cancel")
            .canceledOnTouchOutside(false)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    onPositive(request);
                }
            })
            .dismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (canProceed) {
                        requestPerm();
                    }
                }
            })
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    onNegative();
                }
            }).build();
    }

    private void buildDeniedPermDialog() {
        denied = new MaterialDialog.Builder(this)
            .title("Hello")
            .content("To work properly we need you to grant the permissions.")
            .positiveText("Tray again")
            .negativeText("Close")
            .canceledOnTouchOutside(false)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    onPositive(denied);
                }
            })
            .dismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (canProceed) {
                        requestPerm();
                    }
                }
            })
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    finish();
                }
            }).build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initApp();
                } else {
                    onNegative();
                }
            }
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void askMarshMallowPermission() {
        if (!hasPermissions(this, PERMISSIONS)) {
            request.show();
        } else {
            initApp();
        }
    }

    private void onPositive(MaterialDialog dialog) {
        canProceed = true;
        dialog.dismiss();
    }

    private void requestPerm() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
    }

    private void onNegative() {
        canProceed = false;
        denied.show();
    }

    private void initApp() {

        int delayValues = 0;

        if (!getBoolean("launchedBefore")) {
            delayValues = 500;
            setBoolean("launchedBefore", true);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent actualIntent = getIntent();
                String action = actualIntent.getAction();
                String type = actualIntent.getType();
                String imageUri = null;

                if (Intent.ACTION_EDIT.equals(action) && type != null && type.startsWith("image/")) {
                    imageUri = PixUtils.contentToPath(getApplicationContext(), actualIntent.getData());
                }

                if (imageUri != null) {
                    Intent editActivity = new Intent(getApplicationContext(), EditorActivity.class);
                    editActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    editActivity.putExtra("photoUri", imageUri);
                    startActivity(editActivity);
                    overridePendingTransition(0, 0);

                } else {
                    Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    overridePendingTransition(0,0);
                }
            }
        }, 0);
    }

    @Override
    public void finish() {
        super.finish();
        //overridePendingTransition(0, 0);
    }
}
