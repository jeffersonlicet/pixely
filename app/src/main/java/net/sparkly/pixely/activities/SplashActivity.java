package net.sparkly.pixely.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.sparkly.pixely.R;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;

public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_ALL = 1;
    private boolean canProceed;
    MaterialDialog request;
    MaterialDialog denied;
    /**
     *
     */
    private String[] PERMISSIONS = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);


        askMarshMallowPermission();
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
        Log.d("HOLA", hasPermissions(this, PERMISSIONS) + " Permissions");
        if (!hasPermissions(this, PERMISSIONS)) {

            try {

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
                            if(canProceed)
                            {
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

                request.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
            initApp();
        }
    }

    private void onPositive(MaterialDialog dialog) {
        canProceed = true;
        dialog.dismiss();
    }

    private void requestPerm()
    {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);

    }

    private void onNegative() {
        canProceed = false;

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
                    if(canProceed)
                    {
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

        denied.show();
    }

    private void initApp() {
        Timer tm = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        };

        tm.schedule(tt, 10);

    }
}
