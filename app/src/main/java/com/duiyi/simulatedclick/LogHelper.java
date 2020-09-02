package com.duiyi.simulatedclick;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogHelper {
    private SimpleDateFormat mTimeFormat;
    private SimpleDateFormat mDateFormat;
    public Context mContext;

    public LogHelper(Context context) {
        this.mContext = context;
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public void i(String tag, String msg) {
        Log.i(tag, msg);
        try {
            writeLogToFile(tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void writeLogToFile(String tag, String msg) {
        mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date nowTime = new Date();
        String filename = mDateFormat.format(nowTime);
        String msgStr = mTimeFormat.format(nowTime) + " " + tag + " processId=" + android.os.Process.myPid() + " " + msg;
        File dir = mContext.getExternalCacheDir();
        if (!dir.exists()){
            dir.mkdirs();
        }
        File file = new File(dir, filename);
        try {
            FileWriter filerWriter = new FileWriter(file, true);
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(msgStr);
            bufWriter.newLine();
            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
