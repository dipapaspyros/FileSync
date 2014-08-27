package com.itp13113.filesync.services;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.gdrive.GoogleDriveDriver;

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
public class StorageManager implements CloudStorageInterface {
    private Context context;
    private LinearLayout fileList;
    private ArrayList<CloudStorageInterface> storages;
    private AssetManager assetManager;

    public Integer onResume = new Integer(0);
    private Drawable icon;

    public StorageManager(AssetManager assetManager, LinearLayout fileList) {
        this.assetManager = assetManager;
        this.fileList = fileList;

        storages = new ArrayList<CloudStorageInterface>();

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

        for (int i = 0; i < storages.size(); i++) {
            storages.get(i).setContext(context);
        }
    }

    @Override
    public String getStorageServiceTitle() {
        return "All storages";
    }

    @Override
    public String getHomeDirectory() {
        return "";
    }

    @Override
    public void authenticate() throws CloudStorageAuthenticationError {

        for (int i = 0; i < storages.size(); i++) {
            CloudStorageInterface storage = storages.get(i);
            storage.authenticate();
            storage.setDirectory(storage.getHomeDirectory());
        }
    }

    @Override
    public void setDirectory(String directory) {
        for (int i = 0; i < storages.size(); i++) {
            storages.get(i).setDirectory(directory);
        }
    }

    @Override
    public Vector<CloudFile> list() {
        //get a list of all the files
        final Vector<CloudFile> result = new Vector<CloudFile>();

        for (int i = 0; i < storages.size(); i++) {
            result.addAll(storages.get(i).list());
        }

        //show the files
        fileList.removeAllViewsInLayout();
        for (CloudFile file : result) {
            Button b = new Button(context);
            b.setText(file.getTitle());

            //load the icon if it was specified
            if (!file.getIconLink().equals("")) {
                try {
                    Drawable icon  = Drawable.createFromResourceStream(context.getResources(), null, assetManager.open(file.getIconLink()), file.getIconLink(), new BitmapFactory.Options());
                    b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                } catch (IOException e) {
                    //invalid icon name - do nothing
                    System.out.println("Icon could not be displayed - " + file.getIconLink());
                }
            }

            fileList.addView(b);
        }


        return result;
    }
}
