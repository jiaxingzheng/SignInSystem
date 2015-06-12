package com.smartworld.signinsystem;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends Activity {
	public static final String TARGET_ACTIVITY = "target_activity";
	public static final int WAIT_ACTIVITY = 0;
	public static final int SIGNIN_ACTIVITY = 1;
	public static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
    }
    
    public void onClick(View view){
    	Intent intent = new Intent(this,AlertDialog.class);
		intent.putExtra("editTextShow", true);
		intent.putExtra("titleIsCancel", true);
		intent.putExtra("msg", getResources().getString(R.string.prompt));
		
    	switch(view.getId()){
    	case R.id.open:
    		intent.putExtra(TARGET_ACTIVITY, WAIT_ACTIVITY);
    		break;
    	case R.id.sign_in:
    		intent.putExtra(TARGET_ACTIVITY, SIGNIN_ACTIVITY);
    		break;
    	}
    	startActivity(intent);
    	
    }


}
