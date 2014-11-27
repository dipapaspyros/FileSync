package com.itp13113.filesync.util;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.itp13113.filesync.MainActivity;

import java.math.BigInteger;

/**
 * Created by dimitris on 27/11/2014.
 */
public class NetworkJob {
    public String title;
    private boolean started = false;
    private long totalBytes = 0;
    private long completedBytes = 0;

    private ProgressBar progressBar;
    private TextView textView;
    private MainActivity mainActivity;

    NetworkJob(String title, ProgressBar progressBar, TextView textView, MainActivity mainActivity) {
        this.title = title;
        this.progressBar = progressBar;
        this.textView = textView;
        this.mainActivity = mainActivity;
    }

    public boolean hasStarted() {
        return started;
    }

    public boolean hasFinished() {
        return completedBytes == totalBytes;
    }

    public int getPercentageDone() {
        if (!started) {
            return 0;
        } else {
            return (int) Math.floor(100*((completedBytes + 0.0)/totalBytes));
        }
    }

    public void setTotalBytes(final long totalBytes) {
        this.totalBytes = totalBytes;
        started = true;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(title + "("  + ReadableFileSize.getReadableFileSize(totalBytes) + ") - 0%");
            }
        });
    }

    public void appendCompletedBytes(long moreCompletedBytes) {
        final NetworkJob that = this;
        completedBytes += moreCompletedBytes;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int percentage = that.getPercentageDone();
                progressBar.setProgress(percentage);
                textView.setText(title + "(" + ReadableFileSize.getReadableFileSize(totalBytes) + ") - " + percentage + "%");
                if (that.hasFinished()) {
                    textView.setText(title + "(" + ReadableFileSize.getReadableFileSize(totalBytes) + ") - " + "Completed");
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(mainActivity.getApplicationContext(), "Uploading " + that.title + " completed.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
