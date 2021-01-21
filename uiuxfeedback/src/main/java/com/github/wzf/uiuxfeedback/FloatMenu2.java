package com.github.wzf.uiuxfeedback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.List;

public class FloatMenu2 {
    private static final String TAG = "ExToast";

    public static String TBCO_ID = "";
    public static String TBCO_URL = "";
    public static String TBCO_URL_PARAMS = "";
    public static String TBCO_TITLE = "提示";
    public static String TBCO_MESSAGE = "成功";
    public static String TBCO_OK = "ok";
    public static String TBCO_TITLE_F = "提示";
    public static String TBCO_MESSAGE_F = "失败";
    public static String TBCO_OK_F = "ok";
    public static String TBCO_PERMISSION_ALERT = "请在系统设置-应用权限管理中找到本应用，开启悬浮窗权限";


    private static Context mContext;
    public static Activity mActivity;
    private int animations = -1;
    private boolean isShow = false;

    private WindowManager mWM;
    private WindowManager.LayoutParams params = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsRecord = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsPlay = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsDone = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsCancel = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsMask = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsVideo = new WindowManager.LayoutParams();
    private WindowManager.LayoutParams paramsProgress = new WindowManager.LayoutParams();
    private View mView;
    private static RecordView recordView;
    private static ImageView cancelTextView;
    private View maskView;
    private ImageView playImageView, doneImageView;
    private VideoView videoView;
    private ProgressBar progressBar;

    private float mTouchStartX;
    private float mTouchStartY;
    private float x;
    private float y;

    public static int screenWidth, screenHeight, statusBarHeight;

    private Handler handler = new Handler();
    private ShakeListener shakeListener;
    private boolean firstShow = true;
    private boolean background = false;
    private static boolean recording = false;
    AlertDialog alertDialog;

    private static MediaProjectionManager mMediaProjectionManager;
    private static ScreenRecorder mRecorder;
    private static final int RECORD_REQUEST_CODE = 9527;
    public static final String CRASH_PATH = Environment.getExternalStorageDirectory().getPath() + "/tiny/log/";
    private static String recordFileName;


