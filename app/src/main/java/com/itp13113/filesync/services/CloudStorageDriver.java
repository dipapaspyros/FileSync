package com.itp13113.filesync.services;

import android.content.Context;
import java.util.Vector;

/**
 * Created by dimitris on 27/7/2014.
 */
public abstract class CloudStorageDriver {

    protected String currentDirectory; //the current directory - home directory after authentication
    protected final Vector<CloudFile> fileList = new Vector<CloudFile>(); //list of directory files after list() is called
    protected boolean directoryExists = true;

    public CloudStorageDriver() {
        currentDirectory = getHomeDirectory();
    }

    abstract public void setContext(Context context);
    abstract public String getStorageServiceTitle();
    abstract  public String getHomeDirectory();

    abstract public void authenticate() throws CloudStorageAuthenticationError;

    public String getDirectoryTitle() {
        if (currentDirectory.equals( getHomeDirectory() )) {
            return "Home";
        } else {
            return currentDirectory.substring( currentDirectory.lastIndexOf("/")+1, currentDirectory.length());
        }

    }
    public void setDirectory(String directory) throws CloudStorageDirectoryNotExists {
        if (directory.equals("..") && currentDirectory.equals(getHomeDirectory())) { //parent directory unavaildable on <home>
            return;
        }

        directoryExists = true;

        if (directory.equals(".")) { //current directory
            return;
        }

        if (directory.equals("..")) { //parent directory
            currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
            return;
        }

        currentDirectory += "/" + directory;

        for (CloudFile file : fileList) { //find a subdirectory
            if (file.isDirectory() && file.getTitle().equals(directory)) {
                return;
            }
        }

        directoryExists = false;
        throw new CloudStorageDirectoryNotExists();
    }

    abstract public Vector<CloudFile> list();


}
