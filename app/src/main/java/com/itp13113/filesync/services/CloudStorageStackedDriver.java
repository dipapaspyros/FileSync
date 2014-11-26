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
        if (directory.equals("..") && currentDirectory.equals(getHomeDirectory())) { //parent directory unavaildable on <home>
            directoryHasChanged = false;
            return;
        }

        directoryExists = true;

        if (directory.equals(".")) { //current directory
            directoryHasChanged = false;
            return;
        }

        if (directory.equals("..") && !currentDirectory.equals(getHomeDirectory())) { //parent directory - unavaildable on <home>
            directoryHasChanged = true;
            currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
            currentFolderID = prevFolderIDs.pop();
            return;
        }

        currentDirectory += "/" + directory;
        prevFolderIDs.push(new String(currentFolderID));

        for (CloudFile file : fileList) { //find a subdirectory
            if (file.isDirectory() && file.getTitle().equals(directory)) {
                System.out.println("Folder ID = " + currentFolderID);
                directoryHasChanged = true;
                currentFolderID = file.getId();
                return;
            }
        }

        //directory not found in the drive
        directoryExists = false;
        directoryHasChanged = false;
        currentFolderID = "-1";

        throw new CloudStorageDirectoryNotExists();
    }

    protected String getDirectoryID() { return currentFolderID; }

}
