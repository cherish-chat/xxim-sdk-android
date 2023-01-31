package chat.cherish.xxim.sdk.manager;

import android.content.Context;
import android.text.TextUtils;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import chat.cherish.xxim.core.XXIMCore;
import chat.cherish.xxim.core.callback.RequestCallback;
import chat.cherish.xxim.sdk.callback.OperateCallback;
import chat.cherish.xxim.sdk.callback.SubscribeCallback;
import chat.cherish.xxim.sdk.common.AesParams;
import chat.cherish.xxim.sdk.common.ContentType;
import chat.cherish.xxim.sdk.common.ConvType;
import chat.cherish.xxim.sdk.common.SendStatus;
import chat.cherish.xxim.sdk.listener.ConvListener;
import chat.cherish.xxim.sdk.listener.MsgListener;
import chat.cherish.xxim.sdk.listener.NoticeListener;
import chat.cherish.xxim.sdk.listener.PullListener;
import chat.cherish.xxim.sdk.listener.UnreadListener;
import chat.cherish.xxim.sdk.model.ConvModel;
import chat.cherish.xxim.sdk.model.ConvModel_;
import chat.cherish.xxim.sdk.model.MsgModel;
import chat.cherish.xxim.sdk.model.MsgModel_;
import chat.cherish.xxim.sdk.model.MyObjectBox;
import chat.cherish.xxim.sdk.model.NoticeModel;
import chat.cherish.xxim.sdk.model.NoticeModel_;
import chat.cherish.xxim.sdk.model.ReadModel;
import chat.cherish.xxim.sdk.model.ReadModel_;
import chat.cherish.xxim.sdk.model.RecordModel;
import chat.cherish.xxim.sdk.model.RecordModel_;
import chat.cherish.xxim.sdk.model.SDKContent;
import chat.cherish.xxim.sdk.tool.SDKTool;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import pb.Core;

public class SDKManager {
    public XXIMCore xximCore;
    public int autoPullTime;
    public int pullMsgCount;
    public SubscribeCallback subscribeCallback;
    public PullListener pullListener;
    public ConvListener convListener;
    public MsgListener msgListener;
    public NoticeListener noticeListener;
    public UnreadListener unreadListener;

    public SDKManager(XXIMCore xximCore, int autoPullTime, int pullMsgCount,
                      SubscribeCallback subscribeCallback, PullListener pullListener,
                      ConvListener convListener, MsgListener msgListener,
                      NoticeListener noticeListener, UnreadListener unreadListener
    ) {
        this.xximCore = xximCore;
        this.autoPullTime = autoPullTime;
        this.pullMsgCount = pullMsgCount;
        this.subscribeCallback = subscribeCallback;
        this.pullListener = pullListener;
        this.convListener = convListener;
        this.msgListener = msgListener;
        this.noticeListener = noticeListener;
        this.unreadListener = unreadListener;
    }

    private String userId;
    private BoxStore boxStore;

    private boolean pullStatus = false;
    private Timer timer;
    private TimerTask timerTask;

    // 打开数据库
    public void openDatabase(Context context, String userId, String boxName) {
        this.userId = userId;
        if (boxName == null) {
            boxName = userId;
        }
        boxStore = MyObjectBox.builder()
                .name(boxName)
                .androidContext(context.getApplicationContext())
                .build();
    }

    // 关闭数据库
    public void closeDatabase() {
        if (boxStore != null) {
            boxStore.close();
        }
    }

    // 记录表
    public Box<RecordModel> recordBox() {
        return boxStore.boxFor(RecordModel.class);
    }

    // 会话表
    public Box<ConvModel> convBox() {
        return boxStore.boxFor(ConvModel.class);
    }

    // 消息表
    public Box<MsgModel> msgBox() {
        return boxStore.boxFor(MsgModel.class);
    }

    // 通知表
    public Box<NoticeModel> noticeBox() {
        return boxStore.boxFor(NoticeModel.class);
    }

    // 已读表
    public Box<ReadModel> readBox() {
        return boxStore.boxFor(ReadModel.class);
    }

