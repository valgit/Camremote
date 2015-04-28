package net.dnsalias.vbr.camremote;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class CamremoteSetting extends PreferenceActivity {
	private static final String TAG = "CamremoteSetting";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	   super.onCreate(savedInstanceState);
	   try {
		   addPreferencesFromResource(R.xml.settings);
	   }
       catch (Exception ex)
       {
           Log.e(TAG, Log.getStackTraceString(ex));
       }
	   initializePref();
	}
	
	private void initializePref() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String foobar = prefs.getString("foobar", "hi");
        Log.i(TAG, foobar);
    }
}
