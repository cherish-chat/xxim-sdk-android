package chat.cherish.xxim.sdk;

import android.content.Context;

import chat.cherish.xxim.core.XXIMCore;
import chat.cherish.xxim.core.listener.ConnectListener;
import chat.cherish.xxim.core.listener.ReceivePushListener;
import chat.cherish.xxim.sdk.callback.SubscribeCallback;
import chat.cherish.xxim.sdk.common.CXNParams;
import chat.cherish.xxim.sdk.listener.ConvListener;
import chat.cherish.xxim.sdk.listener.MsgListener;
import chat.cherish.xxim.sdk.listener.NoticeListener;
import chat.cherish.xxim.sdk.listener.PullListener;
import chat.cherish.xxim.sdk.listener.UnreadListener;
import chat.cherish.xxim.sdk.manager.ConvManager;
import chat.cherish.xxim.sdk.manager.MsgManager;
import chat.cherish.xxim.sdk.manager.NoticeManager;
import chat.cherish.xxim.sdk.manager.SDKManager;
import pb.Core;

public class XXIMSDK {

    private XXIMCore xximCore;
    private SDKManager sdkManager;

    public ConvManager convManager;
    public MsgManager msgManager;
    public NoticeManager noticeManager;

    public void init(Context context, int requestTimeout, CXNParams cxnParams, int autoPullTime, int pullMsgCount,
                     ConnectListener connectListener, SubscribeCallback subscribeCallback,
                     PullListener pullListener, ConvListener convListener,
                     MsgListener msgListener, NoticeListener noticeListener,
                     UnreadListener unreadListener
    ) {
        xximCore = new XXIMCore();
        xximCore.init(
                requestTimeout,
                connectListener,
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

}
