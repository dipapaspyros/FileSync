package com.itp13113.filesync.services;

import android.content.Context;
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

    abstract public Vector<CloudFile> list();


}
