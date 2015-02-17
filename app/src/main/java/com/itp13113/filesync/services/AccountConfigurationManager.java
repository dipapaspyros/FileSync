package com.itp13113.filesync.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.drive.DriveScopes;
import com.itp13113.filesync.MainActivity;
import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.gdrive.GoogleDriveDriver;
import com.itp13113.filesync.onedrive.OneDriveDriver;


/*Listener class to handle connection and callbacks*/
class AddServiceClickListener implements OnClickListener {
    private AccountConfigurationManager serviceTypeManager;
    private Context context;
    private Activity activity;
    private ServiceType serviceType;
    private String accountName;

    public AddServiceClickListener(AccountConfigurationManager serviceTypeManager, Activity activity, ServiceType serviceType, String accountName) {
        this.serviceTypeManager = serviceTypeManager;
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.serviceType = serviceType;
        this.accountName = accountName;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onClick(View v) {
        CloudStorageDriver driver = null;

        if (serviceType.id.equals("gdrive")) {
            driver = new GoogleDriveDriver(accountName);
        } else if (serviceType.id.equals("dropbox")) {
            serviceTypeManager.dDriver = new DropboxDriver("", "");
            driver = serviceTypeManager.dDriver;
        } else if (serviceType.id.equals("onedrive")) {
            driver = new OneDriveDriver(activity, null);
        }

        if (driver != null) {

            try {
                driver.setContext(context);
                driver.authorize();

                if (!(driver instanceof DropboxDriver)) { //dropbox added on resume
                    String properties = "type=\"" + serviceType.id + "\" ";
                    if (driver instanceof GoogleDriveDriver) {
                        properties += "name=\"" + accountName + "\"";
                    }

                    serviceTypeManager.storages += "<storage " + properties + "/>";

                    if (driver instanceof GoogleDriveDriver) { //hide google driver
                        v.setVisibility(View.GONE);
                    }
                }
            } catch (CloudStorageAuthorizationError e) {
                System.out.println("Could not authorize " + driver.getStorageServiceTitle());
                e.printStackTrace();
            }

            //if the application is running add the new driver
            if (MainActivity.storageManager.initialized) {
                MainActivity.storageManager.getStorages().add(driver);
                try { //set the current directory
                    driver.setDirectory(MainActivity.storageManager.getDirectory());
                } catch (CloudStorageDirectoryNotExists cloudStorageDirectoryNotExists) {
                }
            }
        }
    }

}

/*Listener class to handle removing accounts*/
class RemoveServiceClickListener implements View.OnClickListener {
    private AccountConfigurationManager configurationManager;
    private StorageManager storageManager;
    private CloudStorageDriver driver;

    public RemoveServiceClickListener(AccountConfigurationManager configurationManager, StorageManager storageManager, CloudStorageDriver driver) {
        this.configurationManager = configurationManager;
        this.storageManager = storageManager;
        this.driver = driver;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onClick(View v) {
        if (this.driver != null) { //remove the driver from the storage manager & update the configuration
            configurationManager.removeStorage(this.driver);
            storageManager.getStorages().remove(this.driver);
            v.setVisibility(View.GONE);
        }
    }
}

public class AccountConfigurationManager {
    private ArrayList<ServiceType> service_types;
    private AssetManager assetManager;
    private Context context;
    private boolean hasFinalized = false;

    public String storages = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<storages>\n";

    protected DropboxDriver dDriver; //must be exposed for onResume to catch

    public DropboxDriver getPendingDropboxDriver() {

        if (dDriver == null) {
            return null;
        }

        if (dDriver.waitAuthorization()) {
            return dDriver;
        }

        return dDriver;
    }

    public AccountConfigurationManager(Context context, AssetManager assetManager) {
        this.context = context;
        this.assetManager = assetManager;

        service_types = new ArrayList<ServiceType>();

        try {
            String[] xmlText = new String[5];
            int ptr = -1;

            //create the xml parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();

            //read the service_types from asset file
            InputStream service_types_ins = assetManager.open("service_types.xml");
            parser.setInput(new InputStreamReader(service_types_ins));

            //parse service_types xml
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name;
                switch (event) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("service_type")) {
                            for (int i = 0; i < 5; i++) {
                                xmlText[i] = "";
                            }
                        } else if (name.equals("id")) {
                            ptr = 0;
                        } else if (name.equals("title")) {
                            ptr = 1;
                        } else if (name.equals("icon")) {
                            ptr = 2;
                        } else if (name.equals("auth_type")) {
                            ptr = 3;
                        } else if (name.equals("list_uri")) {
                            ptr = 4;
                        }
                        //else ptr = -1;
                        break;
                    case XmlPullParser.TEXT:
                        if (ptr >= 0 && ptr < 5 && xmlText[ptr].equals(""))
                            xmlText[ptr] = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equals("service_type")) { //add the service type to the stored types
                            ServiceType st = new ServiceType(xmlText[0], xmlText[1], xmlText[2], xmlText[3], xmlText[4]);
                            service_types.add(st);
                        }
                        break;
                }
                event = parser.next();

            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try { //read all existing storages
            InputStreamReader inputStreamReader = new InputStreamReader(this.context.openFileInput("storages.xml"));
            storages = "";
            char[] buffer = new char[8192];
            while (inputStreamReader.read(buffer, 0, buffer.length) > 0) {
                storages += buffer;
            }
            inputStreamReader.close();
        } catch (IOException e) {
            System.err.println("File read failed: " + e.toString());
        }
    }

