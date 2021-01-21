/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.wzf.uiuxfeedback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;

/**
 * @author Administrator
 */
public class DensityUtil {
    public static float dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        //System.out.println(scale+" "+(dpValue * scale + 0.5f)+" "+(int) (dpValue * scale + 0.5f));
        //return (int) (dpValue * scale + 0.5f);  
        return (dpValue * scale);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static float px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        //return (int) (pxValue / scale + 0.5f);  
        return (pxValue / scale);
    }


    public static Point getScreenSize(Context context) {
        Display disp = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point outP = new Point();
        disp.getSize(outP);
        return outP;
    }

    public static Rect getAppSize(Context context) {
        Rect outRect = new Rect();
        ((Activity) context).getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect);
        return outRect;
    }

    public static int getStatusHeight(Context context) {
        Rect appSize = getAppSize(context);
        Point screenSize = getScreenSize(context);
        return screenSize.y - appSize.height();
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
