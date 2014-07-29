package com.itp13113.filesync.gdrive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.DriveRequestInitializer;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.itp13113.filesync.services.CloudFile;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageInterface;

class OnTokenAcquired implements AccountManagerCallback<Bundle> {
    private GoogleDriveDriver gDriver;

    OnTokenAcquired(GoogleDriveDriver gDriver) {
        this.gDriver = gDriver;
    }

    @Override
    public void run(AccountManagerFuture<Bundle> result) {
        System.out.println("Shalalala");
        try {
            final String token = result.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            HttpTransport httpTransport = new NetHttpTransport();
            JacksonFactory jsonFactory = new JacksonFactory();
            Drive.Builder b = new Drive.Builder(httpTransport, jsonFactory, null);
            b.setDriveRequestInitializer(new DriveRequestInitializer() {
                @Override
                public void initializeDriveRequest(DriveRequest request) throws IOException {
                    DriveRequest driveRequest = request;
                    driveRequest.setPrettyPrint(true);
                    driveRequest.setKey("f2:76:87:34:e0:e9:ff:f2:02:0c:44:f3:53:2e:95:01:25:10:f3:ee");
                    driveRequest.setOauthToken(token);
                }
            });

            gDriver.drive = b.build();
            System.out.println("ok...");
        } catch (OperationCanceledException e) {
            gDriver.drive = null;
        } catch (AuthenticatorException e) {
            gDriver.drive = null;
        } catch (IOException e) {
            gDriver.drive = null;
        }

        //notify the google drive driver object that we have finished with the authentication
        System.out.println("notf");
        synchronized(gDriver.authenticationComplete) {
            gDriver.authenticationComplete.notify();
        }
    }
}

public class GoogleDriveDriver implements CloudStorageInterface {
    private Context context;
    protected Drive drive = null;
    private String currentDirectory;
    //locks
    protected Integer authenticationComplete = new Integer(0);

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public String getStorageServiceTitle() {
        return "Google Drive";
    }

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void authenticate() throws CloudStorageAuthenticationError {
        final AccountManager am = AccountManager.get(context);

        if (am == null) {
            System.out.println("Could not retrieve Google Drive Account manager");
            throw new CloudStorageAuthenticationError();
        }

        if (am.getAccounts().length == 0) {
            System.out.println("No Google account found");
            throw new CloudStorageAuthenticationError();
        }
        else {
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        final String token = am.blockingGetAuthToken(am.getAccounts()[0], //get the first available google account
                                "oauth2:" + DriveScopes.DRIVE, true
                                /*new Bundle(),
                                true,
                                new OnTokenAcquired(this),
                                null*/);

                        HttpTransport httpTransport = new NetHttpTransport();
                        JacksonFactory jsonFactory = new JacksonFactory();
                        Drive.Builder b = new Drive.Builder(httpTransport, jsonFactory, null);
                        b.setDriveRequestInitializer(new DriveRequestInitializer() {
                            @Override
                            public void initializeDriveRequest(DriveRequest request) throws IOException {
                                DriveRequest driveRequest = request;
                                driveRequest.setPrettyPrint(true);
                                driveRequest.setKey("f2:76:87:34:e0:e9:ff:f2:02:0c:44:f3:53:2e:95:01:25:10:f3:ee");
                                driveRequest.setOauthToken(token);
                            }
                        });

                        drive = b.build();
                        System.out.println("ok...");
                    } catch (OperationCanceledException e) {
                        drive = null;
                        System.out.println("Could not complete operation");
                    } catch (IOException e) {
                        drive = null;
                        System.out.println("I/O exception");
                    } catch (AuthenticatorException e) {
                        drive = null;
                        System.out.println("Could not authenticate");
                    }

                    synchronized(authenticationComplete) {
                        authenticationComplete.notify();
                    }
                } });
            //start the authentication thread
            thread.start();

            //wait for the authentication to complete
            synchronized(authenticationComplete) {
                try {
                    authenticationComplete.wait();
                } catch (InterruptedException e) {
                }
            }

        }
    }

    @Override
    public void setDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public Vector<CloudFile> list() {
        System.out.println("ls");
        final Vector<CloudFile> resultList = new Vector<CloudFile>();
        final Integer listingComplete = new Integer(0);

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    List<com.google.api.services.drive.model.File> res = new ArrayList<File>();
                    Drive.Files.List request = drive.files().list();
                    System.out.println("~~~Listing");
                    do {
                        try {
                            FileList files = request.setQ("'" +  currentDirectory + "' in parents and trashed=false").execute();

                            res.addAll(files.getItems());
                            request.setPageToken(files.getNextPageToken());
                        } catch (IOException e) {
                            System.out.println("An error occurred: " + e);
                            request.setPageToken(null);
                        }
                    } while (request.getPageToken() != null &&
                            request.getPageToken().length() > 0);

                    for(com.google.api.services.drive.model.File f: res) {
                        resultList.add( new CloudFile(f.getId(), f.getTitle(), f.getIconLink(), f.getMimeType().equals("directory"), f.getMimeType()) );
                        System.out.println("~~~" + f.getTitle() + " " + f.getIconLink() + " " + f.getMimeType() + " " + f.getId());
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("~~~Listed");
                //notify parent function that listing has completed
                synchronized(listingComplete) {
                    listingComplete.notify();
                }
            }
        });

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
