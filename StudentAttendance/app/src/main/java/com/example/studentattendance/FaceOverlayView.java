
package com.example.studentattendance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FaceOverlayView extends View {

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Rect> faceBounds = new ArrayList<>();

    private int imageWidth = 0;
    private int imageHeight = 0;
    private boolean mirrorHorizontally = true;

    public FaceOverlayView(Context context) {
        super(context);
        initialize();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FaceOverlayView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        boxPaint.setColor(0xFF00E676);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);

        textPaint.setColor(0xFF00E676);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);

        setWillNotDraw(false);
    }

    public void updateFaces(
            @NonNull List<Rect> bounds,
            int width,
            int height,
            boolean mirror
    ) {
        faceBounds.clear();

        for (Rect bound : bounds) {
            faceBounds.add(new Rect(bound));
        }

        imageWidth = width;
        imageHeight = height;
        mirrorHorizontally = mirror;

        postInvalidate();
    }

    public void clearFaces() {
        faceBounds.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        float scale = Math.max(
                getWidth() / (float) imageWidth,
                getHeight() / (float) imageHeight
        );

        float offsetX =
                (getWidth() - imageWidth * scale) / 2f;

        float offsetY =
                (getHeight() - imageHeight * scale) / 2f;

        for (Rect face : faceBounds) {
            float left = face.left;
            float right = face.right;

            if (mirrorHorizontally) {
                left = imageWidth - face.right;
                right = imageWidth - face.left;
            }

            float mappedLeft = left * scale + offsetX;
            float mappedTop = face.top * scale + offsetY;
            float mappedRight = right * scale + offsetX;
            float mappedBottom = face.bottom * scale + offsetY;

            canvas.drawRect(
                    mappedLeft,
                    mappedTop,
                    mappedRight,
                    mappedBottom,
                    boxPaint
            );

            canvas.drawText(
                    "Face detected",
                    mappedLeft,
                    Math.max(40f, mappedTop - 12f),
                    textPaint
            );
        }
    }
}
