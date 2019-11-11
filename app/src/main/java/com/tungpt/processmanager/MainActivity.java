package com.tungpt.processmanager;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "aaa";
    private Button mButton;
    private Dialog mDialogPermission;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = findViewById(R.id.button_permission);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });

        UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

        if (stats == null || stats.isEmpty()) {
            dialogPermission(this);
        } else {
            Toast.makeText(this, "Permission is Granted", Toast.LENGTH_LONG).show();
        }

//        Process process;
//        try {
//            process = Runtime.getRuntime().exec("adb shell 'ps'");
//            BufferedReader input = new BufferedReader(new InputStreamReader(
//                    process.getInputStream()));
//
//            String line = null;
//
//            while ((line = input.readLine()) != null)
//            {
//                Log.d(TAG, "onCreate: " + line);
//            }
//
//            int exitVal = process.waitFor();
//            System.out.println("Exited with error code " + exitVal);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        Intent intent1 = new Intent(this, ProcessService.class);
        startService(intent1);

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.packages);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String eachline = bufferedReader.readLine();
            while (eachline != null) {
                // `the words in the file are separated by space`, so to get each words
                String[] words = eachline.split(" ");
                eachline = bufferedReader.readLine();
                for (int i = 0; i < words.length; i++) {
                    Log.d(TAG, "onCreate:" + words[i]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void dialogPermission(final Context context) {
        mDialogPermission = new Dialog(this);
        mDialogPermission.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialogPermission.setContentView(R.layout.dialog_check_usage_permission);
        Button allowButton = mDialogPermission.findViewById(R.id.button_allow);
        mDialogPermission.setCanceledOnTouchOutside(false);
        allowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                context.startActivity(intent);
            }
        });
        mDialogPermission.show();
        mDialogPermission.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return (keyCode == KeyEvent.KEYCODE_BACK && !event.isCanceled());
            }
        });
        Window window = mDialogPermission.getWindow();
//        if (window != null) {
//            window.setCallback(new UserInteractionAwareCallback(window.getCallback(), context));
//        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mDialogPermission.dismiss();
        Log.d(TAG, "onRestart: ");
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        if (stats == null || stats.isEmpty()) {
            dialogPermission(this);
        } else {
            Toast.makeText(this, "Permission is Granted", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    public class UsagePermissionMonitor {
        private AppOpsManager mAppOpsManager;
        private final Context context;
        private final Handler handler;
        private boolean isListening;
        private Boolean lastValue;

        public UsagePermissionMonitor(Context context) {
            this.context = context;
            mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            handler = new Handler();
        }

        public void startListening() {
            mAppOpsManager.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS, context.getPackageName(), usageOpListener);
            isListening = true;
        }

        public void stopListening() {
            lastValue = null;
            isListening = false;
            mAppOpsManager.stopWatchingMode(usageOpListener);
            handler.removeCallbacks(checkUsagePermission);
        }

        private final AppOpsManager.OnOpChangedListener usageOpListener = new AppOpsManager.OnOpChangedListener() {
            @Override
            public void onOpChanged(String op, String packageName) {
                // Android sometimes sets packageName to null
                if (packageName == null || context.getPackageName().equals(packageName)) {
                    // Android actually notifies us of changes to ops other than the one we registered for, so filtering them out
                    if (AppOpsManager.OPSTR_GET_USAGE_STATS.equals(op)) {
                        // We're not in main thread, so post to main thread queue
                        handler.post(checkUsagePermission);
                    }
                }
            }
        };

        private final Runnable checkUsagePermission = new Runnable() {
            @Override
            public void run() {
                if (isListening) {
                    int mode = mAppOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myPid(), context.getPackageName());
                    boolean enabled = mode == AppOpsManager.MODE_ALLOWED;

                    // Each change to the permission results in two callbacks instead of one.
                    // Filtering out the duplicates.
                    if (lastValue == null || lastValue != enabled) {
                        lastValue = enabled;

                        // TODO: Do something with the result
                        Log.i(UsagePermissionMonitor.class.getSimpleName(), "Usage permission changed: " + enabled);
                    }
                }
            }
        };
    }
}
