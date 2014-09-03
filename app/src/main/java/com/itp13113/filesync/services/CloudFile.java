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
    private Long size;
    private String openUrl;

    public CloudFile(String id, String title, String iconLink, boolean isDirectory, String mimeType, long size, String openUrl) {
        this.id = id;
        this.title = title;
        this.iconLink = iconLink;

        if (isDirectory)
            this.fileType = CloudFileType.CFT_DIRECTORY;
        else
            this.fileType = CloudFileType.CFT_FILE;
        
        this.mimeType = mimeType;
        this.size = new Long(size);
        this.openUrl = openUrl;
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

    public Long getFileSize() {return size;}

    public String getOpenUrl() {return openUrl;}

    public String getFileSizeReadable() {
        long bytes = this.size.longValue();
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
