package com.itp13113.filesync.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.gms.common.api.*;
import com.google.android.gms.drive.Drive;

class NewServiceClickListener implements OnClickListener {
    private Context context;
	private ServiceType serviceType;
	
	public NewServiceClickListener(Context context, ServiceType serviceType) {
        this.context = context;
		this.serviceType = serviceType;
	}

	@Override
	public void onClick(View v) {
		if (serviceType.auth_type.equals("gplay")) {
			GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
            .addApi(Drive.API)
            .addScope(Drive.SCOPE_FILE)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
		}
	}

};

public class ServiceTypeManager {
	private ArrayList<ServiceType> service_types;
	//private Context applicationContext;
	private AssetManager assetManager;
	
	public ServiceTypeManager(Context applicationContext, AssetManager assetManager) {
		//this.applicationContext = applicationContext;
		this.assetManager = assetManager;
		
		service_types = new ArrayList<ServiceType>();
		
		 try {
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
	               String name=parser.getName();
	               switch (event){
	                  case XmlPullParser.START_TAG:
	                  break;
	                  case XmlPullParser.END_TAG:
	                  if(name.equals("service_type")){ //add the service type to the stored types
	                	  ServiceType st = new ServiceType(parser.getAttributeValue(null,"id"), 
	                			  	parser.getAttributeValue(null,"title"),
		  							parser.getAttributeValue(null,"icon"), 
		  							parser.getAttributeValue(null,"auth_type"), 
		  							parser.getAttributeValue(null,"list_uri"));
	                	  service_types.add(st);
	                	  //Toast.makeText(applicationContext, st.toString(), Toast.LENGTH_LONG).show();
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
	        
	        b.setOnClickListener(new NewServiceClickListener(activity.getApplicationContext(), service_type));
	        
	        l.addView(b);
		}
	}
}
