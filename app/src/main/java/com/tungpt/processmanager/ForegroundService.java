package com.tungpt.processmanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ibct.kanganedu.Grp1Grpc;
import com.ibct.kanganedu.StdAsk;
import com.ibct.kanganedu.StdRet;
import com.tungpt.processmanager.model.IsBlockChecking;
import com.tungpt.processmanager.model.ListUrl;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ForegroundService extends AccessibilityService {
    private ArrayList<String> listUrl = new ArrayList<>();
    private final AccessibilityServiceInfo info = new AccessibilityServiceInfo();
    private ConstraintLayout mLayout;
    private WindowManager wm;
    private String url;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType()).contains("WINDOW") &&
                accessibilityEvent.getPackageName() != null ) {
            AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
            dfs(nodeInfo);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listUrl.add("google.com");
        listUrl.add("m.facebook.com");

        Log.d("aaaaa", "onCreate: Start Service");
    }

    @Override
    public void onInterrupt() {

    }

    public void dfs(AccessibilityNodeInfo info) {
        if (info == null)
            return;
        if (info.getText() != null && info.getText().length() > 0)
            if (info.getClassName().toString().equals("android.widget.EditText") && !ListUrl.getsListUrl().isEmpty() ) {
                Log.d("MYCHROME", info.getText() + "");
                for (int i = 0; i < ListUrl.getsListUrl().size(); i++) {
                    if (Patterns.WEB_URL.matcher(info.getText().toString()).matches() && !info.getText().toString().equals("google.com") &&
                            !(info.getText().toString().equals(ListUrl.getsListUrl().get(i))) &&
                            !IsBlockChecking.getChecking()) {
                        Log.d("MYCHROME in check", info.getText() + "/ " + !info.getText().toString().equals(ListUrl.getsListUrl().get(i)));
                        Log.d("MYCHROME in check", Patterns.WEB_URL.matcher(info.getText().toString()).matches() + "");
                        Log.d("MYCHROME in check", "dfs: " + IsBlockChecking.getChecking());
                        IsBlockChecking.setIsChecking(true);
                        url = info.getText().toString();
                        if (!url.substring(0,4).equals("com.") && isReached(url)){
                            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                            mLayout = new ConstraintLayout(this);
                            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                            lp.format = PixelFormat.TRANSLUCENT;
                            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                            lp.gravity = Gravity.TOP;
                            LayoutInflater inflater = LayoutInflater.from(this);
                            inflater.inflate(R.layout.activity_block_url, mLayout);
                            wm.addView(mLayout, lp);
                            configureExitButton();
                        }
//                        Intent intent = new Intent(this, BlockUrlActivity.class);
//                        intent.putExtra("url", info.getText().toString());
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
                    }
                }
            }
        for (int i = 0; i < info.getChildCount(); i++) {
            AccessibilityNodeInfo child = info.getChild(i);
            dfs(child);
            if (child != null) {
                child.recycle();
            }
        }
    }

    @Override
    public void onServiceConnected() {
        // Set the type of events that this service wants to listen to.
        //Others won't be passed to this service.
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;

        this.setServiceInfo(info);
    }

    private void configureExitButton() {
        Button exitButton = mLayout.findViewById(R.id.button_exit_web);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Onclick", "onClick: ");
                IsBlockChecking.setIsChecking(false);
                sendLog("", url);
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                wm.removeViewImmediate(mLayout);
            }
        });
    }

    private void sendLog(String process, String url) {
        String userId = "";
        SharedPreferences sharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE);
        if (sharedPreferences != null) {
            userId = sharedPreferences.getString("UserId", "");
        }
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
            channel.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection.Response response;

    private boolean isReached(String url) {
        try {
            response = Jsoup.connect("https://" + url)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
                    .execute();
            Log.d("MYCHROME", "isReached: " + response.statusCode());
            if (response.statusCode() == 200) {
                return true;
            }
        } catch (Exception e) {
            Log.d("aaa", e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}
