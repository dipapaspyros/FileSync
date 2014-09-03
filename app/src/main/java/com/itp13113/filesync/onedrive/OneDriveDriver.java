package com.itp13113.filesync.onedrive;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.itp13113.filesync.MainActivity;
import com.itp13113.filesync.services.CloudFile;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageDirectoryNotExists;
import com.itp13113.filesync.services.CloudStorageDriver;

import java.util.Arrays;
import java.util.Stack;
import java.util.Vector;

import com.itp13113.filesync.services.CloudStorageStackedDriver;
import com.microsoft.live.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dimitris on 1/9/2014.
 */
class OneDriveDriverAuthListener implements LiveAuthListener {
    OneDriveDriver driver;

    OneDriveDriverAuthListener(OneDriveDriver driver) {
        this.driver = driver;
    }

    @Override
    public void onAuthComplete(LiveStatus status, LiveConnectSession session, Object userState) {
        if(status == LiveStatus.CONNECTED) {
            driver.client = new LiveConnectClient(session);
            driver.token = session.getAccessToken();
        }
        else {
            driver.client = null;
            System.out.println("OneDrive could not authenticate");
        }
    }

    @Override
    public void onAuthError(LiveAuthException exception, Object userState) {
        System.out.println("Error signing in: " + exception.getMessage());
    }
}

public class OneDriveDriver extends CloudStorageStackedDriver {
    private Activity activity;
    private LiveAuthClient auth;
    protected String token;
    protected LiveConnectClient client;
    private String APP_CLIENT_ID = "0000000048127603";

    protected Integer authenticationComplete = new Integer(0); //authentication lock

    public OneDriveDriver(Activity activity) {
        this.activity = activity;
        this.currentFolderID = "me/skydrive";
    }

    @Override
    public String getStorageServiceTitle() {
        return "Microsoft OneDrive";
    }

    @Override
    public String getHomeDirectory() {
        return "me/skydrive";
    }

    @Override
    public void authenticate() throws CloudStorageAuthenticationError {
        OneDriveDriverAuthListener listener = new OneDriveDriverAuthListener(this);

        auth = new LiveAuthClient(activity, APP_CLIENT_ID);
        Iterable<String> scopes = Arrays.asList("wl.signin", "wl.basic", "wl.skydrive", "wl.skydrive_update");
        auth.login(activity, scopes, listener);
    }

    private String getIcon(String ftype, String fname) {
        if (ftype.equals("folder")) {
            return "icons/onedrive/folder.png";
        }

        return "icons/onedrive/file.png";
    }

    @Override
    public Vector<CloudFile> list() {
        final Integer listingComplete = new Integer(0);

        fileList.removeAllElements();
        //check if this directory exists in the drive
        if (!this.directoryExists) {
            return fileList;
        }

        if (client != null) { //verify that the Live Connect client has been loaded

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("ls");

                    try {
                        JSONObject result = client.get(currentFolderID + "/files").getResult();
                        System.out.println(result);

                        //get directory information
                        System.out.println("Folder ID = " + result.optString("id") +
                                ", name = " + result.optString("name"));

                        try {
                            JSONArray data = result.getJSONArray("data"); //get all directory files

                            for (int i = 0; i < data.length(); i++) {
                                JSONObject file = data.getJSONObject(i);
                                String id = file.getString("id");
                                String name = file.getString("name");
                                String type = file.getString("type");
                                System.out.println("----" + name + " " + type + " " + id);
                                String icon = getIcon(type, name);

                                fileList.add(new CloudFile(id, name, icon, type.equals("folder"), type, file.getLong("size"), file.getString("upload_location")+"?access_token="+token));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //notify parent function that listing has completed
                        synchronized (listingComplete) {
                            listingComplete.notify();
                        }

                    } catch (LiveOperationException e) {
                        e.printStackTrace();

                        //notify parent function that listing has completed
                        synchronized (listingComplete) {
                            listingComplete.notify();
                        }
                    }
                }
            });
            //start the file listing thread
            thread.start();

            //wait for it to complete listing
            System.out.println("Waiting ls");
            synchronized (listingComplete) {
                try {
                    listingComplete.wait();
                } catch (InterruptedException e) {
                }
            }
            System.out.println("Ls  complete");
        }

        return fileList;
    }
}
