package com.example.cvforge.ui.cv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.cvforge.data.model.Cv;

/**
 * Shows the CV the way it will look in the PDF. It is as wide as its parent and as high as the
 * CV needs. Call setCv or setTemplate and the preview redraws.
 */
public class CvPreviewView extends View {

    private final CvRenderer renderer;
    private Cv cv = new Cv();
    private CvTemplate template = CvTemplate.MODERN;

    public CvPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        renderer = new CvRenderer(context);
        setBackgroundColor(Color.WHITE);
    }

    public void setCv(Cv cv) {
        this.cv = cv == null ? new Cv() : cv;
        refresh();
    }

    public void setTemplate(CvTemplate template) {
        this.template = template;
        refresh();
    }

    private void refresh() {
        requestLayout();
        invalidate();
    }

    /** One page unit equals this many pixels. */
    private float scale(int viewWidth) {
        return viewWidth / CvRenderer.PAGE_WIDTH;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float pageHeight = renderer.draw(null, cv, template);
        setMeasuredDimension(width, Math.round(pageHeight * scale(width)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float scale = scale(getWidth());
        canvas.scale(scale, scale);
        renderer.draw(canvas, cv, template);
    }
}
