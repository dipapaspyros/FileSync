package com.itp13113.filesync.dropbox;

import android.content.Context;
import android.content.SharedPreferences;

import com.dropbox.client2.DropboxAPI;
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

import java.io.BufferedInputStream;
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
        return null;
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

    private String key;
    private String secret;

    public DropboxAPI<AndroidAuthSession> mDBApi;

    public DropboxDriver() {
        // And later in some initialization function:
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    @Override
    public String getStorageServiceTitle() {
        return "Dropbox";
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

                //store the access token
                SharedPreferences prefs = context.getSharedPreferences(
                        "com.itp13113.FileSync", Context.MODE_PRIVATE);
                prefs.edit().putString("DropboxKey", accessToken.key).apply();
                prefs.edit().putString("DropboxSecret", accessToken.secret).apply();
            } catch (IllegalStateException e) {
                System.out.println("Error authenticating dropbox");
            }
        } else {
            System.out.println("Could not loggin to dropbox");
        }
    }

    public boolean waitAuthorization() {return this.waitAuthorization;}

    @Override
    public void authenticate() throws CloudStorageAuthenticationError {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.itp13113.FileSync", Context.MODE_PRIVATE);
        key = prefs.getString("DropboxKey", "");
        secret = prefs.getString("DropboxSecret", "");

        mDBApi.getSession().setAccessTokenPair(new AccessTokenPair(key, secret));
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
                        fileList.add(new DropboxCloudFile(mDBApi, getIconFile(e.icon), e));
                    }
                } catch (DropboxException e) {
                    System.out.println("Dropbox could not list " + currentDirectory + " directory");
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
}
