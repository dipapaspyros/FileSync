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

    protected void onResume() { //on resume is called for dropbox authorization
        super.onResume();

        DropboxDriver dDriver = serviceTypeManager.getPendingDropboxDriver();
        if (dDriver != null) {
            dDriver.authorizeComplete();
            serviceTypeManager.storages += "<storage type=\"dropbox\" key=\"" + dDriver.key +"\" secret=\"" + dDriver.secret + "\" />";
        }
    }

    public void finalize(View v) { //called when the user presses i'm ready
        serviceTypeManager.finalize();
        super.onBackPressed();
    }

}
