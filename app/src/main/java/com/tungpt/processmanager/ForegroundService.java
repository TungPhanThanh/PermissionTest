package com.tungpt.processmanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.util.Patterns;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tungpt.processmanager.model.IsBlockChecking;
import com.tungpt.processmanager.model.ListUrl;

import java.util.ArrayList;

public class ForegroundService extends AccessibilityService {
    private ArrayList<String> listUrl = new ArrayList<>();
    private final AccessibilityServiceInfo info = new AccessibilityServiceInfo();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType()).contains("WINDOW") &&
                accessibilityEvent.getPackageName() != null) {
            AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
            dfs(nodeInfo);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listUrl.add("google.com");
        listUrl.add("m.facebook.com");
        listUrl.add("Search or type web address");
        Log.d("aaaaa", "onCreate: Start Service");
        Log.d("aaaaa", "onCreate: " + ListUrl.getsListUrl());
    }

    @Override
    public void onInterrupt() {

    }

    public void dfs(AccessibilityNodeInfo info) {
        if (info == null)
            return;
        if (info.getText() != null && info.getText().length() > 0)
            if (info.getClassName().toString().equals("android.widget.EditText")) {
                Log.d("MYCHROME", info.getText() + "");
                for (int i = 0; i < listUrl.size(); i++) {
                    if (!info.getText().toString().equals(listUrl.get(i)) &&
                            Patterns.WEB_URL.matcher(info.getText().toString()).matches() && !IsBlockChecking.getChecking()) {
                        Log.d("MYCHROME", info.getText() + "");
                        IsBlockChecking.setIsChecking(true);
                        Intent intent = new Intent(this, BlockUrlActivity.class);
                        intent.putExtra("url", info.getText().toString());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
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
}
