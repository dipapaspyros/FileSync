package com.itp13113.filesync;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dropbox.client2.session.AccessTokenPair;
import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.StorageManager;

public class MainActivity extends ActionBarActivity {
    private StorageManager storageManager;
    private LinearLayout fileList;
    private EditText dirTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        //get the list view where files will be presented
        fileList = (LinearLayout) this.findViewById(R.id.fileList);
        //get the directory title edit view
        dirTextView = (EditText) this.findViewById(R.id.dirTextView);

		//check if configuration with stored services exists
        AssetManager mg = getResources().getAssets();
        try {
            mg.open("storages.xml");
            storageManager = new StorageManager( getAssets() , fileList, dirTextView);
            storageManager.setContext(getApplicationContext());
            storageManager.authenticate();
            storageManager.list();
        } catch (IOException ex) {
            Intent welcomeIntent = new Intent(getApplicationContext(), WelcomeActivity.class);
            startActivity(welcomeIntent);
        } catch (CloudStorageAuthenticationError cloudStorageAuthenticationError) {
            System.out.println("Authentication Error");
            cloudStorageAuthenticationError.printStackTrace();
        }

        if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

    protected void onResume() {
        super.onResume();

        /*DropboxDriver dDriver = storageManager.getPendingDropboxDriver();
        if (dDriver != null) {
            if (dDriver.mDBApi.getSession().authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    dDriver.mDBApi.getSession().finishAuthentication();

                    AccessTokenPair accessToken = dDriver.mDBApi.getSession().getAccessTokenPair();

                    //store the access token
                    SharedPreferences prefs = this.getSharedPreferences(
                            "com.itp13113.FileSync", Context.MODE_PRIVATE);
                    prefs.edit().putString("DropboxKey", accessToken.key).apply();
                    prefs.edit().putString("DropboxSecret", accessToken.secret).apply();

                    dDriver.setDirectory(dDriver.getHomeDirectory());
                    storageManager.list();

                } catch (IllegalStateException e) {
                    System.out.println("Error authenticating dropbox");
                }
            } else {
                System.out.println("Could not loggin to dropbox");
            }
        }*/
    }

    public void onRefreshClick(View v) {
        // list all files in all storages again
        storageManager.list();
    }

    public void onUpClick(View v) {
        // go to up level
        storageManager.setDirectory("..");
        storageManager.list();
    }

    @Override
    public void onBackPressed() {
        if (storageManager.getHomeDirectory().equals( storageManager.getDirectory())) { //return to parent directory
            super.onBackPressed();
        } else { //if already at <home> just leave the application
            onUpClick((ImageButton) this.findViewById(R.id.upButton));
        }
    }

}
