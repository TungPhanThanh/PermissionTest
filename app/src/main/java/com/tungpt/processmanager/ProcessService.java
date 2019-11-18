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

import com.ibct.kanganedu.Grp1Grpc;
import com.ibct.kanganedu.StdAsk;
import com.ibct.kanganedu.StdRet;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static com.tungpt.processmanager.MainActivity.TAG;

public class ProcessService extends Service {
    private ManagedChannel channel;
    Handler mHandler = new Handler();
    String[] application;

    public ProcessService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getJson();
        processChecked();
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
        } catch (JSONException | IOException e) {
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

    private void processChecked() {
        Runnable processRunnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void run() {
                retriveNewApp();
                mHandler.postDelayed(this, 3000);
            }
        };

        Runnable pullRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    channel = ManagedChannelBuilder.forAddress("13.124.244.187", 8081).usePlaintext().build();
                    Grp1Grpc.Grp1BlockingStub stub = Grp1Grpc.newBlockingStub(channel);
                    StdAsk request = StdAsk.newBuilder().setAskName("setup-pull").build();
                    StdRet reply = stub.stdRpc(request);
                    Log.d("aaaaaa", "onClick: " + request.getAskStr() + "/" + reply.getRetStr() + "/" + reply.getRetSta());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mHandler.postDelayed(this, 3 * 60 * 1000);
            }
        };

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        executor.execute(processRunnable);
        executor.execute(pullRunnable);
    }
}
