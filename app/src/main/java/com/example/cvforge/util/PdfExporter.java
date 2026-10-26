package com.example.cvforge.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.ui.cv.CvRenderer;
import com.example.cvforge.ui.cv.CvTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Saves a CV as a PDF with Android's PdfDocument. It draws with the same CvRenderer as the
 * preview on screen, so the PDF looks like the preview. Long CVs continue on a second page.
 */
public class PdfExporter {

    public interface Listener {
        /** Runs on the main thread. */
        void onDone(File pdf);

        void onError(String message);
    }

    // A4 in PDF points
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PdfExporter() {
    }

    /** Creates the PDF on a background thread and calls the listener when it is ready. */
    public static void export(Context context, Cv cv, CvTemplate template, Listener listener) {
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            try {
                File file = writePdf(appContext, cv, template);
                mainHandler.post(() -> listener.onDone(file));
            } catch (IOException | RuntimeException e) {
                mainHandler.post(() -> listener.onError(String.valueOf(e.getMessage())));
            }
        });
    }

    private static File writePdf(Context context, Cv cv, CvTemplate template) throws IOException {
        CvRenderer renderer = new CvRenderer(context);
        float scale = PAGE_WIDTH / CvRenderer.PAGE_WIDTH;
        float totalHeight = renderer.draw(null, cv, template) * scale;
        int pageCount = Math.max(1, (int) Math.ceil(totalHeight / PAGE_HEIGHT));

        PdfDocument document = new PdfDocument();
        try {
            for (int page = 0; page < pageCount; page++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, page + 1).create();
                PdfDocument.Page pdfPage = document.startPage(info);
                Canvas canvas = pdfPage.getCanvas();
                canvas.drawColor(Color.WHITE);
                // show only the slice of the long page that belongs on this PDF page
                canvas.translate(0, -page * PAGE_HEIGHT);
                canvas.scale(scale, scale);
                renderer.draw(canvas, cv, template);
                document.finishPage(pdfPage);
            }

            File folder = new File(context.getFilesDir(), "pdf");
            if (!folder.exists() && !folder.mkdirs()) {
                throw new IOException("Could not create the pdf folder");
            }
            File file = new File(folder, fileName(cv));
            try (FileOutputStream out = new FileOutputStream(file)) {
                document.writeTo(out);
            }
            return file;
        } finally {
            document.close();
        }
    }

    /** "CV_Ayesha_Rahman.pdf". Only letters and numbers are kept so the name is safe. */
    private static String fileName(Cv cv) {
        String name = cv.fullName == null ? "" : cv.fullName.trim().replaceAll("[^A-Za-z0-9]+", "_");
        return name.isEmpty() ? "CV.pdf" : "CV_" + name + ".pdf";
    }
}
