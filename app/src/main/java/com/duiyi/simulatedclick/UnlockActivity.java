package com.duiyi.simulatedclick;

import android.app.KeyguardManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class UnlockActivity extends AppCompatActivity {
    private static final String TAG = SimulatedClickService.class.getSimpleName();
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_unlock);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        Log.i(TAG, "onCreate Unlock Activity");

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager == null) {
            return;
        }
/*        try {
            keyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissError() {
                    super.onDismissError();
                    Log.i(TAG, "onDismissError");
                }

                @Override
                public void onDismissSucceeded() {
                    super.onDismissSucceeded();
                    Log.i(TAG, "onDismissSucceeded");
                }

                @Override
                public void onDismissCancelled() {
                    super.onDismissCancelled();
                    Log.i(TAG, "onDismissSucceeded");
                }
            });
        } catch (Exception e) {*/

            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("unLock");
            if (keyguardManager.isKeyguardLocked()) {
                keyguardLock.disableKeyguard();
                Log.i(TAG, "disableKeyguard 2");
            }
 //       }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 500);
    }
}
