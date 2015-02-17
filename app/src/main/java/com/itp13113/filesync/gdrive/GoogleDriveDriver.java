package com.itp13113.filesync.gdrive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.DriveRequestInitializer;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.itp13113.filesync.services.CloudFile;
import com.itp13113.filesync.services.CloudStorageAuthenticationError;
import com.itp13113.filesync.services.CloudStorageAuthorizationError;
import com.itp13113.filesync.services.CloudStorageNotEnoughSpace;
import com.itp13113.filesync.services.CloudStorageStackedDriver;
import com.itp13113.filesync.util.NetworkJob;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

class GoogleCloudFile extends CloudFile {
    private Drive drive;
    private com.google.api.services.drive.model.File f;

    public GoogleCloudFile(Drive drive, com.google.api.services.drive.model.File f) {
        super(f.getId(), f.getTitle(),
                "icons/gdrive/" + f.getIconLink().substring(f.getIconLink().lastIndexOf("/") + 1),
                f.getMimeType().equals("application/vnd.google-apps.folder"), f.getMimeType());

        this.f = f;
        this.drive = drive;

        Long fSize = f.getFileSize(); long size;
        if (fSize != null) {
            this.size = fSize.longValue();
        } else {
            this.size = new Long(f.size());
        }

        System.out.println("----" + f.getTitle() + " " + f.getIconLink() + " " + f.getMimeType());
    }

    @Override
    public String openUrl() {
        String url;
        if (f.getDownloadUrl() == null) { //google docs
            url = f.getAlternateLink();
        } else { //other files
            url = f.getWebContentLink();
        }

        return url;
    }

    @Override
    public String downloadUrl() {
        String url;
        if (f.getDownloadUrl() == null) { //google docs
            url = f.getAlternateLink();
        } else { //other files
            url = f.getWebContentLink();
        }

        return url;
    }

    @Override
    public String shareUrl() {
        f.setShared(true);
        return f.getWebContentLink();
    }

    @Override
    public void delete() {
        final Integer waitForDelete = new Integer(0);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    drive.files().delete(f.getId()).execute();
                } catch (IOException e) {
                    System.err.println("An error occurred: " + e);
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
        String info = "";

        if (f.getLastViewedByMeDate() != null ) {
            info += "Last access on " + f.getLastViewedByMeDate().toStringRfc3339();
        } else {
            info += "Never accessed";
        }
        info += ", modified by " + f.getLastModifyingUser().getDisplayName() + ", type is " + f.getMimeType();

        return info;
    }
}

public class GoogleDriveDriver extends CloudStorageStackedDriver {
    protected Drive drive = null;
    private String accountName;

    //API Application key
    private String APP_KEY = "f2:76:87:34:e0:e9:ff:f2:02:0c:44:f3:53:2e:95:01:25:10:f3:ee";

    //locks
    Integer authenticationComplete = new Integer(0);

    public GoogleDriveDriver(String accountName) {
        this.accountName = accountName;
        this.currentFolderID = "root";
    }

    public String getAccountName() {
        return this.accountName;
    }

    @Override
    public String getStorageServiceTitle() {
        return "Google Drive - " + accountName;
    }

    @Override
    public String getHomeDirectory() {
        return "root";
    }

    @Override
    public void authorize() throws CloudStorageAuthorizationError {
        try {
            this.authenticate();
        } catch (CloudStorageAuthenticationError cloudStorageAuthenticationError) {
            cloudStorageAuthenticationError.printStackTrace();
            throw new CloudStorageAuthorizationError();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void authenticate() throws CloudStorageAuthenticationError {

        //get the account manager
        final AccountManager am = AccountManager.get(context);

        if (am == null) {
            System.err.println("Could not retrieve Google Drive Account manager");
            throw new CloudStorageAuthenticationError();
        }

        //find the account by name
        Account acf = null;
        for (Account acc : am.getAccounts()) {
            if (acc.name.equals(accountName)) {
                acf = acc;
                break;
            }
        }
        final Account account = acf;

        if (acf == null) {
            System.err.println("No Google account " + accountName + " found");
            throw new CloudStorageAuthenticationError();
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String token = am.blockingGetAuthToken(account, //get the first available google account
                                "oauth2:" + DriveScopes.DRIVE, true);

                        HttpTransport httpTransport = new NetHttpTransport();
                        JacksonFactory jsonFactory = new JacksonFactory();
                        Drive.Builder b = new Drive.Builder(httpTransport, jsonFactory, null);
                        b.setDriveRequestInitializer(new DriveRequestInitializer() {
                            @Override
                            public void initializeDriveRequest(DriveRequest request) throws IOException {
                                DriveRequest driveRequest = request;
                                driveRequest.setPrettyPrint(true);
                                driveRequest.setKey(APP_KEY);
                                driveRequest.setOauthToken(token);
                            }
                        });

                        drive = b.build();
                    } catch (OperationCanceledException e) {
                        drive = null;
                        System.err.println("Could not complete operation");
                    } catch (IOException e) {
                        drive = null;
                        System.err.println("I/O exception");
                    } catch (AuthenticatorException e) {
                        drive = null;
                        System.err.println("Could not authenticate");
                    }

                    synchronized (authenticationComplete) {
                        authenticationComplete.notify();
                    }
                }
            });
            //start the authentication thread
            thread.start();

