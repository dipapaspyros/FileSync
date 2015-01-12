package com.itp13113.filesync.dropbox;

import android.content.Context;
import android.content.SharedPreferences;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.itp13113.filesync.services.CloudFile;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageAuthorizationError;
import com.itp13113.filesync.services.CloudStorageDriver;

import com.dropbox.core.*;
import com.itp13113.filesync.services.CloudStorageNotEnoughSpace;
import com.itp13113.filesync.util.NetworkJob;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by dimitris on 29/7/2014.
 */
class DropboxCloudFile extends CloudFile {
    private DropboxAPI.Entry e;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    public DropboxCloudFile( DropboxAPI<AndroidAuthSession> mDBApi, String iconFile, DropboxAPI.Entry e) {
        super(e.fileName(), e.fileName(), iconFile, e.isDir, e.mimeType);

        this.e = e;
        this.size = e.bytes;
        this.mDBApi = mDBApi;

        System.out.println("----" + e.fileName() + " " + iconFile + " " + e.mimeType + e.contents);
    }

    @Override
    public String openUrl() {
        String url = "";
        if (!e.isDir) {
            try {
                url = mDBApi.media(e.path, true).url;
            } catch (DropboxException e1) {
                url = "";
            }
        }

        return url;
    }

    @Override
    public String downloadUrl() {
        String url = "";
        if (!e.isDir) {
            try {
                url = mDBApi.media(e.path, true).url;
            } catch (DropboxException e1) {
                url = "";
            }
        }

        return url;
    }

