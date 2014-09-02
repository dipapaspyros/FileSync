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
import com.itp13113.filesync.services.CloudStorageDriver;

import com.dropbox.core.*;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by dimitris on 29/7/2014.
 */
public class DropboxDriver extends CloudStorageDriver {
    private Integer listingComplete = new Integer(0);

    final static private String APP_KEY = "pcgi5otwwfny748";
    final static private String APP_SECRET = "z0nq7lnwk33kud3";
    final static private Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

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
    public void authenticate() throws CloudStorageAuthenticationError {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.itp13113.FileSync", Context.MODE_PRIVATE);
        String key = prefs.getString("DropboxKey", "");
        String secret = prefs.getString("DropboxSecret", "");

        if (key.equals("") || secret.equals("")) { //first time authenticating
            mDBApi.getSession().startAuthentication(context);
        } else { //has authenticated before
            mDBApi.getSession().setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    private String getIconFile(String ic) {
        return "icons/dropbox/" + ic + ".gif";
    }

    @Override
    public Vector<CloudFile> list() {
        fileList.removeAllElements();
        //check if this directory exists in dropbox
        if (!this.directoryExists) {
            return fileList;
        }

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    DropboxAPI.Entry entries = mDBApi.metadata(currentDirectory, 100, null, true, null);
                    for (DropboxAPI.Entry e : entries.contents) {
                        String icon = getIconFile(e.icon);
                        System.out.println("----" + e.fileName() + " " + icon + " " + e.mimeType + " " + e.hash);
                        fileList.add(new CloudFile(e.fileName(), e.fileName(), icon, e.isDir, e.mimeType, e.bytes));
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
