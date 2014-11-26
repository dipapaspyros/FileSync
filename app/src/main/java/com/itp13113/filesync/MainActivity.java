package com.itp13113.filesync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageDriver;
import com.itp13113.filesync.services.CloudStorageNotEnoughSpace;
import com.itp13113.filesync.services.StorageManager;
import com.itp13113.filesync.util.StorageOperation;

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
        onContextClose((ImageButton) this.findViewById(R.id.prevButton));

        if (storageManager == null) {
            super.onBackPressed();
            return;
        }

        if (storageManager.getHomeDirectory().equals(storageManager.getDirectory())) { //if already at <home> just leave the application
            super.onBackPressed();
        } else { //return to previous (1 level up) directory
            onUpClick((ImageButton) this.findViewById(R.id.prevButton));
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
            storageManager.contextFileButton = null;
        }
        contextMenu.setVisibility(View.GONE);
    }

    public void onContextOpen(View v) { //context menu open click
        storageManager.openFile(storageManager.contextFile);

        onContextClose(v);
    }

    public void onContextDownload(View v) { //context menu download click
        //storageManager.openFile( storageManager.contextFile );

        onContextClose(v);
    }

    public void onContextShare(View v) { //context menu share click
        storageManager.downloadFile(storageManager.contextFile);

        onContextClose(v);
    }

    public void onContextDelete(View v) { //context menu delete click
        final Integer hasResponded = new Integer(0);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = v;

        builder.setTitle("Confirm");
        builder.setMessage("Do you want to delete " + storageManager.contextFile.getTitle() + "? This action can not be undone.");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                //Delete the file
                storageManager.contextFile.delete();

                //hide the button
                storageManager.contextFileButton.setVisibility(View.GONE);

                onContextClose(view);

                dialog.dismiss();
            }

        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Cancel action
                onContextClose(view);
                dialog.dismiss();
            }
        });

        //show the dialog
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onUploadClick(View v) {

    }

    public void onNewDirectoryClick(View v) {
        final EditText input = new EditText(this);
        final MainActivity that = this;

        new AlertDialog.Builder(this)
                .setTitle("Create a directory")
                .setMessage("Enter the name of the new directory")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Editable value = input.getText();
                        storageManager.storagePicker(new StorageOperation() {
                            @Override
                            public void onStorageSelect(CloudStorageDriver driver) { //pick a drive and create the directory
                                driver.createDirectory(value.toString(), that);
                            }
                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();


    }
}
