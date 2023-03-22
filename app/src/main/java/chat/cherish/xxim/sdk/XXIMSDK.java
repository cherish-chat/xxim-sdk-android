package chat.cherish.xxim.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.protobuf.ByteString;

import java.util.concurrent.ConcurrentHashMap;

import chat.cherish.xxim.core.XXIMCore;
import chat.cherish.xxim.core.callback.RequestCallback;
import chat.cherish.xxim.core.common.CxnParams;
import chat.cherish.xxim.core.listener.ConnectListener;
import chat.cherish.xxim.core.listener.ReceivePushListener;
import chat.cherish.xxim.sdk.callback.OperateCallback;
import chat.cherish.xxim.sdk.callback.SubscribeCallback;
import chat.cherish.xxim.sdk.common.AesParams;
import chat.cherish.xxim.sdk.listener.ConvListener;
import chat.cherish.xxim.sdk.listener.MsgListener;
import chat.cherish.xxim.sdk.listener.NoticeListener;
import chat.cherish.xxim.sdk.listener.PullListener;
import chat.cherish.xxim.sdk.listener.UnreadListener;
import chat.cherish.xxim.sdk.manager.ConvManager;
import chat.cherish.xxim.sdk.manager.MsgManager;
import chat.cherish.xxim.sdk.manager.NoticeManager;
import chat.cherish.xxim.sdk.manager.SDKManager;
import chat.cherish.xxim.sdk.tool.SDKTool;
import pb.Core;

public class XXIMSDK {

    private Context context;
    private Handler handler;
    private SharedPreferences preferences;
    private XXIMCore xximCore;
    private SDKManager sdkManager;

    public ConvManager convManager;
    public MsgManager msgManager;
    public NoticeManager noticeManager;

    public void init(Context context, int requestTimeout, String rsaPublicKey, String aesKey, CxnParams cxnParams,
                     int autoPullTime, int pullMsgCount, ConnectListener connectListener,
                     SubscribeCallback subscribeCallback, PullListener pullListener,
                     ConvListener convListener, MsgListener msgListener,
                     NoticeListener noticeListener, UnreadListener unreadListener
    ) {
        this.context = context;
        handler = new Handler(Looper.getMainLooper());
        preferences = context.getSharedPreferences("xxim", Context.MODE_PRIVATE);
        xximCore = new XXIMCore();
        xximCore.init(
                requestTimeout,
                new ConnectListener() {
                    @Override
                    public void onConnecting() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                connectListener.onConnecting();
                            }
                        });
                    }

                    @Override
                    public void onSuccess() {
                        setCxnParams(rsaPublicKey, aesKey, cxnParams, connectListener);
                    }

                    @Override
                    public void onClose(int code, String error) {
                        sdkManager.closeDatabase();
                        closePullSubscribe();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                connectListener.onClose(code, error);
                            }
                        });
                    }
                },
                new ReceivePushListener() {
                    @Override
                    public void onPushMsgDataList(Core.MsgDataList msgDataList) {
                        sdkManager.onPushMsgDataList(msgDataList.getMsgDataListList());
                    }

                    @Override
                    public void onPushNoticeData(Core.NoticeData noticeData) {
                        sdkManager.onPushNoticeData(noticeData);
                    }
                }
        );
        sdkManager = new SDKManager(
                handler,
                xximCore,
                autoPullTime,
                pullMsgCount,
                subscribeCallback,
                pullListener,
                convListener,
                msgListener,
                noticeListener,
                unreadListener
        );
        msgManager = new MsgManager(sdkManager);
        noticeManager = new NoticeManager(sdkManager);
        convManager = new ConvManager(sdkManager, msgManager, noticeManager);
    }

    // 连接
    public void connect(String wsUrl) {
        xximCore.connect(wsUrl);
    }

    // 断连
    public void disconnect() {
        sdkManager.closeDatabase();
        xximCore.disconnect();
        closePullSubscribe();
    }

    // 是否连接
    public boolean isConnect() {
        return xximCore.isConnect();
    }

    // 设置连接参数
    private void setCxnParams(String rsaPublicKey, String aesKey, CxnParams cxnParams, ConnectListener connectListener) {
        setCxnParams(rsaPublicKey, aesKey, cxnParams, new OperateCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectListener.onSuccess();
                    }
                });
            }

            @Override
            public void onError(int code, String error) {
                setCxnParams(rsaPublicKey, aesKey, cxnParams, connectListener);
            }
        });
    }

    // 设置连接参数
    public void setCxnParams(String rsaPublicKey, String aesKey, CxnParams cxnParams, OperateCallback<Boolean> callback) {
        String packageId = preferences.getString("packageId", "");
        if (TextUtils.isEmpty(packageId)) {
            packageId = SDKTool.getUUId();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("packageId", packageId);
            editor.apply();
        }
        xximCore.setCxnParams(SDKTool.getUUId(), packageId, rsaPublicKey, aesKey, cxnParams, new RequestCallback<Core.SetCxnParamsResp>() {
            @Override
            public void onSuccess(Core.SetCxnParamsResp setCxnParamsResp) {
                callback.onSuccess(true);
            }

            @Override
            public void onError(int code, String error) {
                callback.onError(code, error);
            }
        });
    }

    // 设置用户参数
    public void setUserParams(String userId, String token, String ext, String boxName,
                              ConcurrentHashMap<String, AesParams> convParams, OperateCallback<Boolean> callback) {
        sdkManager.openDatabase(context, userId, boxName);
        Core.SetUserParamsReq req = Core.SetUserParamsReq.newBuilder()
                .setUserId(userId)
                .setToken(token)
                .setExt(ByteString.copyFromUtf8(ext))
                .build();
        xximCore.setUserParams(SDKTool.getUUId(), req, new RequestCallback<Core.SetUserParamsResp>() {
            @Override
            public void onSuccess(Core.SetUserParamsResp setUserParamsResp) {
                openPullSubscribe(convParams);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(true);
                    }
                });
            }

            @Override
            public void onError(int code, String error) {
                sdkManager.closeDatabase();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(code, error);
                    }
                });
            }
        });
    }

    // 打开拉取订阅
    public void openPullSubscribe(ConcurrentHashMap<String, AesParams> convParams) {
        sdkManager.openPullSubscribe(convParams);
    }

    // 关闭拉取订阅
    public void closePullSubscribe() {
        sdkManager.closePullSubscribe();
    }

    // 自定义请求
    public void customRequest(String method, byte[] bytes, OperateCallback<byte[]> callback) {
        xximCore.customRequest(SDKTool.getUUId(), method, ByteString.copyFrom(bytes), new RequestCallback<ByteString>() {
            @Override
            public void onSuccess(ByteString byteString) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(byteString.toByteArray());
                    }
                });
            }

            @Override
            public void onError(int code, String error) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(code, error);
                    }
                });
            }
        });
    }
}
