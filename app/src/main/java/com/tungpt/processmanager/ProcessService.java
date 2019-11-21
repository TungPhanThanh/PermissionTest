package com.tungpt.processmanager;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;

import com.ibct.kanganedu.Grp1Grpc;
import com.ibct.kanganedu.StdAsk;
import com.ibct.kanganedu.StdRet;
import com.tungpt.processmanager.model.ListProcess;
import com.tungpt.processmanager.model.ListUrl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static com.tungpt.processmanager.MainActivity.TAG;

public class ProcessService extends Service {
    private static String CHANNEL_ID = "Notification";
    private ArrayList<String> listProcess = new ArrayList<>();
    private ArrayList<String> listUrls = new ArrayList<>();
    private ManagedChannel channel;
    private ConstraintLayout mLayout;
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
        try {
            getJson();
            processChecked();
            String input = intent.getStringExtra("inputExtra");
            createNotificationChannel();
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setOngoing(true)
                    .setPriority(2)
                    .setContentTitle("KangAn Education")
                    .setContentText(input)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
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
                            Intent intent = new Intent(this, BlockProcessActivity.class);
                            intent.putExtra("unallowed",currentApp);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
//                            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
//                            mLayout = new ConstraintLayout(this);
//                            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
//                            lp.format = PixelFormat.TRANSLUCENT;
//                            lp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
//                            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
//                            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                            lp.gravity = Gravity.TOP;
//                            LayoutInflater inflater = LayoutInflater.from(this);
//                            inflater.inflate(R.layout.restrict_screen, mLayout);
//                            wm.addView(mLayout, lp);
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
                listProcess.clear();
                listUrls.clear();
                try {
                    channel = ManagedChannelBuilder.forAddress("13.124.244.187", 8081).usePlaintext().build();
                    Grp1Grpc.Grp1BlockingStub stub = Grp1Grpc.newBlockingStub(channel);
                    StdAsk request = StdAsk.newBuilder().setAskName("setup-pull").build();
                    StdRet reply = stub.stdRpc(request);
                    JSONObject jsonObject = new JSONObject(reply.getRetStr());
                    JSONArray allowedProcesses = jsonObject.getJSONArray("AllowedProcesses");
                    for (int i = 0; i < allowedProcesses.length(); i ++){
                        JSONObject jsonObject1 =  allowedProcesses.getJSONObject(i);
                        listProcess.add(jsonObject1.getString("Process"));
                    }
                    ListProcess.setListProcess(listProcess);
                    JSONArray allowedUrls = jsonObject.getJSONArray("AllowedUrls");
                    for (int i = 0; i < allowedUrls.length(); i ++){
                        JSONObject object = allowedUrls.getJSONObject(i);
                        listUrls.add(object.getString("Url"));
                    }
                    ListUrl.setListUrl(listUrls);
                    Log.d("aaaaaa", "onClick: " + ListProcess.getsListProcess() + "/" + ListUrl.getsListUrl());
                    channel.shutdown();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "KA Education",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
