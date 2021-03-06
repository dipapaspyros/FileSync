package com.itp13113.filesync.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import com.itp13113.filesync.util.NetworkJob;
import com.itp13113.filesync.util.StorageOperation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Callable;

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

    public boolean is_changed = false;
    public boolean initialized = false;

    public void initialize(Activity activity, AssetManager assetManager, LinearLayout fileListView, TextView fileInfo, LinearLayout contextMenu, ProgressBar loading, EditText dirEditText) {
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

        this.initialized = true;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public ArrayList<CloudStorageDriver> getStorages() {
        return this.storages;
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

                    //check if multiple storages are shown
                    int nOfdrives = 0;
                    for (CloudStorageDriver storage : storages) {
                        if (storage.directoryExists) {
                            nOfdrives++;
                        }
                    }
                    boolean showDriveInfo = false;
                    if (nOfdrives > 1) {
                        showDriveInfo = true;
                    }

                    //get a list of all the files
                    fileList.removeAllElements();
                    for (CloudStorageDriver storage : storages) {
                        if (!storage.directoryExists) { //skip empty drives
                            continue;
                        }

                        Vector<CloudFile> list = storage.list();
                        fileList.addAll(list);

                        //show drive infro
                        if (showDriveInfo) {
                            final TextView textView = new TextView(context);
                            textView.setPadding(10,10,10,10);
                            textView.setTextColor(Color.rgb(68,108,179));
                            textView.setTextSize(24);
                            textView.setText(storage.getStorageServiceTitle() + "(" + list.size() + ")");

                            //add the file/folder button
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fileListView.addView(textView);
                                }
                            });
                        }

                        //show the files
                        for (CloudFile file : list) {
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
                                    System.err.println("Icon could not be displayed - " + file.getIconLink());
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
    public boolean storagePicker(final StorageOperation storageOperation) {
        final ArrayList<CloudStorageDriver> non_empty_storages = new ArrayList<CloudStorageDriver>();
        for (CloudStorageDriver storage : storages) {
            if (storage.directoryExists) {
                non_empty_storages.add(storage);
            }
        }

        if (non_empty_storages.size() == 0) { //no non-empty drive directory -- should not happen normally
            return false;
        }

        if (non_empty_storages.size() == 1) { //exactly one non-empty -- autopick
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    storageOperation.onStorageSelect( non_empty_storages.get(0) );
                }
            });
            thread.start();

            return true;
        }

        //create CharSequence[] object
        final CharSequence[] options = new CharSequence[non_empty_storages.size()];
        for (int i =0; i < non_empty_storages.size(); i++) {
            options[i] = non_empty_storages.get(i).getStorageServiceTitle();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Pick a driver");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                storageOperation.onStorageSelect ( non_empty_storages.get(which) );
            }
        });
        builder.show();

        return true;
    }

    public String uploadFile(final NetworkJob job, final String local_file, final String parentID, final String new_file) {
        storagePicker(new StorageOperation() {
            @Override
            public void onStorageSelect(CloudStorageDriver driver) {
                try {
                    driver.uploadFile(job, local_file, parentID, new_file);
                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    System.err.println("Not enough storage in " + driver.getStorageServiceTitle());
                }
            }
        });

        return "Uploading file...";
    }

    public String createDirectory(final String parentID, final String new_directory) throws CloudStorageNotEnoughSpace {
        storagePicker(new StorageOperation() {
            @Override
            public void onStorageSelect(CloudStorageDriver driver) {
                try {
                    driver.createDirectory(parentID, new_directory);
                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    System.err.println("Not enough storage in " + driver.getStorageServiceTitle());
                }
            }
        });

        return "Creating directory...";
    }

    public String uploadDirectory(final NetworkJob job, final String local_directory, final String parentID, final String new_directory) throws CloudStorageNotEnoughSpace {
        storagePicker(new StorageOperation() {
            @Override
            public void onStorageSelect(CloudStorageDriver driver) {
                try {
                    uploadDirectory(job, local_directory, parentID, new_directory);
                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    System.err.println("Not enough storage in " + driver.getStorageServiceTitle());
                }
            }
        });

        return "Uploading directory...";
    }
}
