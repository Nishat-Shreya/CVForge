package com.example.cvforge.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/** Makes QR code pictures with the ZXing library. */
public class QrHelper {

    /**
     * The address a CV QR code points to. The public profile page is not built yet,
     * so for now this is only the planned address of the student's profile.
     */
    private static final String PROFILE_URL = "https://cvforge-93934.web.app/u/";

    private QrHelper() {
    }

    public static String profileLink(String uid) {
        return PROFILE_URL + (uid == null ? "example" : uid);
    }

    /** Dark squares on a white background, with no extra border. Returns null if encoding fails. */
    public static Bitmap create(String text, int sizePx) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int dark = Color.parseColor("#14123A");
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? dark : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }
}