            //wait for the authentication to complete
            synchronized (authenticationComplete) {
                try {
                    authenticationComplete.wait();
                } catch (InterruptedException e) {
                }
            }

        }

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

        System.out.println("GOOGLE DRIVE ls <" + currentDirectory + ">");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<com.google.api.services.drive.model.File> res = new ArrayList<File>();
                    Drive.Files.List request = drive.files().list();
                    System.out.println("~~~Listing");

                    do {
                        try {
                            FileList files = request.setQ("'" + currentFolderID + "' in parents and trashed=false").execute();

                            res.addAll(files.getItems());
                            request.setPageToken(files.getNextPageToken());
                        } catch (HttpResponseException e) {

                            if (e.getStatusCode() == 401) { // Credentials have been revoked.
                                System.err.println("Google Drive API credentials have been revoked");
                                authenticate();
                            }
                        } catch (IOException e) {
                            System.err.println("An error occurred: " + e);
                            request.setPageToken(null);

                        }

                    } while (request.getPageToken() != null &&
                            request.getPageToken().length() > 0);

                    //add shared with me directory when listing home
                    if (currentDirectory.equals( getHomeDirectory() )) {
                        FileList files = request.setQ("sharedWithMe").execute();
                        res.addAll(files.getItems());
                    }

                    for (com.google.api.services.drive.model.File f : res) {
                        fileList.add(new GoogleCloudFile(drive, f));
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("~~~Listed");
                //notify parent function that listing has completed
                synchronized (listingComplete) {
                    listingComplete.notify();
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

        return fileList;
    }

    public long getTotalSpace() {
        About about = null;
        try {
            about = drive.about().get().execute();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return about.getQuotaBytesTotal();
    }

    public long getUsedSpace() {
        About about = null;
        try {
            about = drive.about().get().execute();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return about.getQuotaBytesUsed();
    }

    public long getFreeSpace() {
        return this.getTotalSpace() - this.getUsedSpace();
    }

    public String uploadFile(final NetworkJob job, String local_file, String parentID, String new_file) throws CloudStorageNotEnoughSpace {
        //get original file
        java.io.File lcFile = new java.io.File(local_file);

        //check if there is enough space for the file
        if (this.getFreeSpace() < lcFile.length()) {
            throw new CloudStorageNotEnoughSpace();
        }

        //get file mime type
        String mimeType = "text/html";
        String extension = MimeTypeMap.getFileExtensionFromUrl(local_file);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension);
        }

        //create the drive file
        File newFile = new File();
        newFile.setTitle(new_file);
        newFile.setMimeType(mimeType);
        newFile.setParents(Arrays.asList(new ParentReference().setId(parentID)));

        //insert the file and upload contents
        final FileContent gContent = new FileContent(mimeType, lcFile);
        final long totalSize = gContent.getLength();

        try {
            Drive.Files.Insert insert = drive.files().insert(newFile, gContent);

            MediaHttpUploader uploader = insert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);
            uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
                long prevBytes = 0;

                @Override
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    long bytes;

                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            break;
                        case INITIATION_COMPLETE:
                            break;
                        case MEDIA_IN_PROGRESS:
                            bytes = uploader.getNumBytesUploaded() - prevBytes;
                            prevBytes += bytes;
                            job.appendCompletedBytes(bytes);
                            break;
                        case MEDIA_COMPLETE:
                            bytes = gContent.getLength() - prevBytes;
                            job.appendCompletedBytes(bytes);
                            break;
                        case NOT_STARTED :
                            break;
                    }
                }
            });
            File insertedFile = insert.execute();

            return insertedFile.getId();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String createDirectory(String parentID, String new_directory) throws CloudStorageNotEnoughSpace {
        //to create a directory just create an empty file with the appropriate mime type
        File newFile = new File();
        newFile.setTitle(new_directory);
        newFile.setMimeType("application/vnd.google-apps.folder");
        newFile.setParents(Arrays.asList(new ParentReference().setId(parentID)));

        try {
            File insertedFile = drive.files().insert(newFile).execute();
            return insertedFile.getId();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