    // 启动定时器
    private void startTimer() {
        cancelTimer();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                openPullSubscribe(null);
            }
        };
        timer.schedule(timerTask, 0, autoPullTime);
    }

    // 取消定时器
    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    // 打开拉取订阅
    public void openPullSubscribe(List<String> convIdList) {
        pullStatus = true;
        cancelTimer();
        if (pullListener != null) pullListener.onStart();
        if (convIdList == null) {
            if (subscribeCallback != null) {
                convIdList = subscribeCallback.onConvIdList();
            }
        }
        if (convIdList == null || convIdList.isEmpty()) {
            if (pullListener != null) pullListener.onEnd();
            if (pullStatus) startTimer();
            return;
        }
        Core.BatchGetConvSeqReq req = Core.BatchGetConvSeqReq.newBuilder()
                .addAllConvIdList(convIdList)
                .build();
        xximCore.batchGetConvSeq(
                SDKTool.getUUId(),
                req,
                new RequestCallback<Core.BatchGetConvSeqResp>() {
                    @Override
                    public void onSuccess(Core.BatchGetConvSeqResp resp) {
                        if (resp.getConvSeqMapMap().isEmpty()) {
                            if (pullListener != null) pullListener.onEnd();
                            if (pullStatus) startTimer();
                            return;
                        }
                        List<Core.BatchGetMsgListByConvIdReq.Item> items = new ArrayList<>();
                        Set<String> convIdList = resp.getConvSeqMapMap().keySet();
                        QueryBuilder<RecordModel> recordBuilder = recordBox().query();
                        int index = 0;
                        for (String convId : convIdList) {
                            ++index;
                            recordBuilder.equal(
                                    RecordModel_.convId, convId,
                                    QueryBuilder.StringOrder.CASE_SENSITIVE
                            );
                            if (index < convIdList.size()) {
                                recordBuilder.or();
                            }
                        }
                        Query<RecordModel> recordQuery = recordBuilder.build();
                        List<RecordModel> recordModelList = recordQuery.find();
                        recordQuery.close();
                        for (String convId : convIdList) {
                            RecordModel recordModel = handleConvSeq(
                                    recordModelList,
                                    convId,
                                    resp.getConvSeqMapMap().get(convId)
                            );
                            long minSeq = recordModel.minSeq;
                            long maxSeq = recordModel.maxSeq;
                            if (recordModel.seq >= minSeq) {
                                minSeq = recordModel.seq;
                            }
                            if (maxSeq - minSeq > pullMsgCount) {
                                minSeq = maxSeq - pullMsgCount;
                            }
                            if (maxSeq <= minSeq) return;
                            items.add(Core.BatchGetMsgListByConvIdReq.Item.newBuilder()
                                    .setConvId(convId)
                                    .addAllSeqList(SDKTool.generateSeqList(minSeq, maxSeq))
                                    .build()
                            );
                        }
                        if (!items.isEmpty()) {
                            pullMsgDataList(items, new RequestCallback<Core.GetMsgListResp>() {
                                @Override
                                public void onSuccess(Core.GetMsgListResp resp) {
                                    if (pullListener != null) pullListener.onEnd();
                                    if (pullStatus) startTimer();
                                }

                                @Override
                                public void onError(int code, String error) {
                                    if (pullListener != null) pullListener.onEnd();
                                    if (pullStatus) startTimer();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(int code, String error) {
                        if (pullListener != null) pullListener.onEnd();
                        if (pullStatus) startTimer();
                    }
                }
        );
    }

    RecordModel handleConvSeq(List<RecordModel> recordModelList, String convId,
                              Core.BatchGetConvSeqResp.ConvSeq convSeq
    ) {
        int index = -1;
        for (int i = 0; i < recordModelList.size(); i++) {
            if (TextUtils.equals(recordModelList.get(i).convId, convId)) {
                index = i;
                break;
            }
        }
        RecordModel recordModel;
        if (index != -1) {
            long minSeq = Long.parseLong(convSeq.getMinSeq());
            long maxSeq = Long.parseLong(convSeq.getMaxSeq());
            long updateTime = Long.parseLong(convSeq.getUpdateTime());
            recordModel = recordModelList.get(index);
            boolean updated = false;
            if (recordModel.minSeq != minSeq) {
                recordModel.minSeq = minSeq;
                updated = true;
            }
            if (recordModel.maxSeq != maxSeq) {
                recordModel.maxSeq = maxSeq;
                updated = true;
            }
            if (recordModel.updateTime != updateTime) {
                recordModel.updateTime = updateTime;
                updated = true;
            }
            if (updated) recordBox().put(recordModel);
        } else {
            recordModel = RecordModel.fromProto(convSeq);
            recordBox().put(recordModel);
        }
        return recordModel;
    }

    // 关闭拉取订阅
    public void closePullSubscribe() {
        pullStatus = false;
        cancelTimer();
    }

    private Map<String, AesParams> convAesParams(List<Core.MsgData> msgDataList) {
        List<String> convIdList = new ArrayList<>();
        for (Core.MsgData msgData : msgDataList) {
            convIdList.add(msgData.getConvId());
        }
        return subscribeCallback.onConvAesParams(convIdList);
    }

    public void pullMsgDataList(List<Core.BatchGetMsgListByConvIdReq.Item> items,
                                RequestCallback<Core.GetMsgListResp> callback
    ) {
        Core.BatchGetMsgListByConvIdReq req = Core.BatchGetMsgListByConvIdReq.newBuilder()
                .addAllItems(items)
                .build();
        xximCore.batchGetMsgListByConvId(
                SDKTool.getUUId(),
                req,
                new RequestCallback<Core.GetMsgListResp>() {
                    @Override
                    public void onSuccess(Core.GetMsgListResp resp) {
                        Map<String, AesParams> convAesMap = convAesParams(resp.getMsgDataListList());
                        for (Core.MsgData msgData : resp.getMsgDataListList()) {
                            handleMsg(msgData, convAesMap.get(msgData.getConvId()));
                        }
                        if (callback != null) callback.onSuccess(resp);
                    }

                    @Override
                    public void onError(int code, String error) {
                        if (callback != null) callback.onError(code, error);
                    }
                }
        );
    }

    public void pullMsgDataById(String serverMsgId, String clientMsgId,
                                OperateCallback<MsgModel> callback
    ) {
        Core.GetMsgByIdReq req = Core.GetMsgByIdReq.newBuilder()
                .setServerMsgId(serverMsgId)
                .setClientMsgId(clientMsgId)
                .build();
        xximCore.getMsgById(SDKTool.getUUId(), req, new RequestCallback<Core.GetMsgByIdResp>() {
            @Override
            public void onSuccess(Core.GetMsgByIdResp resp) {
                Core.MsgData msgData = resp.getMsgData();
                List<Core.MsgData> msgDataList = new ArrayList<>();
                msgDataList.add(msgData);
                Map<String, AesParams> convAesMap = convAesParams(msgDataList);
                callback.onSuccess(handleMsg(msgData, convAesMap.get(msgData.getConvId())));
            }

            @Override
            public void onError(int code, String error) {
                callback.onError(code, error);
            }
        });
    }

    // 推送消息列表
    public void onPushMsgDataList(List<Core.MsgData> msgDataList) {
        boolean isFirstPull = msgBox().count() == 0;
        Map<String, AesParams> convAesMap = convAesParams(msgDataList);
        List<MsgModel> msgModelList = new ArrayList<>();
        for (Core.MsgData msgData : msgDataList) {
            msgModelList.add(handleMsg(msgData, convAesMap.get(msgData.getConvId())));
        }
        if (!isFirstPull && !msgModelList.isEmpty()) {
            if (msgListener != null) msgListener.onReceive(msgModelList);
        }
        if (convListener != null) convListener.onUpdate();
        calculateUnreadCount();
    }

    // 推送通知
    public void onPushNoticeData(Core.NoticeData noticeData) {
        NoticeModel noticeModel = handleNotice(noticeData);
        if (noticeListener != null) {
            boolean status = noticeListener.onReceive(noticeModel);
            if (status) {
                Core.AckNoticeDataReq req = Core.AckNoticeDataReq.newBuilder()
                        .setConvId(noticeModel.convId)
                        .setNoticeId(noticeModel.noticeId)
                        .build();
                xximCore.ackNoticeData(SDKTool.getUUId(), req, new RequestCallback<Core.AckNoticeDataResp>() {
                    @Override
                    public void onSuccess(Core.AckNoticeDataResp resp) {
                        super.onSuccess(resp);
                    }

                    @Override
                    public void onError(int code, String error) {
                        super.onError(code, error);
                    }
                });
            }
        }
        if (convListener != null) convListener.onUpdate();
        calculateUnreadCount();
    }

    // 处理消息
    private MsgModel handleMsg(Core.MsgData msgData, AesParams aesParams) {
        MsgModel msgModel = MsgModel.fromProto(msgData, aesParams);
        msgModel.sendStatus = SendStatus.success;
        updateRecord(msgModel);
        updateMsg(msgModel);
        if (!msgData.getServerMsgId().isEmpty()) {
            updateRead(msgModel);
            updateMsgConv(msgModel);
        }
        return msgModel;
    }

    private void updateRecord(MsgModel msgModel) {
        Query<RecordModel> recordQuery = recordBox().query()
                .equal(
                        RecordModel_.convId, msgModel.convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        RecordModel recordModel = recordQuery.findFirst();
        recordQuery.close();
        if (recordModel == null) return;
        if (msgModel.seq > recordModel.seq) {
            recordModel.seq = msgModel.seq;
            recordBox().put(recordModel);
        }
    }

    private void updateMsg(MsgModel msgModel) {
        if (!msgModel.options.storageForClient) return;
        Query<MsgModel> msgQuery = msgBox().query()
                .equal(
                        MsgModel_.convId, msgModel.convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .and()
                .equal(
                        MsgModel_.clientMsgId, msgModel.clientMsgId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                ).build();
        MsgModel model = msgQuery.findFirst();
        msgQuery.close();
        if (model != null) {
            boolean updated = false;
            if (!TextUtils.equals(model.serverMsgId, msgModel.serverMsgId)) {
                model.serverMsgId = msgModel.serverMsgId;
                updated = true;
            }
            if (model.serverTime != msgModel.serverTime) {
                model.serverTime = msgModel.serverTime;
                updated = true;
            }
            if (model.contentType != msgModel.contentType) {
                model.contentType = msgModel.contentType;
                updated = true;
            }
            if (!TextUtils.equals(model.content, msgModel.content)) {
                model.content = msgModel.content;
                updated = true;
            }
            if (model.seq != msgModel.seq) {
                model.seq = msgModel.seq;
                updated = true;
            }
            if (updated) msgBox().put(model);
        } else {
            msgBox().put(msgModel);
        }
    }

    private void updateRead(MsgModel msgModel) {
        if (msgModel.contentType != ContentType.read) return;
        SDKContent.ReadContent content = SDKContent.ReadContent.fromJson(msgModel.content);
        Query<ReadModel> readQuery = readBox().query()
                .equal(
                        ReadModel_.senderId, msgModel.senderId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .and()
                .equal(
                        ReadModel_.convId, msgModel.convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                ).build();
        ReadModel readModel = readQuery.findFirst();
        readQuery.close();
        if (readModel != null) {
            if (content.seq > readModel.seq) {
                readModel.seq = content.seq;
                readBox().put(readModel);
            }
        } else {
            readModel = new ReadModel(
                    msgModel.senderId,
                    msgModel.convId,
                    content.seq
            );
            readBox().put(readModel);
        }
    }

    private void updateMsgConv(MsgModel msgModel) {
        Query<ConvModel> convQuery = convBox().query()
                .equal(
                        ConvModel_.convId, msgModel.convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                ).build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) {
            convModel = new ConvModel(msgModel.convId, ConvType.msg);
        }
        MsgModel model = null;
        if (convModel.clientMsgId != null) {
            Query<MsgModel> msgQuery = msgBox().query()
                    .equal(
                            MsgModel_.clientMsgId, convModel.clientMsgId,
                            QueryBuilder.StringOrder.CASE_SENSITIVE
                    ).build();
            model = msgQuery.findFirst();
            msgQuery.close();
        }
        if (msgModel.options.updateConvMsg) {
            if (model != null) {
                if (msgModel.seq > model.seq) {
                    convModel.clientMsgId = msgModel.clientMsgId;
                    convModel.time = msgModel.serverTime;
                    convModel.hidden = false;
                    convModel.deleted = false;
                }
            } else {
                convModel.clientMsgId = msgModel.clientMsgId;
                convModel.time = msgModel.serverTime;
                convModel.hidden = false;
                convModel.deleted = false;
            }
        }
        if (msgModel.options.updateUnreadCount &&
                !TextUtils.equals(msgModel.senderId, userId)) {
            Query<ReadModel> readQuery = readBox().query()
                    .equal(
                            ReadModel_.senderId, userId,
                            QueryBuilder.StringOrder.CASE_SENSITIVE
                    )
                    .and()
                    .equal(
                            ReadModel_.convId, msgModel.convId,
                            QueryBuilder.StringOrder.CASE_SENSITIVE
                    ).build();
            ReadModel readModel = readQuery.findFirst();
            readQuery.close();
            if (readModel != null) {
                if (msgModel.seq > readModel.seq) {
                    ++convModel.unreadCount;
                }
            } else {
                if (model != null) {
                    if (msgModel.seq > model.seq) {
                        ++convModel.unreadCount;
                    }
                } else {
                    ++convModel.unreadCount;
                }
            }
        }
        convBox().put(convModel);
    }

    // 处理通知
    private NoticeModel handleNotice(Core.NoticeData noticeData) {
        NoticeModel noticeModel = NoticeModel.fromProto(noticeData);
        updateNotice(noticeModel);
        updateNoticeConv(noticeModel);
        return noticeModel;
    }

    private void updateNotice(NoticeModel noticeModel) {
        if (!noticeModel.options.storageForClient) return;
        Query<NoticeModel> noticeQuery = noticeBox().query()
                .equal(
                        NoticeModel_.convId, noticeModel.convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .and()
                .equal(
                        NoticeModel_.noticeId, noticeModel.noticeId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        NoticeModel model = noticeQuery.findFirst();
        noticeQuery.close();
        if (model != null) {
            if (!TextUtils.equals(model.noticeId, noticeModel.noticeId)) {
                noticeBox().put(noticeModel);
            }
        } else {
            noticeBox().put(noticeModel);
        }
    }

    private void updateNoticeConv(NoticeModel noticeModel) {
        Query<ConvModel> convQuery = convBox().query()
                .equal(
                        ConvModel_.convId, noticeModel.convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) {
            convModel = new ConvModel(noticeModel.convId, ConvType.notice);
        }
        NoticeModel model = null;
        if (convModel.noticeId != null) {
            Query<NoticeModel> noticeQuery = noticeBox().query()
                    .equal(
                            NoticeModel_.noticeId, convModel.noticeId,
                            QueryBuilder.StringOrder.CASE_SENSITIVE
                    )
                    .build();
            model = noticeQuery.findFirst();
            noticeQuery.close();
        }
        if (noticeModel.options.updateConvMsg) {
            if (model != null) {
                if (!TextUtils.equals(model.noticeId, noticeModel.noticeId)) {
                    convModel.noticeId = noticeModel.noticeId;
                    convModel.time = noticeModel.createTime;
                    convModel.hidden = false;
                    convModel.deleted = false;
                }
            } else {
                convModel.noticeId = noticeModel.noticeId;
                convModel.time = noticeModel.createTime;
                convModel.hidden = false;
                convModel.deleted = false;
            }
        }
        ++convModel.unreadCount;
        convBox().put(convModel);
    }

    // 计算未读数量
    public void calculateUnreadCount() {
        Query<ConvModel> convQuery = convBox().query().build();
        long count = convQuery.property(ConvModel_.unreadCount).sum();
        convQuery.close();
        if (unreadListener != null) unreadListener.onUnreadCount(count);
    }

    // 创建消息
    public MsgModel createMsg(String senderInfo, String convId, List<String> atUsers, int contentType, String content,
                              MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext
    ) {
        return createMsg(null, null, senderInfo, convId, atUsers, contentType, content, options, offlinePush, ext);
    }

    // 创建消息
    public MsgModel createMsg(String clientMsgID, Long serverTime, String senderInfo, String convId, List<String> atUsers,
                              int contentType, String content, MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext
    ) {
        long timestamp = System.currentTimeMillis();
        long seq = 0;
        Query<MsgModel> msgQuery = msgBox().query()
                .equal(
                        MsgModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                ).build();
        MsgModel model = msgQuery.findFirst();
        msgQuery.close();
        if (model != null) {
            seq = ++model.seq;
        }
        if (clientMsgID == null) {
            clientMsgID = SDKTool.getUUId();
        }
        if (serverTime == null) {
            serverTime = timestamp;
        }
        MsgModel msgModel = new MsgModel(
                clientMsgID,
                serverTime,
                serverTime,
                userId,
                senderInfo,
                convId,
                atUsers,
                contentType,
                content,
                seq,
                options,
                offlinePush,
                ext
        );
        upsertMsg(msgModel, true);
        return msgModel;
    }

    // 发送消息列表
    public void sendMsgList(List<MsgModel> msgModelList, int deliverAfter,
                            OperateCallback<List<MsgModel>> callback
    ) {
        sendMsgList(null, msgModelList, deliverAfter, callback);
    }

    // 发送消息列表
    public void sendMsgList(String senderInfo, List<MsgModel> msgModelList, int deliverAfter,
                            OperateCallback<List<MsgModel>> callback
    ) {
        List<String> convIdList = new ArrayList<>();
        for (MsgModel msgModel : msgModelList) {
            convIdList.add(msgModel.convId);
        }
        Map<String, AesParams> convAesMap = subscribeCallback.onConvAesParams(convIdList);
        Core.SendMsgListReq.Builder builder = Core.SendMsgListReq.newBuilder();
        for (MsgModel msgModel : msgModelList) {
            if (senderInfo != null) {
                msgModel.senderInfo = senderInfo;
            }
            ByteString content = ByteString.copyFromUtf8(msgModel.content);
            AesParams aesParams = convAesMap.get(msgModel.convId);
            if (msgModel.options.needDecrypt && aesParams != null) {
                content = ByteString.copyFrom(
                        SDKTool.aesEncode(aesParams.key, aesParams.iv, msgModel.content)
                );
            }
            Core.MsgData.Options options = Core.MsgData.Options.newBuilder()
                    .setStorageForServer(msgModel.options.storageForServer)
                    .setStorageForClient(msgModel.options.storageForClient)
                    .setNeedDecrypt(msgModel.options.needDecrypt)
                    .setOfflinePush(msgModel.options.offlinePush)
                    .setUpdateConvMsg(msgModel.options.updateConvMsg)
                    .setUpdateUnreadCount(msgModel.options.updateUnreadCount)
                    .build();
            Core.MsgData.OfflinePush offlinePush = Core.MsgData.OfflinePush.newBuilder()
                    .setTitle(msgModel.offlinePush.title)
                    .setContent(msgModel.offlinePush.content)
                    .setPayload(msgModel.offlinePush.payload)
                    .build();
            Core.MsgData msgData = Core.MsgData.newBuilder()
                    .setClientMsgId(msgModel.clientMsgId)
                    .setClientTime(String.valueOf(msgModel.clientTime))
                    .setServerTime(String.valueOf(msgModel.serverTime))
                    .setSenderId(msgModel.senderId)
                    .setSenderInfo(ByteString.copyFromUtf8(msgModel.senderInfo))
                    .setConvId(msgModel.convId)
                    .addAllAtUsers(msgModel.atUsers)
                    .setContentType(msgModel.contentType)
                    .setContent(content)
                    .setSeq(String.valueOf(msgModel.seq))
                    .setOptions(options)
                    .setOfflinePush(offlinePush)
                    .setExt(ByteString.copyFromUtf8(msgModel.ext))
                    .build();
            builder.addMsgDataList(msgData);
        }
        builder.setDeliverAfter(deliverAfter);
        xximCore.sendMsgList(SDKTool.getUUId(), builder.build(), new RequestCallback<Core.SendMsgListResp>() {
            @Override
            public void onSuccess(Core.SendMsgListResp resp) {
                for (MsgModel msgModel : msgModelList) {
                    msgModel.sendStatus = SendStatus.success;
                    upsertMsg(msgModel, true);
                }
                callback.onSuccess(msgModelList);
            }

            @Override
            public void onError(int code, String error) {
                for (MsgModel msgModel : msgModelList) {
                    msgModel.sendStatus = SendStatus.failed;
                    upsertMsg(msgModel, true);
                }
                callback.onError(code, error);
            }
        });
    }

    public void upsertMsg(MsgModel msgModel, boolean includeMsgConv) {
        if (msgModel.options.storageForClient) {
            msgBox().put(msgModel);
        }
        if (includeMsgConv) updateMsgConv(msgModel);
    }
}
