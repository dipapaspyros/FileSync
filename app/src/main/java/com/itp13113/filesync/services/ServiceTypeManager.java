package com.itp13113.filesync.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

import com.google.api.services.drive.DriveScopes;
import com.itp13113.filesync.dropbox.DropboxDriver;
import com.itp13113.filesync.gdrive.GoogleDriveDriver;


/*Listener class to handle connection and callbacks*/
class NewServiceClickListener implements OnClickListener {
    private ServiceTypeManager serviceTypeManager;
    private Context context;
    private Activity activity;
	private ServiceType serviceType;
	
	public NewServiceClickListener(ServiceTypeManager serviceTypeManager, Activity activity, ServiceType serviceType) {
        this.serviceTypeManager = serviceTypeManager;
        this.context = activity.getApplicationContext();
        this.activity = activity;
		this.serviceType = serviceType;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
	public void onClick(View v) {
        System.out.println(serviceType);

		if (serviceType.id.equals("gdrive")) {
            GoogleDriveDriver gDriver = new GoogleDriveDriver();
            try {
                gDriver.setContext(context);
                gDriver.authenticate();
                gDriver.setDirectory( gDriver.getHomeDirectory() );
                gDriver.list();
            } catch (CloudStorageAuthenticationError cloudStorageAuthenticationError) {
                System.out.println("Could not authenticate");
                cloudStorageAuthenticationError.printStackTrace();
            }
        }
        else if (serviceType.id.equals("dropbox")) {
            serviceTypeManager.dDriver = new DropboxDriver();
            try {
                serviceTypeManager.dDriver.setContext(context);
                serviceTypeManager.dDriver.authenticate();
            } catch (CloudStorageAuthenticationError cloudStorageAuthenticationError) {
                System.out.println("Could not authenticate");
                cloudStorageAuthenticationError.printStackTrace();
            }
        }

	}

}

public class ServiceTypeManager {
	private ArrayList<ServiceType> service_types;
	//private Context applicationContext;
	private AssetManager assetManager;

    public DropboxDriver dDriver;

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
                          if (ptr >=0 && ptr<5 && xmlText[ptr].equals(""))
                            xmlText[ptr] = parser.getText();
                          break;
	                  case XmlPullParser.END_TAG:
                          name=parser.getName();
                           if(name.equals("service_type")){ //add the service type to the stored types
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

	        //load the icon if it was specified
            if (!service_type.icon.equals("")) {
                try {
                    Drawable icon = Drawable.createFromResourceStream(activity.getResources(), null, assetManager.open(service_type.icon), service_type.icon, new BitmapFactory.Options());
                    b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                } catch (IOException e) {
                //invalid icon name - do nothing
                }
            }

            NewServiceClickListener cl = new NewServiceClickListener(this, activity, service_type);
	        b.setOnClickListener(cl);
	        
	        l.addView(b);
		}
	}
}
