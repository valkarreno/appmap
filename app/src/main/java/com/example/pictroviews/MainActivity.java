package com.example.pictroviews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button startButton;
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton=findViewById(R.id.startButton);
        exitButton=findViewById(R.id.exitButton);

    }
    public void closeApplication(View view) {

        finishAffinity();
    }
    public void openSecondActivity(View view) {
        Intent intent = new Intent(this, MenuApp.class);
        startActivity(intent);
    }

}