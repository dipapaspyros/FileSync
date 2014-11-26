package com.itp13113.filesync.services;

import android.content.Context;
import android.widget.Toast;

import com.itp13113.filesync.MainActivity;
import com.itp13113.filesync.R;

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
    public Context getContext() {return context;}

    abstract public String getStorageServiceTitle();
    abstract  public String getHomeDirectory();

    abstract public void authorize() throws CloudStorageAuthorizationError;
    abstract public void authenticate() throws CloudStorageAuthenticationError;

    public String getDirectoryTitle() {
        if (currentDirectory.equals( getHomeDirectory() )) {
            return "Home";
        } else {
            return currentDirectory.substring( currentDirectory.lastIndexOf("/")+1, currentDirectory.length());
        }

    }

    public String getDirectory() {
        return currentDirectory;
    }
    protected String getDirectoryID() { return currentDirectory; }

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

        currentDirectory += "/" + directory;

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

    public void resetCashe() { this.directoryHasChanged = true; }

    //listing information
    abstract public Vector<CloudFile> list();

    //quota information
    abstract public long getTotalSpace();
    abstract public long getUsedSpace();
    abstract public long getFreeSpace();

    //uploading
    abstract public String uploadFile(String local_file, String parentID, String new_file) throws CloudStorageNotEnoughSpace;
    public String uploadFile(String local_file, String new_file) {
        try {
            return this.uploadFile(local_file, this.getDirectoryID(),new_file);
        } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
            Toast.makeText(this.getContext(), "Could not upload file", Toast.LENGTH_LONG);
            return "";
        }
    }

    abstract public String createDirectory(String parentID, String new_directory) throws CloudStorageNotEnoughSpace;
    public void createDirectory(final String new_directory, final MainActivity mainActivity) {
        final CloudStorageDriver that = this;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    that.createDirectory(that.getDirectoryID(), new_directory);
                    mainActivity.onRefreshClick(mainActivity.findViewById(R.id.refreshButton));
                } catch (CloudStorageNotEnoughSpace cloudStorageNotEnoughSpace) {
                    Toast.makeText(that.getContext(), "Could not create directory", Toast.LENGTH_LONG);
                }
            }
        });
        thread.start();
    }

    /*Upload a directory by recursively creating directories and uploading files*/
    public String uploadDirectory(String local_directory, String parentID, String new_directory) throws CloudStorageNotEnoughSpace {
        String newFolderID = this.createDirectory(parentID, new_directory); //create the new directory

        File[] files = new File("local_directory").listFiles();
        for (File file : files) {
            String new_name = file.getName();
            if (file.isDirectory()) {
                this.uploadDirectory(file.getAbsolutePath(), newFolderID, new_name);
            }
            else {
                this.uploadFile(file.getAbsolutePath(), newFolderID, new_name);
            }
        }

        return newFolderID;
    }

}
