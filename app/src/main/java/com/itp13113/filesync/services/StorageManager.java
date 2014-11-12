package com.itp13113.filesync.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.gdrive.GoogleDriveDriver;
import com.itp13113.filesync.onedrive.OneDriveDriver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
        //hide the context menu
        storageManager.contextMenu.setVisibility(View.GONE);

        storageManager.openFile(file);
    }
}

class CloudFileLongClickListener implements View.OnLongClickListener {
    private CloudFile file;
    private StorageManager storageManager;

    public CloudFileLongClickListener(StorageManager storageManager, CloudFile file) {
        this.storageManager = storageManager;
        this.file = file;
    }

    @Override
    public boolean onLongClick(View view) {
        //rearrange items so that the menu appears under the file
        storageManager.fileListView.removeView(storageManager.contextMenu);
        storageManager.fileListView.addView(storageManager.contextMenu, storageManager.fileListView.indexOfChild(view) + 1);

        //load the info
        storageManager.fileInfo.setText(file.info());

        //show the menu
        storageManager.contextMenu.setVisibility(View.VISIBLE);

        //set the context file
        storageManager.contextFile = file;
        storageManager.contextFileButton = (Button) view;

        return true;
    }
}

public class StorageManager extends CloudStorageDriver {
    private Activity activity;
    protected LinearLayout fileListView, contextMenu;
    protected TextView fileInfo;
    private ProgressBar loading;
    private EditText dirEditText;
    private ArrayList<CloudStorageDriver> storages;
    private AssetManager assetManager;
    public CloudFile contextFile = null;
    public Button contextFileButton = null;

    public Integer onResume = new Integer(0);
    private Drawable icon;

    public StorageManager(Activity activity, AssetManager assetManager, LinearLayout fileListView, TextView fileInfo, LinearLayout contextMenu, ProgressBar loading, EditText dirEditText) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.assetManager = assetManager;
        this.fileListView = fileListView;
        this.contextMenu = contextMenu;
        this.fileInfo = fileInfo;
        this.loading = loading;
        this.dirEditText = dirEditText;

        storages = new ArrayList<CloudStorageDriver>();

        try {
            String[] xmlText = new String[5];
            int ptr = -1;

            //create the dom factory
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

            //read the storages xml file
            InputStream inputStream = context.openFileInput("storages.xml");

            try {
                DocumentBuilder builder = domFactory.newDocumentBuilder();
                Document dom = builder.parse(inputStream);
                Element root = dom.getDocumentElement();

                NodeList items = root.getChildNodes();
                for (int i = 0; i < items.getLength(); i++) {
                    Node item = items.item(i);
                    if (item instanceof Element) {
                        Element element = (Element) item;
                        if (element.getTagName().equals("storage")) { //a new storage element
                            String type = element.getAttribute("type");
                            System.out.println("Init - " + type);
                            if (type.equals("gdrive")) {
                                storages.add(new GoogleDriveDriver(element.getAttribute("name")));
                            } else if (type.equals("dropbox")) {
                                storages.add(new DropboxDriver(element.getAttribute("key"), element.getAttribute("secret")));
                            } else if (type.equals("onedrive")) {
                                storages.add(new OneDriveDriver(activity, this));
                            } else {
                                System.out.println("Unsupported storage type: " + type);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void openFile(CloudFile file) {
        final CloudFile f = file;

        if (file.isDirectory()) { //open the directory
            setDirectory(file.getTitle());
            list();
        } else { //open the file - default action
            Thread thread = new Thread(new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(f.openUrl()));
                    browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(browserIntent);
                }
            }));
            thread.start();
        }
    }

    public void downloadFile(CloudFile file) {
        final CloudFile f = file;

        if (!file.isDirectory()) { //download the file
            Thread thread = new Thread(new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(f.downloadUrl()));
                    browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(browserIntent);
                }
            }));
            thread.start();
        }
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
    public void authorize() throws CloudStorageAuthorizationError {
        return; //no operation needed - authorization has already happened for each storage
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
        final StorageManager that = this;


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (that.fileList) { //synchronize on fileList so that a new list() command will have to wait
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fileListView.removeAllViews();
                            fileListView.addView(contextMenu);
                            loading.setVisibility(View.VISIBLE);
                        }
                    });

                    //get a list of all the files
                    fileList.removeAllElements();
                    for (CloudStorageDriver storage : storages) {
                        fileList.addAll(storage.list());
                    }

                    //show the files
                    for (CloudFile file : fileList) {
                        final Button b = new Button(context);
                        String bTitle = file.getTitle();
                        if (!file.isDirectory()) {
                            bTitle = bTitle + " (" + file.getFileSizeReadable() + ")";
                        }
                        b.setText(bTitle);

                        //load the icon if it was specified
                        if (!file.getIconLink().equals("")) {
                            try {
                                Drawable icon = Drawable.createFromResourceStream(context.getResources(), null, assetManager.open(file.getIconLink()), file.getIconLink(), new BitmapFactory.Options());
                                b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                            } catch (IOException e) { //invalid icon name - do nothing
                                System.out.println("Icon could not be displayed - " + file.getIconLink());
                            }
                        }

                        CloudFileClickListener cl = new CloudFileClickListener(that, file);
                        CloudFileLongClickListener lcl = new CloudFileLongClickListener(that, file);
                        b.setOnClickListener(cl);
                        b.setOnLongClickListener(lcl);

                        //add the file/folder button
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fileListView.addView(b);
                            }
                        });
                    }

                    if (fileList.isEmpty()) { //show appropriate message on empty directories
                        final TextView tv = new TextView(context);
                        tv.setText("This folder is empty");
                        tv.setPadding(5, 5, 5, 5);
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fileListView.addView(tv);
                            }
                        });

                        }
                    }

                //hide the loading progress bar
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                    }
                });
            }
        });
        thread.start();

        return null;
    }

    public long getTotalSpace() {
        long totalSpace = 0;

        for (CloudStorageDriver storage : storages) {
            totalSpace += storage.getTotalSpace();
        }

        return totalSpace;
    }

    public long getUsedSpace() {
        long usedSpace = 0;

        for (CloudStorageDriver storage : storages) {
            usedSpace += storage.getUsedSpace();
        }

        return usedSpace;
    }

    public long getFreeSpace() {
        long freeSpace = 0;

        for (CloudStorageDriver storage : storages) {
            freeSpace += storage.getFreeSpace();
        }

        return freeSpace;
    }

    /*Show a picker to select one of the available storages*/
    /*If only one storage is available at that directory, it is autoselected*/
    //TODO: implement method CloudStorageDriver
    public CloudStorageDriver storagePicker() {
        return null;
    }

    public String uploadFile(String local_file, String parentID, String new_file) throws CloudStorageNotEnoughSpace {
        return storagePicker().uploadFile(local_file, parentID, new_file);
    }

    public String createDirectory(String parentID, String new_directory) throws CloudStorageNotEnoughSpace {
        return storagePicker().createDirectory(parentID, new_directory);
    }

    public String uploadDirectory(String local_directory, String parentID, String new_directory) throws CloudStorageNotEnoughSpace {
        return storagePicker().uploadDirectory(local_directory, parentID, new_directory);
    }
}
