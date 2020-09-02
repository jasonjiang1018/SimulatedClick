package com.duiyi.simulatedclick;

import android.util.Log;

import com.dingtalk.chatbot.message.TextMessage;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DingTalkChatBot {
    private static final String TAG = "DingTalkChatBot";
    private OkHttpClient mOkHttpClient;

    public DingTalkChatBot() {
        mOkHttpClient = new OkHttpClient();
    }

    public void send(String str) {
        TextMessage textMessage = new TextMessage(str + " talk ");
        String url = "https://oapi.dingtalk.com/robot/send?access_token=c71ac6321d74ede03ad3650b5d2d164d7f9ad50cd760310179f1bb2a424e845b";

        final Request request = new Request.Builder().url(url).post(RequestBody.create(MediaType.parse("application/json"),textMessage.toJsonString() )).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, ",e=" + e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                Log.i(TAG, "response=" + response);
            }
        });
    }
}
