package com.itp13113.filesync;

import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.services.AccountConfigurationManager;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class WelcomeActivity extends ActionBarActivity {

	private AccountConfigurationManager serviceTypeManager;

    private String readFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getApplicationContext().openFileInput(fileName)));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

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
		serviceTypeManager = new AccountConfigurationManager( getApplicationContext(), getAssets() );

        //show existing storage accounts
        if (MainActivity.storageManager.initialized) {
            //change ui elements from welcome to settings
            ((TextView) this.findViewById(R.id.textView1)).setText("Settings");
            ((Button) this.findViewById(R.id.readyButton)).setText("Save changes");

            serviceTypeManager.storages = this.readFile("storages.xml");
            //remove the closing tag so it can be added on save
            serviceTypeManager.storages = serviceTypeManager.storages.substring(0,  serviceTypeManager.storages.indexOf("</storages>"));
            System.out.println(serviceTypeManager.storages);
            serviceTypeManager.showExistingStorages(MainActivity.storageManager, linearLayout);
        }
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