    @Override
    public String shareUrl() {
        final Integer waitForUrl = new Integer(0);
        final String[] url = {""};

        if (!e.isDir) { //no share url for folder
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        url[0] = mDBApi.share(e.path).url;
                    } catch (DropboxException e1) {
                        url[0] = "";
                    }

                    synchronized(waitForUrl) { //notify that the url was retrieved
                        waitForUrl.notify();
                    }
                }
            });
            thread.start();

            synchronized(waitForUrl) { //wait for the url to be retrieved
                try {
                    waitForUrl.wait();
                } catch (InterruptedException e) {
                }
            }

            return url[0];
        }

        return "";
    }

    @Override
    public void delete() {
        final Integer waitForDelete = new Integer(0);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (e.isDir) { //delete directory with all its contents
                        mDBApi.delete(e.path + "/");
                    }else { //delete the file
                        mDBApi.delete(e.path);
                    }
                } catch (DropboxException e1) {
                    e1.printStackTrace();
                }

                synchronized(waitForDelete) { //notify that the file was deleted
                    waitForDelete.notify();
                }
            }
        });
        thread.start();

        synchronized(waitForDelete) { //wait for the file to be deleted
            try {
                waitForDelete.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public String info() {
        String info = "Last modified on: " + e.modified + ", type is ";
        if (isDirectory()) {
            info += "directory";
        } else {
            info += e.mimeType;
        }

        info += ".";

        return info;
    }
}

public class DropboxDriver extends CloudStorageDriver {
    private Integer listingComplete = new Integer(0);

    final static private String APP_KEY = "pcgi5otwwfny748";
    final static private String APP_SECRET = "z0nq7lnwk33kud3";
    final static private Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

    private boolean waitAuthorization = false;

    public String key = "";
    public String secret = "";

    public DropboxAPI<AndroidAuthSession> mDBApi;
    private String displayName;

    public DropboxDriver(String key, String secret) {
        this.key = key;
        this.secret = secret;

        //initialization function:
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    @Override
    public String getStorageServiceTitle() {
        String result = "Dropbox";

        if (displayName != null) {
            result += " - " + this.displayName;
        }

        return result;
    }

    @Override
    public String getHomeDirectory() {
        return "/";
    }

    @Override
    public void authorize() throws CloudStorageAuthorizationError {
        this.waitAuthorization = true;
        mDBApi.getSession().startAuthentication(context);
    }

    public void authorizeComplete() {
        this.waitAuthorization = false;

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                AccessTokenPair accessToken = mDBApi.getSession().getAccessTokenPair();
                this.key = accessToken.key;
                this.secret = accessToken.secret;
            } catch (IllegalStateException e) {
                System.err.println("Error authenticating dropbox account");
            }
        } else {
            System.err.println("Could not log in to dropbox");
        }
    }

    public boolean waitAuthorization() {return this.waitAuthorization;}

    @Override
    public void authenticate() throws CloudStorageAuthenticationError {
        mDBApi.getSession().setAccessTokenPair(new AccessTokenPair(key, secret));

        //store account display name
        //network operation so it needs a thread
        final DropboxDriver that = this;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    that.displayName = " - " + mDBApi.accountInfo().displayName;
                } catch (DropboxException e) {
                    that.displayName = "";
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private String getIconFile(String ic) {
        return "icons/dropbox/" + ic + ".gif";
    }

    @Override
    public Vector<CloudFile> list() {
        //check if this directory exists in the drive
        if (!this.directoryExists) {
            fileList.removeAllElements();
            return fileList;
        }

        if (!directoryHasChanged) {
            return fileList;
        }
        fileList.removeAllElements();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    DropboxAPI.Entry entries = mDBApi.metadata(currentDirectory, 100, null, true, null);
                    for (DropboxAPI.Entry e : entries.contents) {
                        if (!e.isDeleted) { //ignore from listing deleted files
                            fileList.add(new DropboxCloudFile(mDBApi, getIconFile(e.icon), e));
                        }
                    }
                } catch (DropboxException e) {
                    System.err.println("Dropbox could not list " + currentDirectory + " directory");
                    e.printStackTrace();
                }

                synchronized(listingComplete) {
                    listingComplete.notify();
                }
            }});
        //start the file listing thread
        thread.start();

        //wait for it to complete listing
        System.out.println("Waiting ls");
        synchronized(listingComplete) {
            try {
                listingComplete.wait();
            } catch (InterruptedException e) {
            }
        }
        System.out.println("Ls  complete");

        return fileList;
    }

    public long getTotalSpace() {
        try {
            return mDBApi.accountInfo().quota;
        } catch (DropboxException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getUsedSpace() {
        try {
            return mDBApi.accountInfo().quotaNormal + mDBApi.accountInfo().quotaShared;
        } catch (DropboxException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getFreeSpace() {
        return this.getTotalSpace() - this.getUsedSpace();
    }

    public String uploadFile(NetworkJob job, String local_file, String parentID, String new_file) throws CloudStorageNotEnoughSpace {
        final NetworkJob myJob = job;

        //get original file
        java.io.File lcFile = new java.io.File(local_file);

        //check if there is enough space for the file
        if (this.getFreeSpace() < lcFile.length()) {
            throw new CloudStorageNotEnoughSpace();
        }

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(lcFile);
        } catch (FileNotFoundException e) {
            System.err.println("Could not read file " + local_file);
            e.printStackTrace();
        }

        DropboxAPI.Entry newEntry = null;
        try {
            newEntry = mDBApi.putFile(parentID +new_file, inputStream,
                    lcFile.length(), null, new ProgressListener() {
                        private long prevBytes = 0;

                        @Override
                        public void onProgress(long bytes, long total) {
                            synchronized (this) {
                                long new_bytes = bytes - prevBytes;
                                prevBytes = bytes;
                                myJob.appendCompletedBytes(new_bytes);
                            }
                        }

                        @Override
                        public long progressInterval() {
                            return -1;//always report back
                        }
                    });
        } catch (DropboxException e) {
            System.err.println("Could not upload file " + local_file + " to " + getStorageServiceTitle());
            e.printStackTrace();
        }

        return newEntry.path;
    }

    public String createDirectory(String parentID, String new_directory) throws CloudStorageNotEnoughSpace {
        DropboxAPI.Entry newEntry = null;
        try {
            newEntry = mDBApi.createFolder(parentID + "/" + new_directory);
        } catch (DropboxException e) {
            System.err.println("Could not create folder " + parentID + "/" + new_directory + " at " + getStorageServiceTitle());
            e.printStackTrace();
        }

        return newEntry.path + "/";
    }
}
