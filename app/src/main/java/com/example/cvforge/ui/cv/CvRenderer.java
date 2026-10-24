package com.example.cvforge.ui.cv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.util.ListText;
import com.example.cvforge.util.QrHelper;

import java.util.List;

/**
 * Draws a CV with one of the three templates onto any Canvas.
 * The preview on screen and the PDF export (checkpoint 21) both use this class,
 * so they always look the same.
 *
 * All sizes are in "units" on a page that is PAGE_WIDTH units wide. The caller scales the
 * canvas to the real width. Pass a null canvas to only measure the height.
 */
public class CvRenderer {

    public static final float PAGE_WIDTH = 360f;
    private static final float MARGIN = 20f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    private final Context context;
    private final Typeface heading;
    private final Typeface headingBold;
    private final Typeface body;
    private final Typeface bodyBold;

    // set for every draw call
    private Canvas canvas;
    private CvTemplate template;
    private String qrLink;
    private Bitmap qrBitmap;
    private String bitmapLink;

    public CvRenderer(Context context) {
        this.context = context;
        headingBold = ResourcesCompat.getFont(context, R.font.sora_bold);
        heading = ResourcesCompat.getFont(context, R.font.sora_semibold);
        body = ResourcesCompat.getFont(context, R.font.dm_sans_regular);
        bodyBold = ResourcesCompat.getFont(context, R.font.dm_sans_bold);
    }

    /** Draws the CV and returns the page height in units. */
    public float draw(Canvas canvas, Cv cv, CvTemplate template) {
        this.canvas = canvas;
        this.template = template;
        this.qrLink = QrHelper.profileLink(cv.ownerUid);

        float y = drawHeader(cv);

        y = drawSummary(cv, y);
        y = drawSkills(cv, y);
        y = drawEducation(cv, y);
        y = drawProjects(cv, y);
        y = drawExperience(cv, y);
        y = drawCertificates(cv, y);
        y = drawLinks(cv, y);
        if (template != CvTemplate.MODERN) {
            y = drawQrFooter(y);
        }
        return y + MARGIN;
    }

    /** Classic and Minimal have no header tile, so the QR code sits at the bottom right. */
    private float drawQrFooter(float y) {
        float size = 52f;
        if (canvas != null) {
            drawQr(new RectF(PAGE_WIDTH - MARGIN - size, y, PAGE_WIDTH - MARGIN, y + size), qrLink);
            drawText("Scan to open my profile", paint(body, 9.5f, color(R.color.muted)),
                    MARGIN, y + size / 2f - 6f, CONTENT_WIDTH - size - 12f, true);
        }
        return y + size;
    }