    public void showServiceTypes(Activity activity, LinearLayout l) {
        //save accounts to make sure no account is shown twice
        ArrayList<String> google_mails = new ArrayList<String>();

        //subtitle
        TextView textView = new TextView(context);
        textView.setPadding(10,10,10,10);
        textView.setTextColor(Color.rgb(68, 108, 179));
        textView.setTextSize(24);
        textView.setText("Add accounts");
        l.addView(textView);

        for (ServiceType service_type : service_types) {
            if (service_type.id.equals("gdrive")) {
                final AccountManager am = AccountManager.get(context);

                if (am == null) {
                    System.out.println("Could not retrieve Account manager");
                    continue;
                }

                for (Account google_account : am.getAccounts()) {
                    Button b = new Button(activity);

                    //check tat the account is not already added
                    boolean added = false;
                    for (String google_mail : google_mails) {
                        if (google_mail.equals(google_account.name)) {
                            added = true;
                            break;
                        }
                    }
                    if (added) {
                        continue;
                    } else {
                        google_mails.add(google_account.name);
                    }

                    b.setText(service_type.title + " - " + google_account.name);
                    //load the icon if it was specified
                    if (!service_type.icon.equals("")) {
                        try {
                            Drawable icon = Drawable.createFromResourceStream(activity.getResources(), null, assetManager.open(service_type.icon), service_type.icon, new BitmapFactory.Options());
                            b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                        } catch (IOException e) {
                            //invalid icon name - do nothing
                        }
                    }

                    AddServiceClickListener cl = new AddServiceClickListener(this, activity, service_type, google_account.name);
                    b.setOnClickListener(cl);

                    l.addView(b);
                }
            } else {
                Button b = new Button(activity);

                b.setText(service_type.title);

                //load the icon if it was specified
                if (!service_type.icon.equals("")) {
                    try {
                        Drawable icon = Drawable.createFromResourceStream(activity.getResources(), null, assetManager.open(service_type.icon), service_type.icon, new BitmapFactory.Options());
                        b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                    } catch (IOException e) {
                        //invalid icon name - do nothing
                    }
                }

                AddServiceClickListener cl = new AddServiceClickListener(this, activity, service_type, "");
                b.setOnClickListener(cl);

                l.addView(b);
            }
        }
    }

    public void showExistingStorages(StorageManager manager, LinearLayout l) {
        //get storage manager accounts
        ArrayList<CloudStorageDriver> accounts = manager.getStorages();

        //subtitle
        TextView textView = new TextView(context);
        textView.setPadding(10,10,10,10);
        textView.setTextColor(Color.rgb(68, 108, 179));
        textView.setTextSize(24);
        textView.setText("Remove existing accounts");
        l.addView(textView);

        for (CloudStorageDriver storage : accounts) { //foreach account show a button to remove
            Button b = new Button(manager.getActivity());

            b.setText(storage.getStorageServiceTitle());
            try { //add delete icon
                Drawable icon = Drawable.createFromResourceStream(manager.context.getResources(), null, assetManager.open("icons/delete.png"), "icons/delete.png", new BitmapFactory.Options());
                b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            } catch (IOException e) {
                //invalid icon name - do nothing
            }

            RemoveServiceClickListener cl = new RemoveServiceClickListener(this, manager, storage);
            b.setOnClickListener(cl);

            l.addView(b);
        }
    }

    public void removeStorage(CloudStorageDriver driver) {
        int start_pos = 0, end_pos = 0, name_pos = 0;

        if (driver instanceof GoogleDriveDriver) {
            name_pos = storages.indexOf("name=\"" + ((GoogleDriveDriver) driver).getAccountName() + "\"");
        }
        else if (driver instanceof DropboxDriver) {
            name_pos = storages.indexOf("key=\"" + ((DropboxDriver) driver).key + "\"");
        }
        else if (driver instanceof OneDriveDriver) { //TODO: Detect multiple OneDrive accounts
            name_pos = storages.indexOf("type=\"onedrive\"");
        }

        // find where the service tag starts
        for (int i=name_pos - 1; i>=0; i--) {
            if (storages.charAt(i) == '<') {
                start_pos = i;
                break;
            }
        }

        // find where the service tag ends
        for (int i=name_pos + 1; i<storages.length(); i++) {
            if (storages.charAt(i) == '>') {
                end_pos = i;
                break;
            }
        }

        //update the xml
        storages = storages.substring(0, start_pos) + storages.substring(end_pos + 1);
        System.out.println("After remove: " + storages);
    }

    public void finalize() {
        if (!hasFinalized) {
            hasFinalized = true;
            storages += "</storages>";

            MainActivity.storageManager.is_changed = true;
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.context.openFileOutput("storages.xml", Context.MODE_PRIVATE));
                outputStreamWriter.write(storages);
                outputStreamWriter.close();
            } catch (IOException e) {
                System.err.println("File write failed: " + e.toString());
            }
        }
    }
}
