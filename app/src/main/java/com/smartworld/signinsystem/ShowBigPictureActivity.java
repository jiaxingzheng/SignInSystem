package com.smartworld.signinsystem;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class ShowBigPictureActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_big_picture);
        String name = getIntent().getStringExtra("NAME");
        File file = new File("/sdcard/SignInSystem/", name+"_big_picture.jpg");
        FileInputStream fileStream = null;
        try {
            fileStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(fileStream);

        ((ImageView)findViewById(R.id.picture)).setImageBitmap(bitmap);
    }


}
