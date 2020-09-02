package com.duiyi.simulatedclick;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SimulatedClickService extends AccessibilityService {
    private static final String TAG = SimulatedClickService.class.getSimpleName();
    ///时间需要修改，check()中return 需要打开
    private boolean DEBUG = false;

    private static final String ACTION_CHECKIN = "com.simulate.ACTION_CHECKIN";
    private static final long TIME_INTERVAL = 2 * DateUtils.MINUTE_IN_MILLIS;
    private static final int AM_HOUR = 8;
    private static final int AM_MINUTE = 40;
    private static final int PM_HOUR = 20;
    private static final int PM_MINUTE = 0;
    private static int mTryCount;
    private boolean mCheckInEnabled;
    private Handler mHandler = new ActionHandler();
    private DingTalkChatBot mDingTalkChatBot = new DingTalkChatBot();
    private LogHelper mLogHelper;
    private int mRetryCount = 0;

    /**
     * 监听窗口变化的回调
     *
     * @param event 窗口变化事件
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        mLogHelper.i(TAG, "onInterrupt," + Log.getStackTraceString(new Throwable()));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLogHelper = new LogHelper(this);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        startCheckInTimer();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CHECKIN);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void startCheckInTimer() {
        mLogHelper.i(TAG, "startCheckInTimer");
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_CHECKIN);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), TIME_INTERVAL, pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void check() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            mLogHelper.i(TAG, "dayOfWeek=" + dayOfWeek);
            return;
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int rand = (int) (Math.random() * 10);
        mLogHelper.i(TAG, "Time==>" + hour + ":" + minute + ", rand=" + rand);
        //其他小时时间重置状态，保证8,17点只执行一次
        if (hour != AM_HOUR && hour != PM_HOUR) {
            mCheckInEnabled = true;
            mTryCount = 0;
        }
        if (!mCheckInEnabled && !DEBUG) {
            mLogHelper.i(TAG, "check in disabled.");
            return;
        }
        mLogHelper.i(TAG, "check: time");
        if (DEBUG || (hour == AM_HOUR && minute > (AM_MINUTE + rand))) {
            wakeUpAndUnlock();
            mHandler.sendEmptyMessageDelayed(ActionHandler.FIND_TARGET, 3000);
        } else if (hour == PM_HOUR && minute >= (PM_MINUTE + rand * 3)) {
            wakeUpAndUnlock();
            mHandler.sendEmptyMessageDelayed(ActionHandler.FIND_TARGET, 3000);
        } else {
            mLogHelper.i(TAG, "next time");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isCheckInWindow() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.hisense.kdweibo.client:id/main_item_layout");
            if (list != null && !list.isEmpty()) {
                return true;
            } else {
                mLogHelper.i(TAG, "isCheckInWindow, list is empty");
            }
        } else {
            mLogHelper.i(TAG, "isCheckInWindow, nodeInfo is null");
        }
        return false;
    }

    private void startCheckInClick() {
        postClick("com.hisense.kdweibo.client:id/main_item_layout", 2000);
        postClick("com.hisense.kdweibo.client:id/rl_checkin", 6000);
        mHandler.sendEmptyMessageDelayed(ActionHandler.CHECK_NODES, 10000);
    }

    private void postClick(final String id, long delay) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performCheckInClick(id);
            }
        }, delay);
    }

    private void performCheckInClick(String id) {
        try {
            inputClick(id);
        } catch (Exception e) {
            mLogHelper.i(TAG, "performCheckInClick, e=" + e);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void inputClick(final String clickId) {
        mLogHelper.i(TAG, "inputClick, clickId=" + clickId);
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            if (list == null) {
                mLogHelper.i(TAG, "inputClick, list == null");
                return;
            }
            mLogHelper.i(TAG, "inputClick, list.size=" + list.size());
            if (list.size() >= 3) {
                list.get(2).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            if (list.size() == 1) {
                list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                mDingTalkChatBot.send(Calendar.getInstance().getTime().toString());
                mCheckInEnabled = false;
            }
        } else {
            mLogHelper.i(TAG, "inputClick, nodeInfo == null");
            if (mRetryCount < 2) {
                mRetryCount ++;
                performGlobalAction(GLOBAL_ACTION_BACK);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inputClick(clickId);
                    }
                }, 2000);
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_CHECKIN.equals(intent.getAction())) {
                mLogHelper.i(TAG, "receiver timer");
                check();
            }
        }
    };

    private void startUnlockActivity() {
        Intent intent = new Intent(this, UnlockActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void wakeUpAndUnlock() {
        // 获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean screenOn = pm != null && pm.isInteractive();
        mLogHelper.i(TAG, "wakeup....screenOn=" + screenOn);

        if (!screenOn && pm != null) {
            @SuppressLint("InvalidWakeLockTag") final PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ScreenOn");
            // 点亮屏幕
            wl.acquire(60 * 1000 * 5);
            mLogHelper.i(TAG, "wl.acquire");
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void checkNodes() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.hisense.kdweibo.client:id/main_item_layout");
            if (list == null || list.size() == 0) {
                //未回到首页
                mHandler.sendEmptyMessage(ActionHandler.GO_BACK);
                return;
            }
        }
    }

    public class ActionHandler extends Handler {
        public final static int GO_BACK = 1;
        public final static int CHECK_NODES = 2;
        public final static int FIND_TARGET = 3;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == GO_BACK) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                mLogHelper.i(TAG, "goBack");
                mHandler.sendEmptyMessageDelayed(CHECK_NODES, 2000);
            } else if (msg.what == CHECK_NODES) {
                mLogHelper.i(TAG, "checkNodes");
                checkNodes();
            } else if (msg.what == FIND_TARGET) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                boolean screenOn = pm != null && pm.isInteractive();
                mLogHelper.i(TAG, "handleMessage, FIND_TARGET....screenOn=" + screenOn);

                if (isCheckInWindow()) {
                    startCheckInClick();
                } else if (mTryCount++ < 10) {
                    mLogHelper.i(TAG, "not checkIn activity");
                    // performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
                    //takeScreenshot();

                    if (mTryCount > 3 && mTryCount < 6) {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                    } else {
                        performGlobalAction(GLOBAL_ACTION_HOME);
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.hisense.kdweibo.client", "com.kdweibo.android.ui.activity.StartActivity"));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    sendEmptyMessageDelayed(FIND_TARGET, 10000);
                } else {
                    mLogHelper.i(TAG, "can't find activity");
                }
            }
        }
    }

    private Bitmap screenShotByReflect() {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float[] dims = { displayMetrics.widthPixels, displayMetrics.heightPixels };
        try {
            Class<?> demo = Class.forName("android.view.SurfaceControl");
            Method method = demo.getDeclaredMethod("screenshot", int.class, int.class);
            return (Bitmap) method.invoke(null, (int) dims[0], (int) dims[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void takeScreenshot() {
        //调用截屏
        Bitmap bitmap = screenShotByReflect();
        if (bitmap == null) {
            mLogHelper.i(TAG, "takeScreenshot fail");
            return;
        }
        File dir = this.getExternalCacheDir();
        if (!dir.exists()){
            dir.mkdirs();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String filename = dateFormat.format(new Date()) + ".jpg";
        File file = new File(dir, filename);
        saveBitmap(bitmap, filename);
    }


    private void saveBitmap(Bitmap bitmap, String filePath) {
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
