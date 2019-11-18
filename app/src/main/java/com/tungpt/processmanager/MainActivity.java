package com.tungpt.processmanager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.ibct.kanganedu.Grp1Grpc;
import com.ibct.kanganedu.StdAsk;
import com.ibct.kanganedu.StdRet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "aaa";
    private EditText editTextUser;
    private EditText editTextPassword;
    private Button mButton;
    private Dialog mDialogPermission;
    private String deviceId;
    private ManagedChannel channel;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        channel = ManagedChannelBuilder.forAddress("13.124.244.187", 8081).usePlaintext().build();
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

        if (stats == null || stats.isEmpty()) {
            dialogPermission(this);
        } else {
            Toast.makeText(this, "Permission is Granted", Toast.LENGTH_LONG).show();
        }

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

        initView();

    }

    @SuppressLint("HardwareIds")
    private void initView() {
        editTextUser = findViewById(R.id.edit_text_user_id);
        editTextPassword = findViewById(R.id.edit_text_user_password);
        mButton = findViewById(R.id.button_login);
        mButton.setOnClickListener(this);
        deviceId = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private void handleLogin() {
        String statusCode = "";
        Log.d(TAG, "handleLogin: " + editTextUser.getText() + "/" + editTextPassword.getText());
        if (editTextUser.getText().equals("") || editTextPassword.getText().equals("")) {
            Toast.makeText(this, "Input User ID or Password", Toast.LENGTH_SHORT).show();
        } else {
            try {
                Grp1Grpc.Grp1BlockingStub stub = Grp1Grpc.newBlockingStub(channel);
                StdAsk request = StdAsk.newBuilder().setAskName("app-login").setAskStr("{\n" +
                        "    \"UserId\": \"" + editTextUser.getText() + "\",\n" +
                        "    \"Password\": \"" + editTextPassword.getText() + "\",\n" +
                        "    \"MacAddr\": \"" + deviceId + "\",\n" +
                        "    \"RDeviceYes\": true,\n" +
                        "    \"CDeviceYes\": true\n" +
                        "}").build();
                StdRet reply = stub.stdRpc(request);
                Log.d("aaaaaa", "onClick: " + request.getAskStr() + "/" + reply.getRetStr() + "/" + reply.getRetSta());
                statusCode = reply.getRetSta();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
            }
            if (statusCode.equals("200")) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "User ID or Password is incorrect!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_login) {
            handleLogin();
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
        if (mDialogPermission != null) {
            mDialogPermission.dismiss();
        }
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
}
