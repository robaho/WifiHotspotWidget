package com.robaho.wifihotspotwidget;

import java.io.IOException;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import static com.robaho.wifihotspotwidget.MyConstants.*;


public class MyService extends IntentService {
	public MyService() {
		super(MyService.class.getName());
	}
	
	private Handler handler;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler = new Handler();  
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		boolean autostart = prefs.getBoolean("autostart",false);
		if(autostart) {
			try {
				hotspotOn();
				sendNotification();
			} catch (Exception e) {
				e.printStackTrace();
				makeToast("unable to auto-start hotspot");
			}
		}
		return super.onStartCommand(intent, flags, startId);  
	}
	
	private void makeToast(final String msg) {
		handler.post(new Runnable(){
			public void run() {
		        Toast.makeText(MyService.this.getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
			}});
	}
	
	private void toggleHotspot(Intent intent) {
        boolean hotspotOn = isHotspotOn() ? false : true;
        
		Log.i("MyService","hotspotOn becoming "+hotspotOn);
		
		makeToast("Turning Hotspot "+(hotspotOn ? "On" : "Off"));
		
		
		try {
			if(hotspotOn) {
				hotspotOn();
				sendNotification();
			} else{
				hotspotOff();
				clearNotification();
			}
		} catch(Exception e){
			Log.e("MyService","unable to toggle hotspot : "+e);
			e.printStackTrace();
			makeToast("unable to toggle hotspot");
		}
		
		sendStatus();
	}
	
	private void sendNotification() {
		Notification.Builder mBuilder =
		        new Notification.Builder(this)
		        .setSmallIcon(R.drawable.hotspoton_small)
		        .setContentTitle("WifiHotspotWidget")
		        .setContentText("Running...");
		
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(1, mBuilder.build());
	}
	
	private void sendFailedNotification(String msg) {
		Notification.Builder mBuilder =
		        new Notification.Builder(this)
		        .setSmallIcon(R.drawable.failed)
		        .setContentTitle("WifiHotspotWidget")
		        .setContentText(msg);
		
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(1, mBuilder.build());
	}
	
	private void clearNotification() {
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
		mNotificationManager.cancel(1);
	}
	
	private void executeCmd(String cmd) throws Exception {
		CommandCapture command = new CommandCapture(0,cmd);
		Shell.runRootCommand(command);
		while (!command.isFinished()) {
		    Thread.sleep(1);
		}
		Log.d("MyService","command '"+cmd+"', exit code "+command.getExitCode()+", output "+command.toString());
		if(command.getExitCode()!=0) {
			throw new IllegalStateException("command '"+cmd+" failed, exit "+command.getExitCode()+", output "+cmd.toString());
		}
	}
	
	private void hotspotOn() throws Exception {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		String ssid = prefs.getString("ssid","AndroidAP");
		String password = prefs.getString("password","password");
		String security = prefs.getString("security","wpa-psk");
		String wlandev = prefs.getString("wlandev","wlan0");
		String radiodev = prefs.getString("radiodev","rmnet0");
		String dns1 = prefs.getString("dns1","8.8.8.8");
		String dns2 = prefs.getString("dns2","8.8.4.4");
		
		executeCmd("svc wifi disable");
		executeCmd("ndc softap fwreload "+wlandev+" AP");
		executeCmd("ndc softap set wlan0 unused \""+ssid+"\" "+security+" \""+password+"\"");
		executeCmd("echo \"ignore_broadcast_ssid=0\" >> /data/misc/wifi/hostapd.conf");
		executeCmd("echo \"max_num_sta=9\" >> /data/misc/wifi/hostapd.conf");
		executeCmd("ndc ipfwd enable");
		executeCmd("ndc tether interface add "+wlandev);
		executeCmd("ndc tether start 192.168.1.200 192.168.1.209");
		executeCmd("ndc nat enable "+wlandev+" "+radiodev+" 0");
		executeCmd("ndc interface setcfg "+wlandev+" 192.168.1.199 24 down");
		executeCmd("ndc softap start "+wlandev);
		executeCmd("ndc softap startap");
		executeCmd("ndc tether dns set "+dns1+" "+dns2);
	}
	
	private void hotspotOff() throws Exception {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		String wlandev = prefs.getString("wlandev","wlan0");
		String radiodev = prefs.getString("radiodev","rmnet0");
		
		executeCmd("ndc softap stopap");
		executeCmd("ndc softap stop "+wlandev);
		executeCmd("ndc softap fwreload STA");
		executeCmd("ndc nat disable "+wlandev+" "+radiodev+" 0");
		executeCmd("ndc tether stop");
		executeCmd("ndc tether interface remove "+wlandev);
		executeCmd("ndc ipfwd disable");
		executeCmd("svc wifi enable");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		RootTools.handlerEnabled=false;
		
		sendChanging();
		
		if(!RootTools.isAccessGiven()){
			Log.w("MyService","root access not granted");
			makeToast("WifiHotspotWidget requires root");
			sendFailedNotification("Requires root...");
			sendStatus();
			return;
		}
		
		if(!RootTools.findBinary("ndc")){
			Log.w("MyService","rom not supported, ndc required");
			makeToast("WifiHotspotWidget requires ndc");
			sendFailedNotification("Requires ndc...");
			sendStatus();
			return;
		}
		
		if(!RootTools.findBinary("hostapd")){
			Log.w("MyService","rom not supported, hostapd required");
			makeToast("WifiHotspotWidget requires hostapd");
			sendFailedNotification("Requires hostapd...");
			sendStatus();
			return;
		}
		
		if(intent.getAction().equals(TOGGLE_HOTSPOT)) {
			sendChanging();
			toggleHotspot(intent);
		} else if(intent.getAction().equals(HOTSPOT_STATUS)){
			sendStatus();
		}
		
        try {
			Shell.closeRootShell();
		} catch (IOException e) {
			Log.e("MyService","unable to close root shell : "+e);
			e.printStackTrace();
		}
        
		Log.d("MyService","handled intent "+intent);
	}
	
	private void sendStatus() {
        Intent i = new Intent(this.getApplicationContext(),MyWidget.class);
        i.setAction(isHotspotOn() ? HOTSPOT_ON : HOTSPOT_OFF);
        this.sendBroadcast(i);
        Log.i("MyService","send intent "+i);
        
        i = new Intent();
        i.setAction(isHotspotOn() ? HOTSPOT_ON : HOTSPOT_OFF);
        this.sendBroadcast(i);
        Log.i("MyService","send intent "+i);
	}
	
	private void sendChanging() {
        Intent i = new Intent(this.getApplicationContext(),MyWidget.class);
        i.setAction(HOTSPOT_CHANGING);
        this.sendBroadcast(i);
        Log.i("MyService","send intent "+i);
        
        i = new Intent();
        i.setAction(HOTSPOT_CHANGING);
        this.sendBroadcast(i);
        Log.i("MyService","send intent "+i);
	}
	
	private boolean isHotspotOn() {
		CommandCapture command = new CommandCapture(0, "ndc softap status");
		try {
			RootTools.getShell(true);
			Shell.runRootCommand(command);
			while (!command.isFinished()) {
			    Thread.sleep(1);
			}
			boolean on = command.toString().contains("started");
			Log.d("MyService","hotspotOn is "+on+", output "+command);
			return on;
		} catch (Exception e) {
			Log.e("MyService","unable to run root");
			e.printStackTrace();
			return false;
		}
	}
}
