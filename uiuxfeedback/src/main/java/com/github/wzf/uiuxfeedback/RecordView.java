package com.github.wzf.uiuxfeedback;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author wzf
 * @date 2016/8/11
 */
public class RecordView extends View {
    private static final String TAG = "RecordView";
    private static final int DURATION = 8000;

    private RecordListener recordListener;
    private GestureDetector mGestureListener;
    Paint paint = new Paint();
    RectF r1 = new RectF();
    RectF r2 = new RectF();
    private int length, padding, borderWidth, fontSize, bianchang;
    ValueAnimator circleAnimator;
    float angle = 0;
    int played = 0;
    private boolean end = false, force = false;

    public RecordView(Context context) {
        super(context);
        length = (int) DensityUtil.dip2px(getContext(), 50);
        bianchang = (int) DensityUtil.dip2px(getContext(), 10);
        padding = (int) DensityUtil.dip2px(getContext(), 4);
        borderWidth = (int) DensityUtil.dip2px(getContext(), 2);
        fontSize = (int) DensityUtil.dip2px(getContext(), 10);
    }

    public void init() {
        end = false;
        played = 0;
        if (mGestureListener == null) {
            mGestureListener = new GestureDetector(getContext(), new GestureListener());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(length + padding * 2 + fontSize * 2, length + padding * 2 + fontSize * 2);
    }

    public static int getTextWidth(Paint paint, String str) {
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int measuredHeight = this.getMeasuredHeight();
        int measuredWidth = this.getMeasuredWidth();

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        int measuredWidth2 = measuredWidth / 2;
        canvas.drawCircle(measuredWidth2, measuredHeight / 2, length / 2, paint);

        paint.setColor(Color.parseColor("#24b4f1"));
        if (end) {
            paint.setTextSize(fontSize);
            String str = "已录完";
            int textWidth = getTextWidth(paint, str);
            canvas.drawText(str, measuredWidth2 - textWidth / 2, measuredHeight / 2 + fontSize / 2, paint);
        } else {
            r1.left = measuredWidth2 - bianchang;
            r1.top = measuredWidth2 - bianchang;
            r1.right = measuredWidth2 + bianchang;
            r1.bottom = measuredWidth2 + bianchang;
            canvas.drawRoundRect(r1, 3, 3, paint);


            if (played > 0) {
                paint.setTextSize(fontSize);
                String str = played + "秒";
                int textWidth = getTextWidth(paint, str);
                canvas.drawText(str, measuredWidth2 - textWidth / 2, fontSize, paint);
            }
        }


        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);


        r2.left = padding + fontSize;
        r2.top = padding + fontSize;
        r2.right = length + padding + fontSize;
        r2.bottom = length + padding + fontSize;
        canvas.drawArc(r2, -90, angle, false, paint);
//        canvas.drawCircle(measuredWidth / 2, measuredHeight / 2, length / 2, paint);
    }

    public void startRecord() {
        circleAnimator = ValueAnimator.ofFloat(0, 360);
        circleAnimator.setDuration(DURATION);
        circleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                played = (int) (valueAnimator.getCurrentPlayTime() / 1000);
                angle = (Float) valueAnimator.getAnimatedValue();
                RecordView.this.invalidate();
            }
        });
        circleAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                end = true;
                angle = 360;
                if (!force) {
                    played = 8;
                }
                RecordView.this.invalidate();
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recordListener.onRecordEnd();
                    }
                }, 200);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        circleAnimator.start();
    }

    public void setRecordListener(RecordListener recordListener) {
        this.recordListener = recordListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureListener.onTouchEvent(event);
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (circleAnimator != null) {
                force = true;
                circleAnimator.cancel();
            }
            return super.onSingleTapUp(e);
        }
    }
}
