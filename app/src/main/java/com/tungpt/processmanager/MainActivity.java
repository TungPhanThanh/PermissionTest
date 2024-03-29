package com.tungpt.processmanager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.ibct.kanganedu.Grp1Grpc;
import com.ibct.kanganedu.StdAsk;
import com.ibct.kanganedu.StdRet;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "MYPROCESS";
    private EditText editTextUser;
    private EditText editTextPassword;
    private Button mButtonLogin;
    private Button mButtonChange;
    private Switch aSwitchRegister;
    private Switch aSwitchChange;
    private Dialog mDialogPermission;
    private String deviceId;
    private String UserID;
    private Boolean checkRegister = false;
    private Boolean checkChange = false;
    private ManagedChannel channel;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        initView();
        Intent intent1 = new Intent(this, ProcessService.class);
        startService(intent1);
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

        if (stats == null || stats.isEmpty()) {
            dialogPermission(this);
        } else {
            Toast.makeText(this, "Permission is Granted", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("HardwareIds")
    private void initView() {
        editTextUser = findViewById(R.id.edit_text_user_id);
        editTextPassword = findViewById(R.id.edit_text_user_password);
        aSwitchRegister = findViewById(R.id.switch_register);
        aSwitchChange = findViewById(R.id.switch_change);
        mButtonLogin = findViewById(R.id.button_login);
        mButtonChange = findViewById(R.id.button_change);
        mButtonLogin.setOnClickListener(this);
        mButtonChange.setOnClickListener(this);
        aSwitchRegister.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkRegister = isChecked;
            }
        });

        aSwitchChange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkChange = isChecked;
                checkButton();
            }
        });
        deviceId = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private void checkButton() {
        if (checkChange) {
            aSwitchRegister.setEnabled(false);
            mButtonLogin.setVisibility(View.GONE);
            mButtonChange.setVisibility(View.VISIBLE);
        } else {
            aSwitchRegister.setEnabled(true);
            mButtonLogin.setVisibility(View.VISIBLE);
            mButtonChange.setVisibility(View.GONE);
        }
    }

    private void handleLogin() {
        String statusCode = "";
        Log.d(TAG, "handleLogin: " + editTextUser.getText() + "/" + editTextPassword.getText());
        if (editTextUser.getText().length() == 0 || editTextPassword.getText().length() == 0) {
            Toast.makeText(this, "Input User ID or Password", Toast.LENGTH_SHORT).show();
        } else {
            try {
                channel = ManagedChannelBuilder.forAddress("13.124.244.187", 8081).usePlaintext().build();
                Grp1Grpc.Grp1BlockingStub stub = Grp1Grpc.newBlockingStub(channel);
                StdAsk request = StdAsk.newBuilder().setAskName("app-login").setAskStr("{\n" +
                        "    \"UserId\": \"" + editTextUser.getText() + "\",\n" +
                        "    \"Password\": \"" + editTextPassword.getText() + "\",\n" +
                        "    \"MacAddr\": \"" + deviceId + "\",\n" +
                        "    \"RDeviceYes\": true,\n" +
                        "    \"CDeviceYes\": true\n" +
                        "}").build();
                StdRet reply = stub.stdRpc(request);
                JSONObject jsonObject = new JSONObject(reply.getRetStr());
                UserID = jsonObject.getString("DeviceId");
                Log.d("aaaa", request.getAskStr() + "/" + UserID + "/" + reply.getRetSta());
                statusCode = reply.getRetSta();
                channel.shutdown();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
            }
            if (statusCode.equals("200")) {
                SharedPreferences sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("UserId", UserID);
                editor.putString("UserName", editTextUser.getText().toString());
                editor.putString("Password", editTextPassword.getText().toString());
                editor.apply();
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "User ID or Password is incorrect!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_login:
                handleLogin();
                break;
            case R.id.button_change:
                handleLogin();
                break;
            default:
                break;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialogPermission != null) {
            mDialogPermission.dismiss();
        }
    }
}
