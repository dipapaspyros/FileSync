package com.itp13113.filesync.dropbox;

import android.content.Context;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.itp13113.filesync.services.CloudFile;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageInterface;

import com.dropbox.core.*;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by dimitris on 29/7/2014.
 */
public class DropboxDriver implements CloudStorageInterface {
    private Context context;
    private String directory;
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
    public void setContext(Context context) {
        this.context = context;
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
        mDBApi.getSession().startAuthentication(context);
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public Vector<CloudFile> list() {
        Vector<CloudFile> resultList = new Vector<CloudFile>();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    DropboxAPI.Entry entries = mDBApi.metadata(directory, 100, null, true, null);
                    for (DropboxAPI.Entry e : entries.contents) {
                        System.out.println("----" + e.fileName());
                    }
                } catch (DropboxException e) {
                    System.out.println("Dropbox could not list " + directory + " directory");
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

        return resultList;
    }
}