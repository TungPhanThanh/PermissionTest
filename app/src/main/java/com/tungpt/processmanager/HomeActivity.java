package com.tungpt.processmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Intent intent1 = new Intent(this, ProcessService.class);
        startService(intent1);
    }

    public void runAsRoot() {
        try {
            String[] array = new String[]{"cat /storage/emulated/0/hosts ", "cat /system/etc/hosts ", "cat /storage/emulated/0/hosts >> /system/etc/hosts ",};
            Process process;
            // Executes the command.
            process = Runtime.getRuntime().exec("mount -o remount,rw /system");
//            OutputStream os = process.getOutputStream();
//            os.write("cat /storage/emulated/0/hosts \n".getBytes());
//            os.write("cat /storage/emulated/0/hosts >> /system/etc/hosts \n".getBytes());
//            os.write("cat /system/etc/hosts\n".getBytes());
//            os.flush();
            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();
            Log.d("aaaa", "runAsRoot: " + Environment.getExternalStorageDirectory() + "/" + output.toString());
//            process1 = Runtime.getRuntime().exec("echo \"The next line\" >> /system/etc/hosts");
//
//            BufferedReader reader1 = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()));
//
//            int read1;
//            char[] buffer1 = new char[4096];
//            StringBuffer output1 = new StringBuffer();
//            while ((read1 = reader1.read(buffer1)) > 0) {
//                output1.append(buffer1, 0, read1);
//            }
//            reader1.close();
//
//            // Waits for the command to finish.
//            process1.waitFor();
//            Log.d("aaaa", "runAsRoot: " + output1.toString());
            output.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
