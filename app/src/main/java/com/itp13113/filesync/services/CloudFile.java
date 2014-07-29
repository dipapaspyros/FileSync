package com.itp13113.filesync.services;

/**
 * Created by dimitris on 27/7/2014.
 */
enum CloudFileType {CFT_FILE, CFT_DIRECTORY};

public class CloudFile {
    private String id;
    private String title;
    private String iconLink;
    private CloudFileType fileType;
    private String mimeType;

    public CloudFile(String id, String title, String iconLink, boolean isDirectory, String mimeType) {
        this.id = id;
        this.title = title;
        this.iconLink = iconLink;
        if (isDirectory)
            this.fileType = CloudFileType.CFT_DIRECTORY;
        else
            this.fileType = CloudFileType.CFT_FILE;
        this.mimeType = mimeType;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIconLink() {
        return iconLink;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isDirectory() {
        return this.fileType == CloudFileType.CFT_DIRECTORY;
    }
}
