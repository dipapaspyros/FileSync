package com.itp13113.filesync.services;

import android.content.Context;
import android.widget.Toast;

import com.itp13113.filesync.MainActivity;
import com.itp13113.filesync.R;
import com.itp13113.filesync.util.NetworkJob;

import java.io.File;
import java.util.List;
import java.util.Vector;

/**
 * Created by dimitris on 27/7/2014.
 */
public abstract class CloudStorageDriver {

    protected Context context; //application context
    protected String currentDirectory; //the current directory - home directory after authentication
    protected final Vector<CloudFile> fileList = new Vector<CloudFile>(); //list of directory files after list() is called
    protected boolean directoryExists = true;
    protected boolean directoryHasChanged = true;

    public CloudStorageDriver() {
        currentDirectory = getHomeDirectory();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    abstract public String getStorageServiceTitle();

    abstract public String getHomeDirectory();

    abstract public void authorize() throws CloudStorageAuthorizationError;

    abstract public void authenticate() throws CloudStorageAuthenticationError;

    public String getDirectoryTitle() {
        if (currentDirectory.equals(getHomeDirectory())) {
            return "Home";
        } else {
            return currentDirectory.substring(currentDirectory.lastIndexOf("/") + 1, currentDirectory.length());
        }

    }

    public String getDirectory() {
        return currentDirectory;
    }

    protected String getDirectoryID() {
        return currentDirectory;
    }

    public void setDirectory(String directory) throws CloudStorageDirectoryNotExists {
        if (directory.equals("..") && currentDirectory.equals(getHomeDirectory())) { //parent directory unavaildable on <home>
            directoryHasChanged = false;
            return;
        }

        directoryExists = true;

        if (directory.equals(".")) { //current directory
            directoryHasChanged = false;
            directoryHasChanged = false;
            return;
        }

        if (directory.equals("..")) { //parent directory
            directoryHasChanged = true;
            currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
            return;
        }
        else {
            currentDirectory += "/" + directory;
        }

        for (CloudFile file : fileList) { //find a subdirectory
            if (file.isDirectory() && file.getTitle().equals(directory)) {
                directoryHasChanged = true;
                return;
            }
        }

        directoryExists = false;
        directoryHasChanged = false;
        throw new CloudStorageDirectoryNotExists();
    }

    public void resetCashe() {
        this.directoryHasChanged = true;
    }

    //listing information
    abstract public Vector<CloudFile> list();

    //quota information
    abstract public long getTotalSpace();

    abstract public long getUsedSpace();

    abstract public long getFreeSpace();

    //uploading
    abstract public String uploadFile(NetworkJob job, String local_file, String parentID, String new_file) throws CloudStorageNotEnoughSpace;

    public void uploadFile(final NetworkJob job, final String local_file, final String new_file, final MainActivity mainActivity) {
        final CloudStorageDriver that = this;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    that.uploadFile(job, local_file, that.getDirectoryID(), new_file);
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(that.getContext(), "Uploading " + new_file + " completed.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(that.getContext(), "Could not upload file", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    abstract public String createDirectory(String parentID, String new_directory) throws CloudStorageNotEnoughSpace;

    //creating new directories
    public void createDirectory(final String new_directory, final MainActivity mainActivity) {
        final CloudStorageDriver that = this;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    that.createDirectory(that.getDirectoryID(), new_directory);
                    mainActivity.onRefreshClick(mainActivity.findViewById(R.id.refreshButton));

                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(that.getContext(), "Could not create directory", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    /*Upload a directory by recursively creating directories and uploading files*/
    public void uploadDirectory(NetworkJob job, final String local_directory, String parentID, String new_directory, final MainActivity mainActivity) throws CloudStorageNotEnoughSpace {
        final String newFolderID = this.createDirectory(parentID, new_directory); //create the new directory
        if (newFolderID == "" || newFolderID == null) { //not valid id -- folder was not created
            throw new CloudStorageNotEnoughSpace();
        }

        final CloudStorageDriver that = this;

        File[] files = new File(local_directory).listFiles();
        for (File file : files) {
            final String new_name = file.getName();
            if (file.isDirectory()) {
                that.uploadDirectory(job, file.getAbsolutePath(), newFolderID, new_name, mainActivity);
            } else {
                that.uploadFile(job, file.getAbsolutePath(), newFolderID, new_name);
            }
        }
    }

    public void uploadDirectory(final NetworkJob job, final String local_directory, final String new_directory, final MainActivity mainActivity) {
        final CloudStorageDriver that = this;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    that.uploadDirectory(job, local_directory, that.getDirectoryID(), new_directory, mainActivity);
                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(that.getContext(), "Could not upload directory " + new_directory, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }

}
