package com.robaho.wifihotspotwidget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import static com.robaho.wifihotspotwidget.MyConstants.*;

public class MyActivity extends Activity {
	MyReceiver receiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appactivity);
	
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		receiver = new MyReceiver();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(HOTSPOT_CHANGING);
		filter.addAction(HOTSPOT_ON);
		filter.addAction(HOTSPOT_OFF);
		
        registerReceiver(receiver, filter);
		
		Log.i("MyActivity","intent is "+getIntent());
		// the following will force an update
        startService(new Intent(HOTSPOT_STATUS,null,this,MyService.class));
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}
	
	public void toggleHotspot(View v) {
        startService(new Intent(TOGGLE_HOTSPOT,null,this,MyService.class));
	}
	
	public void configureSettings(View v) {
		Intent i= new Intent(getBaseContext(), MyPreferences.class);
		Log.i("MyActivity","sending intent "+i);
        startActivity(i);
	}

	private class MyReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			Log.i("MyActivity","received intent "+intent+", context "+context);
			if(intent.getAction().equals(HOTSPOT_ON)) {
				hotspot_enabled(context,true);
			} else if(intent.getAction().equals(HOTSPOT_OFF)){
				hotspot_enabled(context,false);
			} else if(intent.getAction().equals(HOTSPOT_CHANGING)){
				hotspot_changing(context);
			}
		}
		
		private void hotspot_enabled(Context context,boolean enabled){
	        ImageButton toggleButton = (ImageButton) findViewById(R.id.toggleButton);
	        toggleButton.setImageResource(enabled ? R.drawable.hotspoton : R.drawable.hotspotoff);
	        TextView textStatus = (TextView)findViewById(R.id.textStatus);
	        textStatus.setText(enabled ? R.string.wifi_hotspot_on : R.string.wifi_hotspot_off);
		}
		
		private void hotspot_changing(Context context){
	        ImageButton toggleButton = (ImageButton) findViewById(R.id.toggleButton);
	        toggleButton.setImageResource(R.drawable.changing);
	        
	     // Get the background, which has been compiled to an AnimationDrawable object.
	        AnimationDrawable frameAnimation = (AnimationDrawable) toggleButton.getDrawable();
	        // Start the animation (looped playback by default).
	        frameAnimation.start();
		}
	}
	
	
	
}
