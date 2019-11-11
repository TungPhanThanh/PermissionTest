package com.tungpt.processmanager;

import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.tungpt.processmanager.MainActivity.TAG;

public class ProcessService extends Service {
    Handler mHandler = new Handler();
    String[] application;

    public ProcessService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getJson();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                retriveNewApp();
                mHandler.postDelayed(this, 2000);
            }
        }, 2000);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void retriveNewApp() {
        int pid = 0;
        if (Build.VERSION.SDK_INT >= 22) {
            String currentApp = null;
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            assert usm != null;
            List<UsageStats> applist = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (applist != null && applist.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : applist) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                    pid = android.os.Process.myPid();
                    for (String s : application) {
                        if (currentApp.equals(s)) {
                            Intent intent = new Intent(this, BlockActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {

                        }
                    }
                }
            }
            Log.e(TAG, "Current App in foreground is: " + currentApp + "/" + pid);
        } else {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            String mm = (manager.getRunningTasks(1).get(0)).topActivity.getPackageName();
            Log.e(TAG, "Current App in foreground is: " + mm);
        }
    }

    private void getJson() {
        try {
            String readJson = readJson(this);
            JSONObject jsonObject = new JSONObject(readJson);
            JSONArray jSONArray = (JSONArray) jsonObject.get("application");
            application = new String[jSONArray.length()];
            for (int i = 0; i < jSONArray.length(); i++) {
                application[i] = jSONArray.getString(i);
                Log.d(TAG, "onCreate: " + application[i]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readJson(Context context) throws IOException {
        InputStream is = context.getResources().openRawResource(R.raw.blocking_ka_edu);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String s = null;
        while ((s = br.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        return sb.toString();
    }
}
