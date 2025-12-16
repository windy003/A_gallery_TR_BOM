package com.example.gallery2;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;
import androidx.appcompat.widget.AppCompatImageView;

public class TouchImageView extends AppCompatImageView {
    private Matrix matrix = new Matrix();
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF last = new PointF();
    private PointF start = new PointF();
    private float minScale = 1f;
    private float maxScale = 5f;
    private float[] m;

    private float redundantXSpace, redundantYSpace;
    private float width, height;
    private float saveScale = 1f;
    private float right, bottom, origWidth, origHeight, bmWidth, bmHeight;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        matrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            matrix.getValues(m);
            float x = m[Matrix.MTRANS_X];
            float y = m[Matrix.MTRANS_Y];
            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    last.set(event.getX(), event.getY());
                    start.set(last);
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    last.set(event.getX(), event.getY());
                    start.set(last);
                    mode = ZOOM;
                    // 禁止 ViewPager2 拦截触摸事件
                    disallowParentInterceptTouchEvent();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float deltaX = curr.x - last.x;
                        float deltaY = curr.y - last.y;
                        float scaleWidth = Math.round(origWidth * saveScale);
                        float scaleHeight = Math.round(origHeight * saveScale);

                        // 检查图片是否需要水平或垂直滚动
                        boolean needHorizontalScroll = scaleWidth > width;
                        boolean needVerticalScroll = scaleHeight > height;

                        if (needHorizontalScroll || needVerticalScroll) {
                            // 只有在需要水平滚动时才禁止ViewPager2拦截
                            if (needHorizontalScroll) {
                                disallowParentInterceptTouchEvent();
                            }

                            if (scaleWidth < width) {
                                deltaX = 0;
                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            } else if (scaleHeight < height) {
                                deltaY = 0;
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);
                            } else {
                                // 图片同时需要水平和垂直滚动
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);

                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }

                            matrix.postTranslate(deltaX, deltaY);
                            last.set(curr.x, curr.y);
                        }
                    } else if (mode == ZOOM) {
                        disallowParentInterceptTouchEvent();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mode = NONE;
                    allowParentInterceptTouchEvent();
                    int xDiff = (int) Math.abs(curr.x - start.x);
                    int yDiff = (int) Math.abs(curr.y - start.y);
                    if (xDiff < 3 && yDiff < 3)
                        performClick();
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    allowParentInterceptTouchEvent();
                    break;
            }

            setImageMatrix(matrix);
            return true;
        });
    }

    private void disallowParentInterceptTouchEvent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void allowParentInterceptTouchEvent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        // Fit width to screen, align to top
        float scale;
        float scaleX = width / bmWidth;
        scale = scaleX; // 使用宽度缩放比例
        matrix.setScale(scale, scale);

        // Align to top (no vertical centering)
        redundantYSpace = 0; // 顶部对齐，不需要垂直偏移
        redundantXSpace = 0; // 宽度已经完全填充，不需要水平偏移

        matrix.postTranslate(redundantXSpace, redundantYSpace);

        // 计算图片实际显示的尺寸
        origWidth = scale * bmWidth;
        origHeight = scale * bmHeight;
        right = origWidth * saveScale - width;
        bottom = origHeight * saveScale - height;

        setImageMatrix(matrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= scaleFactor;

            if (saveScale > maxScale) {
                saveScale = maxScale;
                scaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                scaleFactor = minScale / origScale;
            }

            right = width * saveScale - width - (2 * redundantXSpace * saveScale);
            bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);

            if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
                matrix.postScale(scaleFactor, scaleFactor, width / 2, height / 2);
                if (scaleFactor < 1) {
                    matrix.getValues(m);
                    float x = m[Matrix.MTRANS_X];
                    float y = m[Matrix.MTRANS_Y];

                    if (scaleFactor < 1) {
                        if (Math.round(origWidth * saveScale) < width) {
                            if (y < -bottom)
                                matrix.postTranslate(0, -(y + bottom));
                            else if (y > 0)
                                matrix.postTranslate(0, -y);
                        } else {
                            if (x < -right)
                                matrix.postTranslate(-(x + right), 0);
                            else if (x > 0)
                                matrix.postTranslate(-x, 0);
                        }
                    }
                }
            } else {
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                matrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];

                if (scaleFactor < 1) {
                    if (x < -right)
                        matrix.postTranslate(-(x + right), 0);
                    else if (x > 0)
                        matrix.postTranslate(-x, 0);

                    if (y < -bottom)
                        matrix.postTranslate(0, -(y + bottom));
                    else if (y > 0)
                        matrix.postTranslate(0, -y);
                }
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Double tap to zoom
            if (saveScale == 1f) {
                matrix.postScale(2f, 2f, e.getX(), e.getY());
                saveScale = 2f;
            } else {
                matrix.setScale(1f, 1f);
                matrix.postTranslate(redundantXSpace, redundantYSpace);
                saveScale = 1f;
            }

            setImageMatrix(matrix);
            invalidate();
            return true;
        }
    }

    public void setImageBitmap(int bmWidth, int bmHeight) {
        this.bmWidth = bmWidth;
        this.bmHeight = bmHeight;
    }
}
