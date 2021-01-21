package com.github.wzf.uiuxfeedback;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wzf on 2016/7/8.
 */
public class MaskContainer extends RelativeLayout {
    public static final int OPERATION_MODE_RESET = -1;
    public static final int OPERATION_MODE_START_EDIT = 0;
    public static final int OPERATION_MODE_DRAW = 1;
    public static final int OPERATION_MODE_TYPING = 2;
    public static final int OPERATION_MODE_SELECTED = 3;
    public static final int OPERATION_MODE_RECORD = 4;
    private static final String TAG = "MaskContainer";
    private int mMode = OPERATION_MODE_RESET;

    private float mX, mY;
    private float lastX, lastY;
    private List<Path> paths = new ArrayList<Path>();
    private List<float[]> maxMinPoints = new ArrayList<float[]>();//maxX maxY minX minY
    private int otherLine = -1;

    private List<RelativeLayout> texts = new ArrayList<RelativeLayout>();
    private List<ImageView> textsCloses = new ArrayList<ImageView>();
    private RelativeLayout dragging;

    private Stack<Integer> history = new Stack<>();

    private final Paint mGesturePaint = new Paint();
    private final Paint rectPaint = new Paint();
    private SimpleListener simpleListener;
    private GestureDetector mGestureListener;
    private ImageView openButton, drawButton, textButton, confirmButton, undoButton,recordButton,gqImageView;
    private LinearLayout toolsLayout;
    private RelativeLayout progressBar;
    private Bitmap redClose;
    private int mTouchSlop;
    private int textPadding;
    private AlphaAnimation animation;
    private int alphaDuration = 2500;
    private float startAlpha = 0.25f;

    AlertDialog alertDialog;


    public MaskContainer(Context context) {
        super(context);
    }

    public MaskContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaskContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init() {
        mGesturePaint.setAntiAlias(true);
        mGesturePaint.setStyle(Paint.Style.STROKE);
        int width = (int) DensityUtil.dip2px(getContext(), 3);
        mGesturePaint.setStrokeWidth(width);
        mGesturePaint.setColor(Color.RED);

        rectPaint.setAntiAlias(true);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(1);
        rectPaint.setColor(Color.RED);

        mTouchSlop = (int) DensityUtil.dip2px(getContext(), 8);

        textPadding = (int) DensityUtil.dip2px(getContext(), 16);

        redClose = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.ps04);

        progressBar = (RelativeLayout) MaskContainer.this.findViewById(R.id.progressBarPanel);
        progressBar.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        gqImageView = (ImageView) findViewById(R.id.image_gq);
//        /** 设置透明度渐变动画 */
//        animation = new AlphaAnimation(startAlpha, 1.0f);
//        animation.setDuration(alphaDuration);//设置动画持续时间
//        /** 常用方法 */
//        animation.setRepeatCount(Animation.INFINITE);//设置重复次数
//        animation.setRepeatMode(Animation.REVERSE);
//        gqImageView.setAnimation(animation);
//        animation.start();

        openButton = (ImageView) findViewById(R.id.button_open);
        openButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i(TAG, "click open");

