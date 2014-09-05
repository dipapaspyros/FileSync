package com.itp13113.filesync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dropbox.client2.session.AccessTokenPair;
import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.StorageManager;

public class MainActivity extends ActionBarActivity {
    private StorageManager storageManager = null;
    private LinearLayout fileList, contextMenu;
    private TextView fileInfo;
    private EditText dirTextView;
    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get the list view where files will be presented and the context menu
        fileList = (LinearLayout) this.findViewById(R.id.fileList);
        contextMenu = (LinearLayout) this.findViewById(R.id.contextMenu);

        //get the file info text view
        fileInfo = (TextView) this.findViewById(R.id.fileInfo);

        //get the directory title edit view
        dirTextView = (EditText) this.findViewById(R.id.dirTextView);
        //get the progress bar
        loading = (ProgressBar) this.findViewById(R.id.loading);

        //check if configuration with stored services exists
        try {
            InputStream inputStream = openFileInput("storages.xml");
            storageManager = new StorageManager(this, getAssets(), fileList, fileInfo, contextMenu, loading, dirTextView);
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

    @Override
    public void onResume() {
        super.onResume();

        if ((storageManager == null)) { //first run
            try {
                InputStream inputStream = openFileInput("storages.xml");
                storageManager = new StorageManager(this, getAssets(), fileList, fileInfo, contextMenu, loading, dirTextView);
                storageManager.setContext(getApplicationContext());
                storageManager.authenticate();
                storageManager.list();
            } catch (CloudStorageAuthenticationError e) {
                System.out.println("Authentication Error");
                e.printStackTrace();
            } catch (FileNotFoundException e) {

            }
        }
    }

    @Override
    public void onBackPressed() {
        onContextClose((ImageButton) this.findViewById(R.id.upButton));

        if (storageManager == null) {
            super.onBackPressed();
            return;
        }

        if (storageManager.getHomeDirectory().equals(storageManager.getDirectory())) { //if already at <home> just leave the application
            super.onBackPressed();
        } else { //return to previous (1 level up) directory
            onUpClick((ImageButton) this.findViewById(R.id.upButton));
        }
    }

    //clicks

    public void onRefreshClick(View v) {
        onContextClose(v);
        storageManager.resetCashe();
        storageManager.list();
    }

    public void onUpClick(View v) {
        onContextClose(v);
        // go to up level
        storageManager.setDirectory("..");
        storageManager.list();
    }

    public void onContextClose(View v) {
        if (storageManager != null) {
            storageManager.contextFile = null;
        }
        contextMenu.setVisibility(View.GONE);
    }

    public void onContextOpen(View v) {
        storageManager.openFile( storageManager.contextFile );

        onContextClose(v);
    }

    public void onContextDownload(View v) {
        //storageManager.openFile( storageManager.contextFile );

        onContextClose(v);
    }

    public void onContextShare(View v) {
        storageManager.downloadFile( storageManager.contextFile );

        onContextClose(v);
    }

    public void onContextDelete(View v) {
        //storageManager.openFile( storageManager.contextFile );

        onContextClose(v);
    }
}
