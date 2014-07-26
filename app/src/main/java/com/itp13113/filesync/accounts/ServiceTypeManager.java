package com.itp13113.filesync.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

class OnTokenAcquired implements AccountManagerCallback<Bundle> {
    private Activity activity;

    public OnTokenAcquired(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run(AccountManagerFuture<Bundle> result) {
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

            final Drive drive = b.build();

            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        List<com.google.api.services.drive.model.File> res = new ArrayList<com.google.api.services.drive.model.File>();
                        Drive.Files.List request = drive.files().list();
                        System.out.println("~~~Listing");
                        do {
                            try {
                                FileList files = request.setQ("'root' in parents and trashed=false").execute();

                                res.addAll(files.getItems());
                                request.setPageToken(files.getNextPageToken());
                            } catch (IOException e) {
                                System.out.println("An error occurred: " + e);
                                request.setPageToken(null);
                            }
                        } while (request.getPageToken() != null &&
                                request.getPageToken().length() > 0);

                        for(com.google.api.services.drive.model.File f: res) {
                            System.out.println("~~~" + f.getTitle() + " " + f.getIconLink() + " " + f.getMimeType() + " " + f.getId());

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("~~~Listed");
                }
            });

            thread.start();


            //System.out.println(drive.files().list().size());
            /*
            final com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            body.setTitle("My Test File");
            body.setDescription("A Test File");
            body.setMimeType("text/plain");

            final FileContent mediaContent = new FileContent("text/plain", an ordinary java.io.File you'd like to upload. Make it using a FileWriter or something, that's really outside the scope of this answer.)
            new Thread(new Runnable() {
                public void run() {
                    try {
                        com.google.api.services.drive.model.File file = drive.files().insert(body, mediaContent).execute();
                        alreadyTriedAgain = false; // Global boolean to make sure you don't repeatedly try too many times when the server is down or your code is faulty... they'll block requests until the next day if you make 10 bad requests, I found.
                    } catch (IOException e) {
                        if (!alreadyTriedAgain) {
                            alreadyTriedAgain = true;
                            AccountManager am = AccountManager.get(activity);
                            am.invalidateAuthToken(am.getAccounts()[0].type, null); // Requires the permissions MANAGE_ACCOUNTS & USE_CREDENTIALS in the Manifest
                            am.getAuthToken (same as before...)
                        } else {
                            // Give up. Crash or log an error or whatever you want.
                        }
                    }
                }
            }).start();*/
            Intent launch = (Intent)result.getResult().get(AccountManager.KEY_INTENT);
            if (launch != null) {
                activity.startActivityForResult(launch, 3025);
                return; // Not sure why... I wrote it here for some reason. Might not actually be necessary.
            }
        } catch (OperationCanceledException e) {
            // Handle it...
        } catch (AuthenticatorException e) {
            // Handle it...
        } catch (IOException e) {
            // Handle it...
        }
    }
}

/*Listener class to handle connection and callbacks*/
class NewServiceClickListener implements OnClickListener {
    private Context context;
    private Activity activity;
	private ServiceType serviceType;
	
	public NewServiceClickListener(Activity activity, ServiceType serviceType) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
		this.serviceType = serviceType;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
	public void onClick(View v) {
        System.out.println(serviceType);

		if (serviceType.auth_type.equals("gplay")) {
            AccountManager am = AccountManager.get(activity);
            if (am == null) {
                System.out.println("Uh???");
                return;
            }

            if (am.getAccounts().length == 0) {
                Toast.makeText(context, "empty", Toast.LENGTH_LONG).show();
            }
            else
            am.getAuthToken( am.getAccounts()[0], //get the first available google account
                    "oauth2:" + DriveScopes.DRIVE,
                    new Bundle(),
                    true,
                    new OnTokenAcquired(activity),
                    null);
		}
	}

}

public class ServiceTypeManager {
	private ArrayList<ServiceType> service_types;
	//private Context applicationContext;
	private AssetManager assetManager;


    public ServiceTypeManager(Context applicationContext, AssetManager assetManager) {
		//this.applicationContext = applicationContext;
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
	            parser.setInput( new InputStreamReader(service_types_ins) );

	            //parse service_types xml
	            int event = parser.getEventType();
	            while (event != XmlPullParser.END_DOCUMENT)
	            {
                    String name;
	               switch (event){
	                  case XmlPullParser.START_TAG:
                          name=parser.getName();
                          if (name.equals("service_type")) {
                              for (int i=0; i<5; i++) {
                                  xmlText[i] = "";
                              }
                          }
                          else
                          if (name.equals("id")) {
                              ptr = 0;
                          }
                          else if (name.equals("title")) {
                              ptr = 1;
                          }
                          else if (name.equals("icon")) {
                              ptr = 2;
                          }
                          else if (name.equals("auth_type")) {
                              ptr = 3;
                          }
                          else if (name.equals("list_uri")) {
                              ptr = 4;
                          }
                          //else ptr = -1;
	                      break;
                      case XmlPullParser.TEXT:
                          System.out.println("@" + ptr + " -> " + parser.getText());
                          if (ptr >=0 && ptr<5 && xmlText[ptr].equals(""))
                            xmlText[ptr] = parser.getText();
                          break;
	                  case XmlPullParser.END_TAG:
                          name=parser.getName();
                           if(name.equals("service_type")){ //add the service type to the stored types
                              System.out.println("##" + xmlText[0]);
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
	}
	
	public void showServiceTypes(Activity activity, LinearLayout l) {
		for(ServiceType service_type : service_types) {
			Button b = new Button(activity);
	        b.setText(service_type.title);
	        System.out.println(service_type);

	        //load the icon if it was specified
            if (!service_type.icon.equals("")) {
                try {
                    Drawable icon = Drawable.createFromResourceStream(activity.getResources(), null, assetManager.open(service_type.icon), service_type.icon, new BitmapFactory.Options());
                    b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                } catch (IOException e) {
                //invalid icon name - do nothing
                }
            }

            NewServiceClickListener cl = new NewServiceClickListener(activity, service_type);
	        b.setOnClickListener(cl);
	        
	        l.addView(b);
		}
	}
}
