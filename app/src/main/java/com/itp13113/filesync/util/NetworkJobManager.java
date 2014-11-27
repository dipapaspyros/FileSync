package com.itp13113.filesync.util;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.itp13113.filesync.MainActivity;

import java.util.ArrayList;

/**
 * Created by dimitris on 27/11/2014.
 */
public class NetworkJobManager {

    private LinearLayout downloadsList;
    private AlertDialog uploadsDialog;
    private MainActivity mainActivity;

    public NetworkJobManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        //create a download list and add it to a scroll view to be scrollable
        downloadsList = new LinearLayout(mainActivity.getApplicationContext());
        downloadsList.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(mainActivity.getApplicationContext());
        scrollView.addView(downloadsList);

        //create the dialog
        uploadsDialog = new AlertDialog.Builder(mainActivity)
                .setView(scrollView)
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //just hide the dialog
                    }
                })
                .setTitle("Uploads")
                .setCancelable(false)
                .create();
    }

    public void show() {
        uploadsDialog.show();
    }

    public NetworkJob newNetworkJob(String title) {
        //create a progress bar and a label
        ProgressBar newProgressBar = new ProgressBar(mainActivity.getApplicationContext(),null, android.R.attr.progressBarStyleHorizontal);
        TextView textView = new TextView(mainActivity.getApplicationContext());
        textView.setText(title);
        textView.setTextSize(22);
        textView.setPadding(10,10,10,10);

        //append the new view to the main view
        downloadsList.addView(textView, 0);
        downloadsList.addView(newProgressBar, 0);

        ViewGroup.LayoutParams layoutParams = newProgressBar.getLayoutParams();
        layoutParams.width = LinearLayout.LayoutParams.FILL_PARENT;
        newProgressBar.setLayoutParams(layoutParams);

        return new NetworkJob(title, newProgressBar, textView, mainActivity);
    }

}
