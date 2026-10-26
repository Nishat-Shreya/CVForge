package com.example.cvforge.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.cvforge.R;

import java.io.File;

/** Shares a file with other apps through an implicit Intent (ACTION_SEND). */
public class ShareHelper {

    private ShareHelper() {
    }

    /**
     * Opens the Android share sheet for a PDF. The file is in the app's private folder, so a
     * FileProvider gives the other app a temporary permission to read it.
     */
    public static void sharePdf(Context context, File pdf) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdf);

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, pdf.getName());
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_cv)));
    }
}
