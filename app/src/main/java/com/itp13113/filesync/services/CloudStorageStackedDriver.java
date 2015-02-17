package com.itp13113.filesync.services;

import java.util.Stack;
import java.util.Vector;

/**
 * Created by dimitris on 2/9/2014.
 */
public abstract class CloudStorageStackedDriver extends CloudStorageDriver {

    protected String currentFolderID;
    private Stack<String> prevFolderIDs = new Stack<String>();

    @Override
    public void setDirectory(String directory) throws CloudStorageDirectoryNotExists {
        if (directory.equals("..") && currentDirectory.equals(getHomeDirectory())) { //parent directory not available on <home>
            directoryHasChanged = false;
            return;
        }

        directoryExists = true;

        if (directory.equals(".")) { //current directory
            directoryHasChanged = false;
            return;
        }

        if (directory.equals("..")) { //parent directory
            directoryHasChanged = true;
            currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
            currentFolderID = prevFolderIDs.pop();
            if (currentFolderID.equals("NULL")) {
                directoryExists = false;
            }
            return;
        }

        currentDirectory += "/" + directory;
        prevFolderIDs.push(new String(currentFolderID));

        for (CloudFile file : fileList) { //find a subdirectory
            if (file.isDirectory() && file.getTitle().equals(directory)) {
                directoryHasChanged = true;
                currentFolderID = file.getId();
                return;
            }
        }

        //directory not found in the drive
        directoryExists = false;
        directoryHasChanged = false;
        currentFolderID = "NULL";

        throw new CloudStorageDirectoryNotExists();
    }

    protected String getDirectoryID() { return currentFolderID; }

}
