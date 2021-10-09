package com.robaho.wifihotspotwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import static com.robaho.wifihotspotwidget.MyConstants.*;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class MyPreferences extends PreferenceActivity {
	private int mAppWidgetId;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupSimplePreferencesScreen();
		
		final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        
		Log.i("MyPreferences","received intent "+intent);
        
        if(intent.getAction()==null)
        	return;
        
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		// Add 'general' preferences.
		setContentView(R.layout.preferences);
		addPreferencesFromResource(R.xml.preferences);
		
		setupDefaults(findPreference("ssid").getContext());

		bindPreferenceSummaryToValue(findPreference("ssid"));
		bindPreferenceSummaryToValue(findPreference("password"));
		bindPreferenceSummaryToValue(findPreference("security"));
		bindPreferenceSummaryToValue(findPreference("wlandev"));
		bindPreferenceSummaryToValue(findPreference("radiodev"));
		bindPreferenceSummaryToValue(findPreference("dns1"));
		bindPreferenceSummaryToValue(findPreference("dns2"));
		
	    Button saveButton = (Button) findViewById(R.id.save);
	    saveButton.setOnClickListener(new View.OnClickListener() {
	        @Override
			public void onClick(View view) {
	        	final Intent resultValue = new Intent();
	            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK,resultValue);
                finish();
	        }
	    });
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return false;
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);
			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

	private static void setupDefaults(Context context){
		String serviceName = Context.TELEPHONY_SERVICE;
		TelephonyManager m_telephonyManager = (TelephonyManager) context.getSystemService(serviceName);
		String IMEI = m_telephonyManager.getDeviceId();
		
		String phoneNumber = m_telephonyManager.getLine1Number();
		phoneNumber = phoneNumber.replaceAll("/[^0-9 ]/", "");
		
		int index = IMEI.length()-4;
		if(index<0)
			index=0;
		
		String model = Build.MODEL.replaceAll("/[^0-9A-Za-z\\-]/", "");
		
		String ssid = model+"_"+IMEI.substring(index);
		String sharedKey = phoneNumber;
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		if(sharedPref.getString("ssid",null)==null) {
	        SharedPreferences.Editor prefEditor = sharedPref.edit();
	        prefEditor.putString("ssid",ssid);
	        prefEditor.putString("password",sharedKey);
	        prefEditor.commit();
		}
	}
}
