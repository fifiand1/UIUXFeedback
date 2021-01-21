package com.github.wzf.uiuxfeedback;

import android.view.MotionEvent;

/**
 * Created by wzf on 2016/7/8.
 */
public interface SimpleListener {
    void onDown(MotionEvent event);
    void onDrag(MotionEvent event);
    void onDragEnd();
    void onModeChange(int mode);
}