                switch (mMode) {
                    case OPERATION_MODE_RESET:
                        openButton.setImageResource(R.drawable.ps01);
                        toolsLayout.setVisibility(VISIBLE);
                        mMode = OPERATION_MODE_START_EDIT;
                        stopBreath();
                        MaskContainer.this.setBackgroundColor(Color.parseColor("#55000000"));
                        simpleListener.onModeChange(mMode);
                        drawButton.callOnClick();
                        break;

                    case OPERATION_MODE_START_EDIT:
                    case OPERATION_MODE_DRAW:
                    case OPERATION_MODE_TYPING:
                        resetDrawables();
                        mMode = OPERATION_MODE_RESET;
                        simpleListener.onModeChange(mMode);
                        break;
                }
            }
        });
        drawButton = (ImageView) findViewById(R.id.button_draw);
        drawButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mMode = OPERATION_MODE_DRAW;
                drawButton.setImageResource(R.drawable.ps06_f);
                textButton.setImageResource(R.drawable.ps08);
                for (RelativeLayout text : texts) {
                    EditText editText = (EditText) text.getChildAt(0);
                    editText.clearFocus();
                    editText.setEnabled(false);
                }
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(MaskContainer.this.getWindowToken(), 0);
            }
        });
        textButton = (ImageView) findViewById(R.id.button_text);
        textButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mMode = OPERATION_MODE_TYPING;
                otherLine = -1;
                invalidate();
                drawButton.setImageResource(R.drawable.ps06);
                textButton.setImageResource(R.drawable.ps08_f);
                for (RelativeLayout text : texts) {
                    EditText editText = (EditText) text.getChildAt(0);
                    editText.setEnabled(true);
                }
                int x = (int) DensityUtil.dip2px(getContext(), 80);
                int y = (int) DensityUtil.dip2px(getContext(), 50);
                addText(x, y);
            }
        });
        recordButton = (ImageView) findViewById(R.id.button_record);
        recordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mMode = OPERATION_MODE_RECORD;
                resetDrawables();
                simpleListener.onModeChange(mMode);
            }
        });
        undoButton = (ImageView) findViewById(R.id.button_undo);
        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (history.size() > 0) {

                    int pop = history.pop();
                    if (pop == OPERATION_MODE_DRAW) {
                        paths.remove(paths.size() - 1);
                        maxMinPoints.remove(maxMinPoints.size() - 1);
                        otherLine = -1;
                        invalidate();
                    } else if (pop == OPERATION_MODE_TYPING) {
                        RelativeLayout relativeLayout = texts.get(texts.size() - 1);
                        ImageView imageView = textsCloses.get(textsCloses.size() - 1);
                        EditText editText = (EditText) relativeLayout.getChildAt(0);
                        editText.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        MaskContainer.this.removeView(relativeLayout);
                        MaskContainer.this.removeView(imageView);
                        texts.remove(relativeLayout);
                        textsCloses.remove(imageView);

                    }
                }
            }
        });

        confirmButton = (ImageView) findViewById(R.id.button_confirm);
        confirmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (history.size() <= 0) {
                    openButton.callOnClick();
                    return;
                }

                progressBar.setVisibility(VISIBLE);
                File file = null;
                try {
                    Class<?> threadClazz = Class.forName("com.github.tiny.frame.TinyFrame");
                    Method method = threadClazz.getMethod("getScreenShot");
                    Bitmap invoke = (Bitmap) method.invoke(null);
                    Canvas canvas = new Canvas(invoke);

                    for (int i = 0; i < paths.size(); i++) {
                        Path path = paths.get(i);
                        canvas.drawPath(path, mGesturePaint);
                    }

                    for (RelativeLayout text : texts) {
                        LayoutParams layoutParams = (LayoutParams) text.getLayoutParams();
                        EditText editText = (EditText) text.getChildAt(0);


                        String aboutTheGame = String.valueOf(editText.getText());
                        StaticLayout layout = new StaticLayout(aboutTheGame, editText.getPaint(), (int) DensityUtil.dip2px(getContext(), 200), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);

                        canvas.save();
                        canvas.translate(layoutParams.leftMargin + textPadding, layoutParams.topMargin + textPadding);//从20，20开始画
                        layout.draw(canvas);
                        canvas.restore();//别忘了restore


//                        canvas.drawText(String.valueOf(editText.getText()), layoutParams.leftMargin, layoutParams.topMargin + FloatMenu.statusBarHeight, editText.getPaint());
                    }
                    // TODO: 2018/10/8 check save(int)
//                    canvas.save(Canvas.ALL_SAVE_FLAG);
                    canvas.save();
                    canvas.restore();
                    file = saveImage(invoke);

                } catch (Exception e) {
                    e.printStackTrace();
                }



                final File finalFile = file;
                new Thread() {
                    @Override
                    public void run() {


                        int result=-1;
                        try {
                            String info = getInfos();
                            String url = info + "&uploadType=pic";
//                            String file = "/storage/emulated/0/DCIM/Screenshots/home.png";
                            Log.i(TAG, "url:" + url);
                            Log.i(TAG, "file:" + finalFile);
                            result = HttpUtil.uploadForm(null, null, finalFile, null, url, "image/jpeg");

                            Log.i(TAG, "result:" + result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (result == 200) {

                            MaskContainer.this.getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    toolsLayout.setVisibility(GONE);
                                    resetDrawables();
                                    mMode = OPERATION_MODE_RESET;
                                    MaskContainer.this.setBackgroundColor(0);
                                    simpleListener.onModeChange(mMode);
                                    progressBar.setVisibility(GONE);
                                    Log.i(TAG, "done");

                                    AlertDialog.Builder builder = new AlertDialog.Builder(MaskContainer.this.getContext());
                                    builder.setTitle(FloatMenu2.TBCO_TITLE);
                                    builder.setMessage(FloatMenu2.TBCO_MESSAGE);
                                    builder.setPositiveButton(FloatMenu2.TBCO_OK, new AlertDialog.OnClickListener() {
                                        public void onClick(DialogInterface di, int i) {
                                            alertDialog.dismiss();
                                        }
                                    });
                                    builder.setCancelable(false);
                                    alertDialog = builder.create();
                                    alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                    alertDialog.show();
                                }
                            });
                        } else {

                            MaskContainer.this.post(new Runnable() {
                                @Override
                                public void run() {

                                    progressBar.setVisibility(GONE);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MaskContainer.this.getContext());
                                    builder.setTitle(FloatMenu2.TBCO_TITLE_F);
                                    builder.setMessage(FloatMenu2.TBCO_MESSAGE_F);
                                    builder.setPositiveButton(FloatMenu2.TBCO_OK_F, new AlertDialog.OnClickListener() {
                                        public void onClick(DialogInterface di, int i) {
                                            alertDialog.dismiss();
                                        }
                                    });
                                    builder.setCancelable(false);
                                    alertDialog = builder.create();
                                    alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                    alertDialog.show();
                                }
                            });
                        }
                    }
                }.start();

            }
        });
        toolsLayout = (LinearLayout) findViewById(R.id.layout_tools);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            recordButton.setVisibility(VISIBLE);
            ViewGroup.LayoutParams layoutParams = toolsLayout.getLayoutParams();
            layoutParams.width += DensityUtil.dip2px(getContext(), 47);
        }
        mGestureListener = new GestureDetector(getContext(), new GestureListener());
    }

    public String getInfos() {

        String model = Build.MODEL;
        String version = Build.VERSION.RELEASE;
        try {
            model = URLEncoder.encode(model, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = FloatMenu2.TBCO_URL + "?TBCOPId=" + FloatMenu2.TBCO_ID + "&phoneBrand=" + model + "&systemVersion=Android&systemVersionNo=" + version;

        String appVersionNo = "";
        try {
            PackageManager pm = getContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
            appVersionNo = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        url += "&appVersionNo=" + appVersionNo;
        url += "&" + FloatMenu2.TBCO_URL_PARAMS;
        return url;
    }

    public void startBreath() {
        /** 设置透明度渐变动画 */
        animation = new AlphaAnimation(startAlpha, 1.0f);
        animation.setDuration(alphaDuration);//设置动画持续时间
        /** 常用方法 */
        animation.setRepeatCount(Animation.INFINITE);//设置重复次数
        animation.setRepeatMode(Animation.REVERSE);
        gqImageView.setAnimation(animation);
        gqImageView.setVisibility(VISIBLE);
        animation.start();
    }

    public void stopBreath() {
        if (animation != null) {
            animation.reset();
            animation.cancel();
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                gqImageView.setAnimation(null);
                gqImageView.setVisibility(GONE);
            }
        }, 200);
    }

    private File saveImage(Bitmap source) {
        File file = null;
        OutputStream outputStream = null;
        String timestamp = System.currentTimeMillis() + "";
        File mediaStorageDir = new File(getContext().getExternalCacheDir(), "tiny");
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
//        File file = new File(mediaStorageDir.getAbsolutePath() + "/screenshot" + timestamp + ".jpg");
        String directory = mediaStorageDir.getAbsolutePath();
        String filename = "screenshot" + timestamp + ".jpg";
        try {
            file = new File(directory, filename);
            if (file.createNewFile()) {
                outputStream = new FileOutputStream(file);
                source = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight());
                source.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable t) {
                }
            }
        }
        return file;
    }

    public void startEdit() {
        openButton.callOnClick();
    }
    public void reset() {
        Log.i(TAG, "reset");
        switch (mMode) {
            case OPERATION_MODE_START_EDIT:
            case OPERATION_MODE_DRAW:
            case OPERATION_MODE_TYPING:
                resetDrawables();
                mMode = OPERATION_MODE_RESET;
                simpleListener.onModeChange(mMode);
                break;
        }
    }

    private void resetDrawables() {
        openButton.setImageResource(R.drawable.ps02);
        toolsLayout.setVisibility(GONE);
        MaskContainer.this.setBackgroundColor(0);
        paths.clear();
        maxMinPoints.clear();
        history.clear();
        for (RelativeLayout text : texts) {
            try {
                this.removeView(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (ImageView imageView : textsCloses) {
            this.removeView(imageView);
        }
        textsCloses.clear();
        texts.clear();
        dragging = null;
        otherLine = -1;
    }


    public void setMode(int mode) {
        this.mMode = mode;
    }

    public int getMode() {
        return mMode;
    }

    public void setSimpleListener(SimpleListener dragListener) {
        this.simpleListener = dragListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (mMode) {
            case OPERATION_MODE_RESET:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    simpleListener.onDragEnd();
                }
                return mGestureListener.onTouchEvent(event);
            case OPERATION_MODE_DRAW:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
//                        otherLine = -1;
//                        for (int i = 0; i < maxMinPoints.size(); i++) {
//                            float[] floats = maxMinPoints.get(i);
//                            Rect rect = new Rect((int) floats[2] - mTouchSlop, (int) floats[3] - mTouchSlop, (int) floats[0] + mTouchSlop, (int) floats[1] + mTouchSlop);
//                            if (rect.contains((int) event.getX(), (int) event.getY())) {
//                                otherLine = i;
//                            }
//                        }
//                        Log.i(TAG, "otherLine:" + otherLine);
//                        if (otherLine != -1) {
//                        } else {
//                            Path path = new Path();
//                            paths.add(path);
//                            history.push(OPERATION_MODE_DRAW);
//                            float[] a = {event.getX(), event.getY(), event.getX(), event.getY()};
//                            maxMinPoints.add(a);
//
//                            touchDown(event, path);
////                            Log.i(TAG, "maxMinPoints:" + a[0] + "," + a[1] + "," + a[2] + "," + a[3] + ",");
//                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
//                        if (otherLine != -1) {
//                            Path path1 = paths.get(otherLine);
//                            float dx = event.getX() - lastX;
//                            float dy = event.getY() - lastY;
////                            Log.i(TAG, "dx:" + dx + ",dy:" + dy);
////                            path1.offset( dx, dy);
//                            Matrix matrix = new Matrix();
//                            matrix.setTranslate(dx, dy);
//
//                            path1.transform(matrix);
//
//                            float[] floats = maxMinPoints.get(otherLine);
//                            floats[0] += dx;
//                            floats[2] += dx;
//                            floats[1] += dy;
//                            floats[3] += dy;
//
//                            lastX = event.getX();
//                            lastY = event.getY();
//                        } else {
//                            Path path1 = paths.get(paths.size() - 1);
//                            float[] a1 = maxMinPoints.get((maxMinPoints.size() - 1));
//                            touchMove(event, path1, a1);
////                            Log.i(TAG, "maxMinPoints:" + a1[0] + "," + a1[1] + "," + a1[2] + "," + a1[3] + ",");
//                        }
                        break;
                    case MotionEvent.ACTION_UP:
//                        if (otherLine != -1) {
//
//                            float[] floats = maxMinPoints.get(otherLine);
//                            Rect rect = new Rect((int) floats[0] - mTouchSlop, (int) floats[3] - mTouchSlop, (int) floats[0] + mTouchSlop, (int) floats[3] + mTouchSlop);
//                            if (rect.contains((int) event.getX(), (int) event.getY())) {
//                                paths.remove(otherLine);
//                                maxMinPoints.remove(otherLine);
//                                history.pop();
//                                otherLine = -1;
//                            }
//
//                        } else {
//
//                        }
//                        if (maxMinPoints.size() > 0) {
//
//                            float[] floats = maxMinPoints.get(maxMinPoints.size() - 1);
//                            if (floats[0] == floats[2] && floats[1] == floats[3]) {
//
//                                paths.remove(paths.size() - 1);
//                                maxMinPoints.remove(maxMinPoints.size() - 1);
//                                history.pop();
//                            }
//                        }
                        break;
                }
                mGestureListener.onTouchEvent(event);
                //更新绘制
                invalidate();
                return true;
            case OPERATION_MODE_TYPING:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                mGestureListener.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mMode == OPERATION_MODE_RESET) {
            return true;
        }
//        if (mMode == OPERATION_MODE_TYPING) {
//            return true;
//        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //通过画布绘制多点形成的图形
        for (int i = 0; i < paths.size(); i++) {
            Path path = paths.get(i);
            canvas.drawPath(path, mGesturePaint);
        }
        if (otherLine > -1) {

            float[] floats = maxMinPoints.get(otherLine);
            Rect rect = new Rect((int) floats[2], (int) floats[3], (int) floats[0], (int) floats[1]);
            canvas.drawRect(rect, rectPaint);
            canvas.drawBitmap(redClose, (int) floats[0] - redClose.getWidth() / 2, (int) floats[3] - redClose.getHeight() / 2, rectPaint);
        }
    }

    //手指点下屏幕时调用
    private void touchDown(MotionEvent event, Path path) {

        //mPath.rewind();
        //重置绘制路线，即隐藏之前绘制的轨迹
        path.reset();
        float x = event.getX();
        float y = event.getY();

        mX = x;
        mY = y;
        //mPath绘制的绘制起点
        path.moveTo(x, y);
    }

    //手指在屏幕上滑动时调用
    private void touchMove(MotionEvent event, Path path, float[] a1) {
        final float x = event.getX();
        final float y = event.getY();

        final float previousX = mX;
        final float previousY = mY;

        final float dx = Math.abs(x - previousX);
        final float dy = Math.abs(y - previousY);

        //两点之间的距离大于等于3时，生成贝塞尔绘制曲线
        if (dx >= 3 || dy >= 3) {
            //设置贝塞尔曲线的操作点为起点和终点的一半
            float cX = (x + previousX) / 2;
            float cY = (y + previousY) / 2;

            //二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
            path.quadTo(previousX, previousY, cX, cY);
//            path.lineTo(x,y);

            if (cX > a1[0]) {
                a1[0] = cX;
            }
            if (cX < a1[2]) {
                a1[2] = cX;
            }
            if (cY > a1[1]) {
                a1[1] = cY;
            }
            if (cY < a1[3]) {
                a1[3] = cY;
            }

            //第二次执行时，第一次结束调用的坐标值将作为第二次调用的初始坐标值
            mX = x;
            mY = y;
        }
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            if (mMode == OPERATION_MODE_RESET) {
                simpleListener.onDown(e);
            }
            if (mMode == OPERATION_MODE_TYPING) {
                dragging = null;
                for (RelativeLayout layout : texts) {
                    Rect r = new Rect();

                    layout.getGlobalVisibleRect(r);
                    if (r.contains((int) e.getX(), (int) e.getY())) {
                        dragging = layout;
                    }
                }
                lastX = 0;
                lastY = 0;
            }
            if (mMode == OPERATION_MODE_DRAW) {
                int temp=-1;
                for (int i = 0; i < maxMinPoints.size(); i++) {
                    float[] floats = maxMinPoints.get(i);
                    Rect rect = new Rect((int) floats[2] - mTouchSlop, (int) floats[3] - mTouchSlop, (int) floats[0] + mTouchSlop, (int) floats[1] + mTouchSlop);
                    if (rect.contains((int) e.getX(), (int) e.getY())) {
                        temp = i;
                    }
                }
//                otherLine=temp;
                if (temp == -1) {
                    otherLine = -1;
                }

                if (otherLine != -1) {
                } else {
                    Path path = new Path();
                    paths.add(path);
                    history.push(OPERATION_MODE_DRAW);
                    float[] a = {e.getX(), e.getY(), e.getX(), e.getY()};
                    maxMinPoints.add(a);

                    touchDown(e, path);
                }
            }
            return super.onDown(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if (mMode == OPERATION_MODE_RESET) {
                simpleListener.onDrag(e2);
            }
            if (mMode == OPERATION_MODE_TYPING) {
                if (dragging != null) {

                    if (lastX != 0 && lastY != 0) {

                        LayoutParams layoutParams1 = (LayoutParams) dragging.getLayoutParams();
                        layoutParams1.topMargin += (int) (e2.getY() - lastY);
                        layoutParams1.leftMargin += (int) (e2.getX() - lastX);
                        dragging.requestLayout();
                    }
                    lastX = e2.getX();
                    lastY = e2.getY();
                }
            }
            if (mMode == OPERATION_MODE_DRAW) {
                if (otherLine != -1) {
                    Path path1 = paths.get(otherLine);
                    float dx = e2.getX() - lastX;
                    float dy = e2.getY() - lastY;
//                            Log.i(TAG, "dx:" + dx + ",dy:" + dy);
//                            path1.offset( dx, dy);
                    Matrix matrix = new Matrix();
                    matrix.setTranslate(dx, dy);

                    path1.transform(matrix);

                    float[] floats = maxMinPoints.get(otherLine);
                    floats[0] += dx;
                    floats[2] += dx;
                    floats[1] += dy;
                    floats[3] += dy;

                    lastX = e2.getX();
                    lastY = e2.getY();
                } else {
                    Path path1 = paths.get(paths.size() - 1);
                    float[] a1 = maxMinPoints.get((maxMinPoints.size() - 1));
                    touchMove(e2, path1, a1);
//                            Log.i(TAG, "maxMinPoints:" + a1[0] + "," + a1[1] + "," + a1[2] + "," + a1[3] + ",");
                }
            }


            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (mMode == OPERATION_MODE_RESET) {
                openButton.callOnClick();
            }
            if (mMode == OPERATION_MODE_TYPING) {
                Log.i(TAG, "zi:" + event.getX() + " " + event.getY());
                addText((int) event.getX(), (int) event.getY());
                return true;
            }
            if (mMode == OPERATION_MODE_DRAW) {

                if (maxMinPoints.size() > 0) {

                    float[] floats = maxMinPoints.get(maxMinPoints.size() - 1);
                    if (floats[0] == floats[2] && floats[1] == floats[3]) {

                        paths.remove(paths.size() - 1);
                        maxMinPoints.remove(maxMinPoints.size() - 1);
                        history.pop();
                    }
                }
                if (otherLine != -1) {

                    float[] floats = maxMinPoints.get(otherLine);
                    Rect rect = new Rect((int) floats[0] - mTouchSlop, (int) floats[3] - mTouchSlop, (int) floats[0] + mTouchSlop, (int) floats[3] + mTouchSlop);
                    if (rect.contains((int) event.getX(), (int) event.getY())) {
                        paths.remove(otherLine);
                        maxMinPoints.remove(otherLine);
                        if (history.size() > 0) {
                            history.pop();
                        }
                        otherLine = -1;
                    }else{

                        findOtherLine((int) event.getX(), (int) event.getY());
                    }

                } else {

                    findOtherLine((int) event.getX(), (int) event.getY());

                }

            }
            return super.onSingleTapUp(event);
        }
    }

    private void addText(int x, int y) {
        LayoutParams layoutParams = new LayoutParams((int) DensityUtil.dip2px(getContext(), 200), ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = y;
        layoutParams.leftMargin = x;
        layoutParams.alignWithParent = true;

        final RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setId(generateViewId());
        relativeLayout.setMinimumHeight((int) DensityUtil.dip2px(getContext(), 100));
        relativeLayout.setLayoutParams(layoutParams);
        relativeLayout.setPadding(textPadding, textPadding, textPadding, textPadding);
        relativeLayout.setBackgroundColor(Color.parseColor("#80000000"));

        final EditText editText = new EditText(getContext());
        final ImageView imageView = new ImageView(getContext());
        int w = (int) DensityUtil.dip2px(getContext(), 32);
        LayoutParams layoutParams2 = new LayoutParams(w, w);
        int margin = w / 2;
        layoutParams2.leftMargin = -margin;
        layoutParams2.topMargin = -margin;
        Log.i(TAG, "relativeLayout.getId():" + relativeLayout.getId());
        layoutParams2.addRule(RelativeLayout.ALIGN_TOP, relativeLayout.getId());
        layoutParams2.addRule(RelativeLayout.RIGHT_OF, relativeLayout.getId());
        imageView.setImageResource(R.drawable.ps05);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setLayoutParams(layoutParams2);
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i(TAG, "x");
                editText.clearFocus();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                MaskContainer.this.removeView(relativeLayout);
                MaskContainer.this.removeView(imageView);
                texts.remove(relativeLayout);
                textsCloses.remove(imageView);
                if (history.size() > 0) {
                    history.pop();
                }
            }
        });


        relativeLayout.addView(editText);
        MaskContainer.this.addView(relativeLayout);
        MaskContainer.this.addView(imageView);
        texts.add(relativeLayout);
        textsCloses.add(imageView);
        history.push(OPERATION_MODE_TYPING);
        editText.requestFocus();
        editText.setHint("请输入文字");
        editText.setPadding(0, 0, 0, 0);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setTextColor(Color.RED);
        editText.setBackgroundColor(0);
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                Log.i(TAG, editText + "hasFocus:" + hasFocus);
                if (hasFocus) {

                    relativeLayout.setBackgroundColor(Color.parseColor("#80000000"));
                    imageView.setVisibility(VISIBLE);
                } else {

                    relativeLayout.setBackgroundColor(0);
                    imageView.setVisibility(GONE);
                    if (editText.getText().toString().equals("")) {

                        MaskContainer.this.removeView(relativeLayout);
                        MaskContainer.this.removeView(imageView);
                        texts.remove(relativeLayout);
                        textsCloses.remove(imageView);
                        if (history.size() > 0) {
                            history.pop();
                        }
                    }
                }
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.i(TAG, editText + "actionId:" + actionId + " event:" + event);
                if (actionId == 0) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    editText.clearFocus();
                    return true;
                }
                return false;
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.i(TAG, editText + "onTextChanged:" + s);
                if (s.toString().contains("\n")) {
                    editText.setText(editText.getText().toString().replaceAll("\n",""));
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    editText.clearFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(editText, 0);
            }
        }, 200);
    }

    private void findOtherLine(int x,int y) {
        for (int i = 0; i < maxMinPoints.size(); i++) {
            float[] floats = maxMinPoints.get(i);
            Rect rect = new Rect((int) floats[2] - mTouchSlop, (int) floats[3] - mTouchSlop, (int) floats[0] + mTouchSlop, (int) floats[1] + mTouchSlop);
            if (rect.contains(x, y)) {
                otherLine = i;
            }
        }
    }


    /**
     * 动态生成View ID
     * API LEVEL 17 以上View.generateViewId()生成
     * API LEVEL 17 以下需要手动生成
     */
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int generateViewId() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        } else {
            return View.generateViewId();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == event.getKeyCode() && KeyEvent.ACTION_UP == event.getAction()) {
            Log.i(TAG, " event:" + event);
            undoButton.callOnClick();
        }
        return super.dispatchKeyEvent(event);
    }
}