    /** Draws the QR code picture inside the rectangle. The picture is made once per link. */
    private void drawQr(RectF target, String link) {
        if (qrBitmap == null || !link.equals(bitmapLink)) {
            qrBitmap = QrHelper.create(link, 200);
            bitmapLink = link;
        }
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, null, target, null);
        }
    }

    // ---------- header ----------

    private float drawHeader(Cv cv) {
        String name = isEmpty(cv.fullName) ? "Your Name" : cv.fullName;
        String contact = ListText.join(nonEmpty(cv.email, cv.phone, cv.location), "  ·  ");
        String headline = isEmpty(cv.headline) ? "" : cv.headline;

        switch (template) {
            case CLASSIC:
                return drawClassicHeader(name, headline, contact);
            case MINIMAL:
                return drawMinimalHeader(name, headline, contact);
            default:
                return drawModernHeader(name, headline, contact);
        }
    }

    /** Violet band with the name on the left and a white tile for the QR code on the right. */
    private float drawModernHeader(String name, String headline, String contact) {
        float qrSize = 56f;
        float textWidth = CONTENT_WIDTH - qrSize - 12f;
        float y = 20f;

        // measure first, because the band needs to know the text height
        float nameH = drawText(name, paint(headingBold, 20f, color(R.color.white)), MARGIN, y, textWidth, false);
        float headH = isEmpty(headline) ? 0 : drawText(headline, paint(body, 12f, color(R.color.white)),
                MARGIN, y + nameH + 2f, textWidth, false);
        float contactH = isEmpty(contact) ? 0 : drawText(contact, paint(body, 10f, color(R.color.white)),
                MARGIN, y + nameH + headH + 6f, textWidth, false);
        float bandHeight = Math.max(y + nameH + headH + contactH + 20f, y + qrSize + 20f);

        if (canvas != null) {
            canvas.drawRect(0, 0, PAGE_WIDTH, bandHeight, fill(color(R.color.violet)));
            RectF tile = new RectF(PAGE_WIDTH - MARGIN - qrSize, y, PAGE_WIDTH - MARGIN, y + qrSize);
            canvas.drawRoundRect(tile, 10f, 10f, fill(color(R.color.white)));
            drawQr(new RectF(tile.left + 5f, tile.top + 5f, tile.right - 5f, tile.bottom - 5f), qrLink);
            drawText(name, paint(headingBold, 20f, color(R.color.white)), MARGIN, y, textWidth, true);
            drawText(headline, paint(body, 12f, color(R.color.white)), MARGIN, y + nameH + 2f, textWidth, true);
            drawText(contact, paint(body, 10f, color(R.color.white)), MARGIN, y + nameH + headH + 6f, textWidth, true);
        }
        return bandHeight + 16f;
    }

    /** Centered serif name with a thin line under it. */
    private float drawClassicHeader(String name, String headline, String contact) {
        float y = 24f;
        y += drawCentered(name, paint(Typeface.SERIF, 22f, color(R.color.ink), true), y) + 2f;
        if (!isEmpty(headline)) {
            y += drawCentered(headline, paint(Typeface.SERIF, 12f, color(R.color.muted), false), y) + 2f;
        }
        if (!isEmpty(contact)) {
            y += drawCentered(contact, paint(body, 10f, color(R.color.muted)), y) + 2f;
        }
        y += 6f;
        if (canvas != null) {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 1.5f, fill(color(template.accentColor)));
        }
        return y + 16f;
    }

    /** Plain left aligned name and a short accent line. */
    private float drawMinimalHeader(String name, String headline, String contact) {
        float y = 24f;
        y += drawText(name, paint(headingBold, 22f, color(R.color.ink)), MARGIN, y, CONTENT_WIDTH, true) + 2f;
        if (!isEmpty(headline)) {
            y += drawText(headline, paint(body, 12f, color(R.color.teal_text)), MARGIN, y, CONTENT_WIDTH, true) + 2f;
        }
        if (!isEmpty(contact)) {
            y += drawText(contact, paint(body, 10f, color(R.color.muted)), MARGIN, y, CONTENT_WIDTH, true) + 2f;
        }
        y += 6f;
        if (canvas != null) {
            canvas.drawRect(MARGIN, y, MARGIN + 32f, y + 3f, fill(color(template.accentColor)));
        }
        return y + 18f;
    }

    // ---------- sections ----------

    private float drawSummary(Cv cv, float y) {
        if (isEmpty(cv.summary)) {
            return y;
        }
        y = drawSectionTitle("Summary", y);
        return y + drawText(cv.summary, paint(body, 11f, color(R.color.ink)), MARGIN, y, CONTENT_WIDTH, true) + 14f;
    }

    private float drawSkills(Cv cv, float y) {
        if (cv.skills == null || cv.skills.isEmpty()) {
            return y;
        }
        y = drawSectionTitle("Skills", y);
        if (template == CvTemplate.MODERN) {
            y += drawChips(cv.skills, y, new int[]{R.color.violet_tint}, new int[]{R.color.violet_text});
        } else {
            String separator = template == CvTemplate.CLASSIC ? "  ·  " : ", ";
            y += drawText(ListText.join(cv.skills, separator), paint(body, 11f, color(R.color.ink)),
                    MARGIN, y, CONTENT_WIDTH, true);
        }
        return y + 14f;
    }

    private float drawEducation(Cv cv, float y) {
        if (cv.education == null || cv.education.isEmpty()) {
            return y;
        }
        y = drawSectionTitle("Education", y);
        for (Cv.Education item : cv.education) {
            y += drawText(orBlank(item.school), paint(bodyBold, 11f, color(R.color.ink)), MARGIN, y, CONTENT_WIDTH, true);
            String line = ListText.join(nonEmpty(item.degree, item.years), "  ·  ");
            if (!line.isEmpty()) {
                y += drawText(line, paint(body, 10f, color(R.color.muted)), MARGIN, y, CONTENT_WIDTH, true);
            }
            y += 6f;
        }
        return y + 8f;
    }

    private float drawProjects(Cv cv, float y) {
        if (cv.projects == null || cv.projects.isEmpty()) {
            return y;
        }
        y = drawSectionTitle("Projects", y);
        for (Cv.Project project : cv.projects) {
            float nameHeight = drawText(orBlank(project.name), paint(bodyBold, 12f, color(R.color.ink)),
                    MARGIN, y, CONTENT_WIDTH, true);
            y += nameHeight;
            if (!isEmpty(project.description)) {
                y += drawText(project.description, paint(body, 10.5f, color(R.color.muted)),
                        MARGIN, y + 2f, CONTENT_WIDTH, true) + 2f;
            }
            if (project.tags != null && !project.tags.isEmpty()) {
                y += 4f;
                if (template == CvTemplate.MODERN) {
                    y += drawChips(project.tags, y,
                            new int[]{R.color.violet_tint, R.color.sky_tint, R.color.amber_tint},
                            new int[]{R.color.violet_text, R.color.sky_text, R.color.amber_text});
                } else {
                    y += drawText(ListText.join(project.tags, ", "), paint(body, 10f, color(template.textColor)),
                            MARGIN, y, CONTENT_WIDTH, true);
                }
            }
            y += 10f;
        }
        return y + 4f;
    }

    private float drawExperience(Cv cv, float y) {
        if (cv.experience == null || cv.experience.isEmpty()) {
            return y;
        }
        y = drawSectionTitle("Experience", y);
        for (Cv.Experience item : cv.experience) {
            y += drawText(ListText.join(nonEmpty(item.role, item.company), " · "),
                    paint(bodyBold, 11f, color(R.color.ink)), MARGIN, y, CONTENT_WIDTH, true);
            if (!isEmpty(item.years)) {
                y += drawText(item.years, paint(body, 10f, color(R.color.muted)), MARGIN, y, CONTENT_WIDTH, true);
            }
            if (item.bullets != null) {
                for (String bullet : item.bullets) {
                    y += drawText("•  " + bullet, paint(body, 10.5f, color(R.color.ink)),
                            MARGIN + 4f, y + 2f, CONTENT_WIDTH - 4f, true) + 2f;
                }
            }
            y += 8f;
        }
        return y + 6f;
    }

    private float drawCertificates(Cv cv, float y) {
        if (cv.certificates == null || cv.certificates.isEmpty()) {
            return y;
        }
        y = drawSectionTitle("Certificates", y);
        for (Cv.Certificate item : cv.certificates) {
            String text = ListText.join(nonEmpty(item.name, item.issuer, item.year), "  ·  ");
            y += drawText(text, paint(body, 10.5f, color(R.color.ink)), MARGIN, y, CONTENT_WIDTH, true) + 4f;
        }
        return y + 10f;
    }

    private float drawLinks(Cv cv, float y) {
        if (cv.links == null || cv.links.isEmpty()) {
            return y;
        }
        y = drawSectionTitle("Links", y);
        for (Cv.Link item : cv.links) {
            String text = ListText.join(nonEmpty(item.label, item.url), ":  ");
            y += drawText(text, paint(body, 10.5f, color(template.textColor)), MARGIN, y, CONTENT_WIDTH, true) + 4f;
        }
        return y + 6f;
    }

    // ---------- building blocks ----------

    private float drawSectionTitle(String title, float y) {
        String text = title.toUpperCase();
        switch (template) {
            case CLASSIC: {
                float h = drawText(text, paint(Typeface.SERIF, 11f, color(R.color.ink), true), MARGIN, y, CONTENT_WIDTH, true);
                if (canvas != null) {
                    canvas.drawRect(MARGIN, y + h + 1f, PAGE_WIDTH - MARGIN, y + h + 1.5f, fill(color(R.color.border)));
                }
                return y + h + 8f;
            }
            case MINIMAL:
                return y + drawText(text, paint(bodyBold, 9.5f, color(R.color.teal_text)), MARGIN, y, CONTENT_WIDTH, true) + 6f;
            default:
                return y + drawText(text, paint(headingBold, 10f, color(R.color.violet)), MARGIN, y, CONTENT_WIDTH, true) + 6f;
        }
    }

    /** Draws text that wraps inside the width and returns its height. */
    private float drawText(String text, TextPaint paint, float x, float y, float width, boolean draw) {
        if (isEmpty(text)) {
            return 0f;
        }
        StaticLayout layout = new StaticLayout(text, paint, (int) width, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false);
        if (draw && canvas != null) {
            canvas.save();
            canvas.translate(x, y);
            layout.draw(canvas);
            canvas.restore();
        }
        return layout.getHeight();
    }

    private float drawCentered(String text, TextPaint paint, float y) {
        StaticLayout layout = new StaticLayout(text, paint, (int) CONTENT_WIDTH, Layout.Alignment.ALIGN_CENTER, 1.15f, 0f, false);
        if (canvas != null) {
            canvas.save();
            canvas.translate(MARGIN, y);
            layout.draw(canvas);
            canvas.restore();
        }
        return layout.getHeight();
    }

    /** Rounded chips that flow to the next line when the row is full. Returns the height used. */
    private float drawChips(List<String> labels, float y, int[] backgrounds, int[] textColors) {
        TextPaint paint = paint(bodyBold, 9.5f, color(R.color.ink));
        float chipHeight = 18f;
        float gap = 5f;
        float x = MARGIN;
        float top = y;
        int index = 0;
        for (String label : labels) {
            float chipWidth = paint.measureText(label) + 16f;
            if (x + chipWidth > PAGE_WIDTH - MARGIN && x > MARGIN) {
                x = MARGIN;
                top += chipHeight + gap;
            }
            if (canvas != null) {
                int slot = index % backgrounds.length;
                canvas.drawRoundRect(new RectF(x, top, x + chipWidth, top + chipHeight), 9f, 9f,
                        fill(color(backgrounds[slot])));
                paint.setColor(color(textColors[slot]));
                canvas.drawText(label, x + 8f, top + 12.5f, paint);
            }
            x += chipWidth + gap;
            index++;
        }
        return top + chipHeight - y;
    }

    // ---------- small helpers ----------

    private TextPaint paint(Typeface typeface, float size, int color) {
        return paint(typeface, size, color, false);
    }

    private TextPaint paint(Typeface typeface, float size, int color, boolean bold) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(bold ? Typeface.create(typeface, Typeface.BOLD) : typeface);
        paint.setTextSize(size);
        paint.setColor(color);
        return paint;
    }

    private Paint fill(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        return paint;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static String orBlank(String text) {
        return text == null ? "" : text;
    }

    /** Keeps only the texts that are not empty. */
    private static List<String> nonEmpty(String... texts) {
        List<String> result = new java.util.ArrayList<>();
        for (String text : texts) {
            if (!isEmpty(text)) {
                result.add(text.trim());
            }
        }
        return result;
    }
}