    public FloatMenu2(final Activity context, String url, String tbcoid) {
        TBCO_ID = tbcoid;
        TBCO_URL = url;
        mActivity = context;
        mContext = context.getApplicationContext();
        shakeListener = new ShakeListener(context);
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflate.inflate(R.layout.float_tips_layout, null);
        ((MaskContainer) mView).setSimpleListener(simpleListener);
        ((MaskContainer) mView).init();

        try {
            new Thread(new uploadCrash()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        mView.setOnTouchListener(this);
        shakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShakeStart() {

            }

            @Override
            public void onShake() {

                if (recording) {
                    return;
                }
                if (isShow) {
                    hide(true);
                    vibrate();
                } else {
                    if (background) {
                        return;
                    }
                    vibrate();
                    show(firstShow);
                }
            }

            @Override
            public void onShakeEnd() {
            }
        });
        shakeListener.resume();

        recordView = new RecordView(mContext);
        recordView.init();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        recordView.setRecordListener(new RecordListener() {
            @Override
            public void onRecordEnd() {

                if (mRecorder != null) {
                    mRecorder.quit();
                    mRecorder = null;
                }
                mWM.addView(maskView, paramsMask);
                mWM.addView(cancelTextView, paramsCancel);
                mWM.addView(playImageView, paramsPlay);
                mWM.addView(doneImageView, paramsDone);
                mWM.addView(videoView, paramsVideo);

                Uri uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/record/" + recordFileName));//record-720x1280-1470982268530.mp4
                //调用系统自带的播放器
//                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                intent.setDataAndType(uri, "video/mp4");
//                context.startActivity(intent);
                Log.v("URI:::::::::", uri.toString());
                videoView.setVideoURI(uri);
                videoView.start();
            }
        });

        int padding2 = (int) DensityUtil.dip2px(mContext, 10);
        cancelTextView = new ImageView(mContext);
        cancelTextView.setImageResource(R.drawable.cancel_selector);
        cancelTextView.setPadding(padding2, padding2, 0, 0);
//        cancelTextView.setText("取消");
//        cancelTextView.setTextColor(Color.WHITE);
//        int padding = (int) DensityUtil.dip2px(mContext, 20);
//        cancelTextView.setPadding(padding, padding, padding, padding);
        cancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRecord();
                show(true);
                try {
                    File finalFile = new File(Environment.getExternalStorageDirectory() + "/record/" + recordFileName);
                    finalFile.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        ((MaskContainer) mView).startEdit();
                    }
                }, 200);
            }
        });

        playImageView = new ImageView(mContext);
        playImageView.setImageResource(R.drawable.play_selector);
        playImageView.setPadding(padding2, 0, 0, padding2);
        playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                videoView.start();
            }
        });

        doneImageView = new ImageView(mContext);
        doneImageView.setPadding(0, 0, padding2, padding2);
        doneImageView.setImageResource(R.drawable.record_done_selector);
        doneImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final EditText editText = new EditText(mContext);
                editText.setHint("反馈意见");
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        Log.i(TAG, editText + "actionId:" + actionId + " event:" + event);
                        if (actionId == 0) {
                            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                            editText.clearFocus();
                            return true;
                        }
                        return false;
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("提交视频").setView(editText);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        mWM.addView(progressBar, paramsProgress);
                        new Thread() {
                            @Override
                            public void run() {
                                int result = -1;
                                try {

                                    String url = ((MaskContainer) mView).getInfos();
                                    String value = editText.getText().toString();
                                    try {
                                        value = URLEncoder.encode(value, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
//                                    Map<String, String> params = new HashMap<>();
//                                    params.put("bugDescription", value);
//                                    params.put("uploadType", "video");
                                    url += "&uploadType=video&bugDescription=" + value;
                                    Log.i(TAG, "url:" + url);
                                    File finalFile = new File(Environment.getExternalStorageDirectory() + "/record/" + recordFileName);
                                    Log.i(TAG, "file:" + finalFile);
                                    result = HttpUtil.uploadForm(null, null, finalFile, null, url, "video/mpeg4");

                                    Log.i(TAG, "result:" + result);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setCancelable(false);
                                if (result == 200) {
                                    builder.setTitle(FloatMenu2.TBCO_TITLE);
                                    builder.setMessage(FloatMenu2.TBCO_MESSAGE);
                                    builder.setPositiveButton(FloatMenu2.TBCO_OK, new AlertDialog.OnClickListener() {
                                        public void onClick(DialogInterface di, int i) {
                                            alertDialog.dismiss();
                                        }
                                    });

                                    try {
                                        File finalFile = new File(Environment.getExternalStorageDirectory() + "/record/" + recordFileName);
                                        finalFile.delete();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            resetRecord();
                                            show(true);
                                        }
                                    });
                                } else {

                                    try {
                                        mWM.removeView(progressBar);
                                    } catch (Exception ignored) {
                                    }
                                    builder.setTitle(FloatMenu2.TBCO_TITLE_F);
                                    builder.setMessage(FloatMenu2.TBCO_MESSAGE_F);
                                    builder.setPositiveButton(FloatMenu2.TBCO_OK_F, new AlertDialog.OnClickListener() {
                                        public void onClick(DialogInterface di, int i) {
                                            alertDialog.dismiss();
                                        }
                                    });
                                }
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        resetRecord();
                                        show(true);
                                        alertDialog = builder.create();
                                        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                        alertDialog.show();
                                    }
                                });
                            }
                        }.start();
                    }
                });
                alertDialog = builder.create();
                alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                WindowManager.LayoutParams attributes = alertDialog.getWindow().getAttributes();
                int y = (int) DensityUtil.dip2px(mContext, 100);
                attributes.y -= y;
                alertDialog.show();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editText.requestFocus();
                        InputMethodManager inputManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(editText, 0);
                    }
                }, 300);

            }
        });
        maskView = new View(mContext);
        maskView.setBackgroundColor(Color.parseColor("#77000000"));

        videoView = new VideoView(mContext);
        progressBar = new ProgressBar(mContext);
    }

    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private Runnable hideRunnable = new Runnable() {
//        @Override
//        public void run() {
//            hide();
//        }
//    };

    /**
     * Show the view for the specified duration.
     */
    public void show(boolean reset) {
        if (isShow) return;
//        TextView tv = (TextView)mView.findViewById(R.id.message);
//        tv.setText("悬浮窗");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean b = Settings.canDrawOverlays(mContext);
            if (!b) {
                String pkName = mContext.getPackageName();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + pkName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return;
            }
        }

        initTN();
        mWM.addView(mView, params);
        ((MaskContainer) mView).startBreath();
        isShow = true;
        firstShow = false;
        //判断duration，如果大于#LENGTH_ALWAYS 则设置消失时间
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
    public void hide(boolean clear) {
        if (!isShow) return;
        mWM.removeView(mView);
        resetRecord();
        if (clear) {
            ((MaskContainer) mView).reset();
        }

        isShow = false;
    }

    private void resetRecord() {

        recording = false;
        ((MaskContainer) mView).setMode(MaskContainer.OPERATION_MODE_RESET);
        try {
            mWM.removeView(recordView);
        } catch (Exception ignored) {
        }
        try {
            mWM.removeView(cancelTextView);
        } catch (Exception ignored) {
        }
        try {
            mWM.removeView(playImageView);
        } catch (Exception ignored) {
        }
        try {
            mWM.removeView(doneImageView);
        } catch (Exception ignored) {
        }
        try {
            mWM.removeView(maskView);
        } catch (Exception ignored) {
        }
        try {
            mWM.removeView(videoView);
        } catch (Exception ignored) {
        }
        try {
            mWM.removeView(progressBar);
        } catch (Exception ignored) {
        }
        if (mRecorder != null) {
            mRecorder.quit();
            mRecorder = null;
        }
    }

    private void initTN() {
        try {
            mWM = (WindowManager) mActivity.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            params.format = PixelFormat.TRANSLUCENT;

            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.LEFT | Gravity.TOP;


            paramsRecord.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsRecord.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsRecord.format = PixelFormat.TRANSLUCENT;

            paramsRecord.width = WindowManager.LayoutParams.WRAP_CONTENT;
            paramsRecord.height = WindowManager.LayoutParams.WRAP_CONTENT;
            paramsRecord.gravity = Gravity.CENTER_VERTICAL | Gravity.BOTTOM;


            int v = (int) DensityUtil.dip2px(mContext, 54.5f);
            paramsPlay.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsPlay.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsPlay.format = PixelFormat.TRANSLUCENT;

            paramsPlay.width = v;
            paramsPlay.height = v;
            paramsPlay.gravity = Gravity.LEFT | Gravity.BOTTOM;


            paramsDone.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsDone.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsDone.format = PixelFormat.TRANSLUCENT;

            paramsDone.width = v;
            paramsDone.height = v;
            paramsDone.gravity = Gravity.RIGHT | Gravity.BOTTOM;


            paramsCancel.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsCancel.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsCancel.format = PixelFormat.TRANSLUCENT;

            paramsCancel.width = v;
            paramsCancel.height = v;
            paramsCancel.gravity = Gravity.LEFT | Gravity.TOP;

            Point screenSize = DensityUtil.getScreenSize(mActivity);
            screenWidth = screenSize.x;
            screenHeight = screenSize.y;
            statusBarHeight = DensityUtil.getStatusBarHeight(mActivity);
//            params.width=screenWidth;
//            params.height=screenHeight;


            paramsMask.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsMask.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsMask.format = PixelFormat.TRANSLUCENT;

            paramsMask.width = screenWidth;
            paramsMask.height = screenHeight;


            int padding2 = (int) DensityUtil.dip2px(mContext, 90);
            paramsVideo.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsVideo.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsVideo.format = PixelFormat.TRANSLUCENT;

            paramsVideo.width = screenWidth - padding2;
            paramsVideo.height = screenHeight - padding2;
            paramsVideo.gravity = Gravity.CENTER_VERTICAL;


            paramsProgress.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            paramsProgress.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            paramsProgress.format = PixelFormat.TRANSLUCENT;

            paramsProgress.width = WindowManager.LayoutParams.WRAP_CONTENT;
            paramsProgress.height = WindowManager.LayoutParams.WRAP_CONTENT;
            paramsProgress.gravity = Gravity.CENTER;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkCanOpenVideoMP4Url(String videoUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(videoUrl), "video/mp4");

        List<ResolveInfo> resolveInfo = mContext.getPackageManager().queryIntentActivities(intent, 0);

        return (resolveInfo.size() > 0);
    }

    private void updateViewPosition() {
        //更新浮动窗口位置参数
        params.x = (int) (x - mTouchStartX);
        params.y = (int) (y - mTouchStartY);
        mWM.updateViewLayout(mView, params);  //刷新显示
    }

    private void updateViewPosition2() {

        try {
            //更新浮动窗口位置参数
            params.x = (int) (x - mTouchStartX);
            params.y = (int) (y - mTouchStartY - statusBarHeight);
            mWM.updateViewLayout(mView, params);  //刷新显示
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateViewPositionReset() {

        try {
            //更新浮动窗口位置参数
            params.x = 0;
            params.y = screenHeight;
            mWM.updateViewLayout(mView, params);  //刷新显示
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDoneInfo(String title, String message, String ok) {
        TBCO_TITLE = title;
        TBCO_MESSAGE = message;
        TBCO_OK = ok;
    }

    public void setFailureInfo(String title, String message, String ok) {
        TBCO_TITLE_F = title;
        TBCO_MESSAGE_F = message;
        TBCO_OK_F = ok;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public static void startRecord(int resultCode, Intent data) {
        MediaProjection mediaProjection = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        } else {
            return;
        }
        if (mediaProjection == null || !recording) {
            Log.e("@@", "media projection is null@" + Build.VERSION.SDK_INT);
            cancelTextView.callOnClick();
            return;
        }
        // video size
        final int width = 480;
        final int height = 854;

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "record");
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        String directory = mediaStorageDir.getAbsolutePath();

        recordFileName = "record-" + width + "x" + height + "-" + System.currentTimeMillis() + ".mp4";
        File file = new File(directory, recordFileName);
        final int bitrate = 1500000;
        mRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
        mRecorder.start();
        recordView.startRecord();
    }

    private SimpleListener simpleListener = new SimpleListener() {
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
                    } else {
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
                    ((MaskContainer) mView).startBreath();
                    break;
                case MaskContainer.OPERATION_MODE_RECORD:
                    hide(true);
                    recording = true;
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    layoutParams.topMargin = 0;
                    layoutParams.leftMargin = 0;
                    firstShow = true;
                    recordView.init();
                    mWM.addView(recordView, paramsRecord);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                        try {
                            Class<?> threadClazz = Class.forName("com.github.tiny.frame.TinyFrame");
                            Method method = threadClazz.getMethod("getTopActivity");
                            Activity activity = (Activity) method.invoke(null);
                            activity.startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };

    class uploadCrash implements Runnable{

        @Override
        public void run() {

            File dir = new File(CRASH_PATH);
            if (dir.exists()) {
                String[] list = dir.list();
                if (list != null) {
                    for (String name : list) {
                        int result = -1;
                        String url = ((MaskContainer) mView).getInfos();
                        url += "&uploadType=log";
                        Log.i(TAG, "url:" + url);
                        File finalFile = new File(CRASH_PATH,name);
                        Log.i(TAG, "file:" + finalFile);
                        try {

//                            result = HttpUtil.uploadForm(null, null, finalFile, null, url, "text/plain");
//                            Log.i(TAG, "result:" + result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (result == 200) {
                            boolean delete = finalFile.delete();
                            Log.i(TAG, "delete crash log:" + delete);
                        }
                    }
                }

            }
        }
    }
}
