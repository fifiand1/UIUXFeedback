package com.github.wzf.uiuxfeedback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FloatMenu {
    private static final String TAG = "ExToast";

    public static final int LENGTH_ALWAYS = 0;
    public static final int LENGTH_SHORT = 2;
    public static final int LENGTH_LONG = 4;

    public static String TBCO_ID = "";
    public static String TBCO_URL = "";
    public static String TBCO_TITLE = "";
    public static String TBCO_MESSAGE = "";
    public static String TBCO_OK = "";


    private Toast toast;
    private Context mContext;
    public static Activity mActivity;
    private int mDuration = LENGTH_SHORT;
    private int animations = -1;
    private boolean isShow = false;

    private Object mTN;
    private Method show;
    private Method hide;
    private WindowManager mWM;
    private WindowManager.LayoutParams params;
    private View mView;

    private float mTouchStartX;
    private float mTouchStartY;
    private float x;
    private float y;

    public static int screenWidth, screenHeight, statusBarHeight;

    private Handler handler = new Handler();
    private ShakeListener shakeListener;

    public FloatMenu(Activity context, String url, String tbcoid) {
        TBCO_ID = tbcoid;
        TBCO_URL = url;
        mActivity = context;
        this.mContext = context.getApplicationContext();
        shakeListener = new ShakeListener(context);
        mDuration = LENGTH_ALWAYS;
//        this.animations=R.style.anim_view;
        if (toast == null) {
            toast = new Toast(mContext);
        }
        LayoutInflater inflate = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflate.inflate(R.layout.float_tips_layout, null);
        ((MaskContainer) mView).setSimpleListener(new SimpleListener() {
            @Override
            public void onDown(MotionEvent event) {

                //获取相对屏幕的坐标，即以屏幕左上角为原点
                x = event.getRawX();
                y = event.getRawY();
                Log.d("currP", "currX" + x + "====currY" + y);
                //获取相对View的坐标，即以此View左上角为原点
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                Log.d("startP", "startX" + mTouchStartX + "====startY" + mTouchStartY);
            }

            @Override
            public void onDrag(MotionEvent event) {

                //获取相对屏幕的坐标，即以屏幕左上角为原点
                x = event.getRawX();
                y = event.getRawY();
                Log.d("currP", "currX" + x + "====currY" + y);
                updateViewPosition();
            }

            @Override
            public void onDragEnd() {

//                if (x > screenWidth / 2) {
//                    x = screenWidth;
//                } else {
                    x = 0;
//                }
                updateViewPosition();
            }

            @Override
            public void onModeChange(int mode) {
                View viewById = mView.findViewById(R.id.tools_container);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewById.getLayoutParams();
                switch (mode) {
                    case MaskContainer.OPERATION_MODE_START_EDIT:
                        Log.i("currP", "OPERATION_MODE_START_EDIT paramsX" + params.width + "====paramsY" + params.height);
                        params.width = screenWidth;
                        params.height = (screenHeight - statusBarHeight);
                        updateViewPosition();
                        layoutParams.topMargin = params.y - statusBarHeight;
//                        View tools = mView.findViewById(R.id.layout_tools);
//                        View open = mView.findViewById(R.id.button_open);
//                        RelativeLayout.LayoutParams toolsLayoutParams = (RelativeLayout.LayoutParams) tools.getLayoutParams();
//                        RelativeLayout.LayoutParams openLayoutParams = (RelativeLayout.LayoutParams) open.getLayoutParams();
                        layoutParams.leftMargin = params.x;

//                        int v = (int) DensityUtil.dip2px(mContext, 8);
//                        int toolsLeft = (int) DensityUtil.dip2px(mContext, 48);
//                        int openLeft = (int) DensityUtil.dip2px(mContext, 160);
                        if (x == 0) {

                            layoutParams.leftMargin = (int) (params.x + mTouchStartX);
//                            openLayoutParams.setMargins(v,v,v,v);
//                            toolsLayoutParams.setMarginStart(toolsLeft);
//                            layoutParams2.rightMargin=0;
//                            layoutParams2.removeRule(RelativeLayout.END_OF);
//                            layoutParams2.removeRule(RelativeLayout.START_OF);
//                            layoutParams2.addRule(RelativeLayout.RIGHT_OF,R.id.button_open);
                        }else{
//
//                            layoutParams.leftMargin = (int) (params.x - mTouchStartX);
//                            openLayoutParams.setMargins(0,v,v,v);
//                            toolsLayoutParams.setMarginStart(0);
////                            layoutParams2.rightMargin=500;
////                            layoutParams2.removeRule(RelativeLayout.RIGHT_OF);
////                            layoutParams2.addRule(RelativeLayout.LEFT_OF,R.id.button_open);
                        }


                        break;

                    case MaskContainer.OPERATION_MODE_RESET:
                        Log.i("currP", "OPERATION_MODE_RESET paramsX" + params.width + "====paramsY" + params.height);
                        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        updateViewPosition2();
                        layoutParams.topMargin = 0;
                        layoutParams.leftMargin = 0;
                        break;
                }
            }
        });
        ((MaskContainer) mView).init();
//        mView.setOnTouchListener(this);
        shakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShakeStart() {

            }

            @Override
            public void onShake() {

                if (isShow) {
                    hide();
                } else {
                    show(false);
                }
            }

            @Override
            public void onShakeEnd() {
            }
        });
        shakeListener.resume();
    }

    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Show the view for the specified duration.
     */
    public void show(boolean reset) {
        if (isShow) return;
//        TextView tv = (TextView)mView.findViewById(R.id.message);
//        tv.setText("悬浮窗");
        toast.setView(mView);
        initTN();
        try {
            show.invoke(mTN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isShow = true;
        //判断duration，如果大于#LENGTH_ALWAYS 则设置消失时间
        if (mDuration > LENGTH_ALWAYS) {
            handler.postDelayed(hideRunnable, mDuration * 1000);
        }
        if (reset) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    updateViewPositionReset();
                }
            }, 100);
        }
    }

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet.
     * You do not normally have to call this.  Normally view will disappear on its own
     * after the appropriate duration.
     */
    public void hide() {
        if (!isShow) return;
        try {
            hide.invoke(mTN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isShow = false;
    }

    public void setView(View view) {
        toast.setView(view);
    }

    public View getView() {
        return toast.getView();
    }

    /**
     * Set how long to show the view for.
     *
     * @see #LENGTH_SHORT
     * @see #LENGTH_LONG
     * @see #LENGTH_ALWAYS
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setMargin(float horizontalMargin, float verticalMargin) {
        toast.setMargin(horizontalMargin, verticalMargin);
    }

    public float getHorizontalMargin() {
        return toast.getHorizontalMargin();
    }

    public float getVerticalMargin() {
        return toast.getVerticalMargin();
    }

    public void setGravity(int gravity, int xOffset, int yOffset) {
        toast.setGravity(gravity, xOffset, yOffset);
    }

    public int getGravity() {
        return toast.getGravity();
    }

    public int getXOffset() {
        return toast.getXOffset();
    }

    public int getYOffset() {
        return toast.getYOffset();
    }

//    public static FloatMenu makeText(Context context, CharSequence text, int duration) {
//        Toast toast = Toast.makeText(context,text,Toast.LENGTH_SHORT);
//        FloatMenu exToast = new FloatMenu(context);
//        exToast.toast = toast;
//        exToast.mDuration = duration;
//
//        return exToast;
//    }

//    public static FloatMenu makeText(Context context, int resId, int duration)
//            throws Resources.NotFoundException {
//        return makeText(context, context.getResources().getText(resId), duration);
//    }

    public void setText(int resId) {
        setText(mContext.getText(resId));
    }

    public void setText(CharSequence s) {
        toast.setText(s);
    }

    public int getAnimations() {
        return animations;
    }

    public void setAnimations(int animations) {
        this.animations = animations;
    }

    private void initTN() {
        try {
            Field tnField = toast.getClass().getDeclaredField("mTN");
            tnField.setAccessible(true);
            mTN = tnField.get(toast);
            show = mTN.getClass().getMethod("show");
            hide = mTN.getClass().getMethod("hide");

            Field tnParamsField = mTN.getClass().getDeclaredField("mParams");
            tnParamsField.setAccessible(true);
            params = (WindowManager.LayoutParams) tnParamsField.get(mTN);
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
//                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            /**设置动画*/
            if (animations != -1) {
                params.windowAnimations = animations;
            }

            /**调用tn.show()之前一定要先设置mNextView*/
            Field tnNextViewField = mTN.getClass().getDeclaredField("mNextView");
            tnNextViewField.setAccessible(true);
            tnNextViewField.set(mTN, toast.getView());

            mWM = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Point screenSize = DensityUtil.getScreenSize(mActivity);
            screenWidth = screenSize.x;
            screenHeight = screenSize.y;
            statusBarHeight = DensityUtil.getStatusBarHeight(mActivity);
//            params.width=screenWidth;
//            params.height=screenHeight;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setGravity(Gravity.LEFT | Gravity.TOP, 0, 0);
    }

    private void updateViewPosition() {
        //更新浮动窗口位置参数
        params.x = (int) (x - mTouchStartX);
        params.y = (int) (y - mTouchStartY);
        mWM.updateViewLayout(toast.getView(), params);  //刷新显示
    }

    private void updateViewPosition2() {
        //更新浮动窗口位置参数
        params.x = (int) (x - mTouchStartX);
        params.y = (int) (y - mTouchStartY - statusBarHeight);
        mWM.updateViewLayout(toast.getView(), params);  //刷新显示
    }

    private void updateViewPositionReset() {
        //更新浮动窗口位置参数
//        mWM.removeView(toast.getView());
//        initTN();
        params.x = 0;
        params.y = screenHeight;
        mWM.updateViewLayout(toast.getView(), params);  //刷新显示
//        mWM.addView(toast.getView(), params);
//
//        try {
//            show.invoke(mTN);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public void setDoneInfo(String title, String message, String ok) {
        TBCO_TITLE = title;
        TBCO_MESSAGE = message;
        TBCO_OK = ok;
    }

//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        //获取相对屏幕的坐标，即以屏幕左上角为原点
//        x = event.getRawX();
//        y = event.getRawY();
//        Log.i("currP", "currX"+x+"====currY"+y);
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:    //捕获手指触摸按下动作
//                //获取相对View的坐标，即以此View左上角为原点
//                mTouchStartX =  event.getX();
//                mTouchStartY =  event.getY();
//                Log.i("startP","startX"+mTouchStartX+"====startY"+mTouchStartY);
//                break;
//            case MotionEvent.ACTION_MOVE:   //捕获手指触摸移动动作
//                updateViewPosition();
//                break;
//            case MotionEvent.ACTION_UP:    //捕获手指触摸离开动作
//                updateViewPosition();
//                break;
//        }
//        return true;
//    }


}
