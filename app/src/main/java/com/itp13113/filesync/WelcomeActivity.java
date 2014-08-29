package com.itp13113.filesync;

import com.dropbox.client2.session.AccessTokenPair;
import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.services.ServiceTypeManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class WelcomeActivity extends ActionBarActivity {

	private ServiceTypeManager serviceTypeManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		//get the activity layout
		LinearLayout linearLayout = (LinearLayout) this.findViewById(R.id.linearLayout);
		
		//load all service types
		serviceTypeManager = new ServiceTypeManager( getApplicationContext(), getAssets() );
		serviceTypeManager.showServiceTypes(this, linearLayout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_welcome,
					container, false);
			return rootView;
		}
	}

    protected void onResume() {
        super.onResume();

        if (serviceTypeManager.dDriver != null) {
            if (serviceTypeManager.dDriver.mDBApi.getSession().authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    serviceTypeManager.dDriver.mDBApi.getSession().finishAuthentication();


                    AccessTokenPair accessToken = serviceTypeManager.dDriver.mDBApi.getSession().getAccessTokenPair();

                    //store the access token
                    SharedPreferences prefs = this.getSharedPreferences(
                            "com.itp13113.FileSync", Context.MODE_PRIVATE);
                    prefs.edit().putString("DropboxKey", accessToken.key).apply();
                    prefs.edit().putString("DropboxSecret", accessToken.secret).apply();

                    serviceTypeManager.dDriver.list();
                } catch (IllegalStateException e) {
                    System.out.println("Error authenticating dropbox");
                }
            }
        }
    }

}
