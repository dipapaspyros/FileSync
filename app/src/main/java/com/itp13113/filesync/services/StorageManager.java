package com.itp13113.filesync.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.gdrive.GoogleDriveDriver;
import com.itp13113.filesync.onedrive.OneDriveDriver;

import org.apache.james.mime4j.storage.Storage;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by dimitris on 26/8/2014.
 */

class CloudFileClickListener implements View.OnClickListener {
    private CloudFile file;
    private StorageManager storageManager;

    public CloudFileClickListener(StorageManager storageManager, CloudFile file) {
        this.storageManager = storageManager;
        this.file = file;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onClick(View v) {
        System.out.println(file.getTitle() + " " + file.isDirectory());

        if (file.isDirectory()) {
            storageManager.setDirectory( file.getTitle() );
            storageManager.list();
        }
    }
}

public class StorageManager extends CloudStorageDriver {
    private Activity activity;
    private Context context;
    private LinearLayout fileListView;
    private EditText dirEditText;
    private ArrayList<CloudStorageDriver> storages;
    private AssetManager assetManager;

    public Integer onResume = new Integer(0);
    private Drawable icon;

    public StorageManager(Activity activity, AssetManager assetManager, LinearLayout fileListView, EditText dirEditText) {
        this.assetManager = assetManager;
        this.fileListView = fileListView;
        this.dirEditText = dirEditText;

        storages = new ArrayList<CloudStorageDriver>();

        try {
            String[] xmlText = new String[5];
            int ptr = -1;

            //create the xml parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();

            //read the storages from asset file
            InputStream storages_ins = assetManager.open("storages.xml");
            parser.setInput(new InputStreamReader(storages_ins));

            //parse storages xml
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name, type = "";

                switch (event) {
                    case XmlPullParser.TEXT:
                        type = parser.getText();
                        if (type.equals("gdrive"))
                            storages.add(new GoogleDriveDriver());
                        else if (type.equals("dropbox"))
                            storages.add(new DropboxDriver());
                        else if (type.equals("onedrive"))
                            storages.add(new OneDriveDriver(activity));
                        break;
                }
                event = parser.next();

            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public DropboxDriver getPendingDropboxDriver() {
        //TODO: code to get the pending dropbox driver in order to resume it
        return (DropboxDriver) storages.get(1);
    }

    @Override
    public void setContext(Context context) {
        this.context = context;

        for (CloudStorageDriver storage : storages) {
            storage.setContext(context);
        }
    }

    @Override
    public String getStorageServiceTitle() {
        return "All storages";
    }

    @Override
    public String getHomeDirectory() {
        return "Home";
    }

    @Override
    public void authenticate() throws CloudStorageAuthenticationError {

        for (CloudStorageDriver storage : storages) {
            storage.authenticate();
        }
    }

    @Override
    public void setDirectory(String directory) {
        //call "change directory" for each storage
        for (CloudStorageDriver storage : storages) {
            try {
                storage.setDirectory(directory);
            } catch (CloudStorageDirectoryNotExists cloudStorageDirectoryNotExists) {

            }
        }

        //storage manager directory contains just the title
        this.currentDirectory = storages.get(0).getDirectoryTitle();
        //change the directory title in the text view
        dirEditText.setText(currentDirectory);
    }

    @Override
    public Vector<CloudFile> list() {
        //get a list of all the files
        fileList.removeAllElements();
        for (CloudStorageDriver storage : storages) {
            fileList.addAll(storage.list());
        }

        //show the files
        fileListView.removeAllViews();
        for (CloudFile file : fileList) {
            Button b = new Button(context);
            String bTitle = file.getTitle();
            if (!file.isDirectory()) {
                bTitle = bTitle + " (" + file.getFileSizeReadable() + ")";
            }
            b.setText(bTitle);

            //load the icon if it was specified
            if (!file.getIconLink().equals("")) {
                try {
                    Drawable icon  = Drawable.createFromResourceStream(context.getResources(), null, assetManager.open(file.getIconLink()), file.getIconLink(), new BitmapFactory.Options());
                    b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                } catch (IOException e) { //invalid icon name - do nothing
                    System.out.println("Icon could not be displayed - " + file.getIconLink());
                }
            }

            CloudFileClickListener cl = new CloudFileClickListener(this, file);
            b.setOnClickListener(cl);

            fileListView.addView(b);
        }


        return fileList;
    }
}
