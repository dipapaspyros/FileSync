package com.itp13113.filesync.onedrive;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.itp13113.filesync.MainActivity;
import com.itp13113.filesync.services.CloudFile;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageAuthorizationError;
import com.itp13113.filesync.services.CloudStorageDirectoryNotExists;
import com.itp13113.filesync.services.CloudStorageDriver;

import java.util.Arrays;
import java.util.Stack;
import java.util.Vector;

import com.itp13113.filesync.services.CloudStorageStackedDriver;
import com.itp13113.filesync.services.StorageManager;
import com.microsoft.live.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dimitris on 1/9/2014.
 */
class OneDriveCloudFile extends CloudFile {
    private LiveConnectClient client;
    private JSONObject f;
    private String openUrl;

    public OneDriveCloudFile(LiveConnectClient client, JSONObject f, String id, String title, String iconLink, boolean isDirectory, String mimeType, Long size, String openUrl) {
        super(id, title, iconLink, isDirectory, mimeType);

        this.client = client;
        this.f = f;
        this.size = size;
        this.openUrl = openUrl;
    }

    @Override
    public String openUrl() {
        return openUrl;
    }

    @Override
    public String downloadUrl() {
        return openUrl;
    }

    @Override
    public String shareUrl() {
        return null;
    }

    @Override
    public void delete() {
        final Integer waitForDelete = new Integer(0);
        final String id = this.getId();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                client.deleteAsync(id, new LiveOperationListener() {
                    public void onError(LiveOperationException exception, LiveOperation operation) {
                        System.err.println("Error deleting onedrive file: " + exception.getMessage());
                    }
                    public void onComplete(LiveOperation operation) {
                        System.out.println(getTitle() + " deleted.");
                    }
                });
            }
        });
        thread.start();
    }

    @Override
    public String info() {
        String info = "";

        if (isDirectory()) {
            //count items inside
            int count = 0;
            try {
                count = f.getInt("count");
            } catch (JSONException e) {
            }

            if (count>0) {
                info += count;
            } else {
                info += "No";
            }

            info += " items inside. ";
        }

        try {
            info += "Last modified on " + f.getString("updated_time") + ".";
        } catch (JSONException e) {

        }

        return info;
    }
}

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

            if (driver.storageManager != null) {
                driver.storageManager.resetCashe();
                driver.storageManager.list();
            }
        }
        else {
            driver.client = null;
            System.out.println("OneDrive could not authenticate");
        }
    }

    @Override
    public void onAuthError(LiveAuthException exception, Object userState) {
        driver.client = null;
        System.out.println("Error signing in: " + exception.getMessage());
    }
}

public class OneDriveDriver extends CloudStorageStackedDriver {
    private Activity activity;
    private LiveAuthClient auth;
    protected String token;
    protected LiveConnectClient client;

    protected StorageManager storageManager;

    private String APP_CLIENT_ID = "0000000048127603";

    public OneDriveDriver(Activity activity, StorageManager storageManager) {
        this.activity = activity;
        this.storageManager = storageManager;
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
    public void authorize() throws CloudStorageAuthorizationError {
         OneDriveDriverAuthListener listener = new OneDriveDriverAuthListener(this);

         auth = new LiveAuthClient(activity, APP_CLIENT_ID);

         Iterable<String> scopes = Arrays.asList("wl.offline_access", "wl.signin", "wl.basic", "wl.skydrive", "wl.skydrive_update");
         auth.initialize(scopes, listener);
         auth.login(activity, scopes, listener);
    }

    @Override
    public void authenticate() throws CloudStorageAuthenticationError {
        OneDriveDriverAuthListener listener = new OneDriveDriverAuthListener(this);

        auth = new LiveAuthClient(activity, APP_CLIENT_ID);

        Iterable<String> scopes = Arrays.asList("wl.offline_access", "wl.signin", "wl.basic", "wl.skydrive", "wl.skydrive_update");
        auth.initialize(scopes, listener);
    }

    private String getIcon(String ftype, String fname) {
        if (ftype.equals("folder")) {
            return "icons/onedrive/folder.png";
        }

        return "icons/onedrive/file.png";
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

        final Integer listingComplete = new Integer(0);
        fileList.removeAllElements();

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

                                fileList.add(new OneDriveCloudFile(client, file, id, name, icon, type.equals("folder"), type, file.getLong("size"), file.getString("upload_location")+"?access_token="+token));
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
