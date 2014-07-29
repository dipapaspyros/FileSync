package com.itp13113.filesync.services;

import android.content.Context;
import java.util.Vector;

/**
 * Created by dimitris on 27/7/2014.
 */
public interface CloudStorageInterface {

    public void setContext(Context context);
    public String getStorageServiceTitle();

    public void authenticate() throws CloudStorageAuthenticationError;
    public void setDirectory(String directory);
    public Vector<CloudFile> list();


}
