package chat.cherish.xxim.sdk;

import android.content.Context;

import com.google.protobuf.ByteString;

import java.util.List;

import chat.cherish.xxim.core.XXIMCore;
import chat.cherish.xxim.core.callback.RequestCallback;
import chat.cherish.xxim.core.listener.ConnectListener;
import chat.cherish.xxim.core.listener.ReceivePushListener;
import chat.cherish.xxim.sdk.callback.OperateCallback;
import chat.cherish.xxim.sdk.callback.SubscribeCallback;
import chat.cherish.xxim.sdk.common.CxnParams;
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
    private XXIMCore xximCore;
    private SDKManager sdkManager;

    public ConvManager convManager;
    public MsgManager msgManager;
    public NoticeManager noticeManager;

    public void init(Context context, int requestTimeout, CxnParams cxnParams, int autoPullTime, int pullMsgCount,
                     ConnectListener connectListener, SubscribeCallback subscribeCallback,
                     PullListener pullListener, ConvListener convListener,
                     MsgListener msgListener, NoticeListener noticeListener,
                     UnreadListener unreadListener
    ) {
        this.context = context;
        xximCore = new XXIMCore();
        xximCore.init(
                requestTimeout,
                new ConnectListener() {
                    @Override
                    public void onConnecting() {
                        connectListener.onConnecting();
                    }

                    @Override
                    public void onSuccess() {
                        setCxnParams(cxnParams, connectListener);
                    }

                    @Override
                    public void onClose(int code, String error) {
                        connectListener.onClose(code, error);
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
    private void setCxnParams(CxnParams cxnParams, ConnectListener connectListener) {
        setCxnParams(cxnParams, new OperateCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                connectListener.onSuccess();
            }

            @Override
            public void onError(int code, String error) {
                setCxnParams(cxnParams, connectListener);
            }
        });
    }

    // 设置连接参数
    public void setCxnParams(CxnParams cxnParams, OperateCallback<Boolean> callback) {
        Core.SetCxnParamsReq req = Core.SetCxnParamsReq.newBuilder()
                .setPlatform(cxnParams.platform)
                .setDeviceId(cxnParams.deviceId)
                .setDeviceModel(cxnParams.deviceModel)
                .setOsVersion(cxnParams.osVersion)
                .setAppVersion(cxnParams.appVersion)
                .setLanguage(cxnParams.language)
                .setNetworkUsed(cxnParams.networkUsed)
                .setExt(ByteString.copyFromUtf8(cxnParams.ext))
                .build();
        xximCore.setCxnParams(SDKTool.getUUId(), req, new RequestCallback<Core.SetCxnParamsResp>() {
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
    public void setUserParams(String userId, String token, byte[] ext, String boxName, List<String> convIdList,
                              OperateCallback<Boolean> callback) {
        Core.SetUserParamsReq req = Core.SetUserParamsReq.newBuilder()
                .setUserId(userId)
                .setToken(token)
                .setExt(ByteString.copyFrom(ext))
                .build();
        xximCore.setUserParams(SDKTool.getUUId(), req, new RequestCallback<Core.SetUserParamsResp>() {
            @Override
            public void onSuccess(Core.SetUserParamsResp setUserParamsResp) {
                sdkManager.openDatabase(context, userId, boxName);
                openPullSubscribe(convIdList);
                callback.onSuccess(true);
            }

            @Override
            public void onError(int code, String error) {
                callback.onError(code, error);
            }
        });
    }

    // 打开拉取订阅
    public void openPullSubscribe(List<String> convIdList) {
        sdkManager.openPullSubscribe(convIdList);
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
                callback.onSuccess(byteString.toByteArray());
            }

            @Override
            public void onError(int code, String error) {
                callback.onError(code, error);
            }
        });
    }
}
