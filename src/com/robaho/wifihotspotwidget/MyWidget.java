package com.robaho.wifihotspotwidget;

import android.app.PendingIntent;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import static com.robaho.wifihotspotwidget.MyConstants.*;

public class MyWidget extends AppWidgetProvider {
	
	public MyWidget() {		
		Log.i("MyWidget","created");
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,int[] appWidgetIds) {
		// the following will force an update
        context.startService(new Intent(HOTSPOT_STATUS,null,context,MyService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("MyWidget","received intent "+intent+", context "+context);
		if(intent.getAction().equals(HOTSPOT_ON)) {
			hotspot_enabled(context,true);
		} else if(intent.getAction().equals(HOTSPOT_OFF)){
			hotspot_enabled(context,false);
		} else if(intent.getAction().equals(HOTSPOT_CHANGING)){
			hotspot_changing(context);
		} else
			super.onReceive(context, intent);
	}
	
	private void hotspot_enabled(Context context,boolean enabled){
        Log.i("MyWidget","changing status to hotspot enabled "+enabled);
        
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		
		// must build complete view on a widget on each update since the RemoteViews is
		// cached and used to rebuild the view after a rotation, etc.
		
        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.appwidget);
        Intent intent = new Intent(TOGGLE_HOTSPOT,null,context,MyService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.imageview, pendingIntent);
        views.setImageViewResource(R.id.imageview, enabled ? R.drawable.hotspoton : R.drawable.hotspotoff);
        views.setViewVisibility(R.id.imageview, View.VISIBLE);
        views.setViewVisibility(R.id.imageAnim, View.INVISIBLE);
        awm.updateAppWidget(new ComponentName(context,MyWidget.class), views);
	}
	
	private void hotspot_changing(Context context){
        Log.i("MyWidget","changing status...");
        
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		
		// must build complete view on a widget on each update since the RemoteViews is
		// cached and used to rebuild the view after a rotation, etc.
		
        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.appwidget);
        views.setViewVisibility(R.id.imageview, View.INVISIBLE);
        views.setViewVisibility(R.id.imageAnim, View.VISIBLE);
        awm.updateAppWidget(new ComponentName(context,MyWidget.class), views);
	}
}
