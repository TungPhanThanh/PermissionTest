package com.tungpt.processmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tungpt.processmanager.model.IsBlockChecking;

public class BlockUrlActivity extends AppCompatActivity {

    private Button mButtonExit;
    private String url;
    private EditText editTextUrl;
    private BlockProcessActivity blockProcessActivity = new BlockProcessActivity();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_url);
        IsBlockChecking.setIsChecking(false);
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        Log.d("aaaa", "onCreate: " + url);
        editTextUrl = findViewById(R.id.edit_text_restricted_web);
        editTextUrl.setText(url);
        mButtonExit = findViewById(R.id.button_exit_web);
        mButtonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                blockProcessActivity.sendLog("", url);
                finish();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}
