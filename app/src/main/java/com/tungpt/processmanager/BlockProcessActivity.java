package com.tungpt.processmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.ibct.kanganedu.Grp1Grpc;
import com.ibct.kanganedu.StdAsk;
import com.ibct.kanganedu.StdRet;
import com.tungpt.processmanager.model.IsBlockChecking;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class BlockProcessActivity extends AppCompatActivity {

    private Button mButtonExit;
    private String process;
    private String systemId;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restrict_screen);
        Intent intent = getIntent();
        process = intent.getStringExtra("unallowed");
        Log.d("aaaa", "onCreate: " + process);
        editText = findViewById(R.id.edit_text_restricted_process);
        editText.setText(process);
        mButtonExit = findViewById(R.id.button_exit);
        mButtonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLog(process, "");
                IsBlockChecking.setIsCheckingProcess(false);
                finish();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sendLog(process, "");
        IsBlockChecking.setIsCheckingProcess(false);
        finish();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendLog(process, "");
        IsBlockChecking.setIsCheckingProcess(false);
        finish();
    }

    public void sendLog(String process, String url) {
        SharedPreferences sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("UserId", "");
        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("13.124.244.187", 8081).usePlaintext().build();
            Grp1Grpc.Grp1BlockingStub stub = Grp1Grpc.newBlockingStub(channel);
            @SuppressLint("HardwareIds") StdAsk request = StdAsk.newBuilder().setAskName("device-log-add").setAskStr("{\n" +
                    "\t\"UdId\": " + userId + ",\n" +
                    "\t\"LogType\": \"illegal access\",\n" +
                    "\t\"JsonLogs\": [\n" +
                    "\t\t{ \"Process\": \" " + process + " \", \"Url\": \"" + "" + "\", \"Ip\": \"" + "" + "\" },\n" +
                    "\t\t{ \"Process\": \"" + url + "\", \"Url\": \"" + "" + "\", \"Ip\": \"" + "" + "\" }\n" +
                    "\t],\n" +
                    "\t\"StrLog\": \"mpgewrqio\"\n" +
                    "}").build();
            StdRet reply = stub.stdRpc(request);
            Log.d("aaaaaa", "onClick: " + request.getAskStr() + "/" + reply.getRetStr() + "/" + reply.getRetSta());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
