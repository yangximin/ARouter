package com.muyi.module;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.muyi.arouter.annotation.ARouter;

@ARouter(path = "/app/MainActivity")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
