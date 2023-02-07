package chat.cherish.xxim.sdk.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.cherish.xxim.sdk.callback.OperateCallback;
import chat.cherish.xxim.sdk.common.ConvType;
import chat.cherish.xxim.sdk.model.ConvModel;
import chat.cherish.xxim.sdk.model.ConvModel_;
import chat.cherish.xxim.sdk.model.MsgModel;
import chat.cherish.xxim.sdk.model.MsgModel_;
import chat.cherish.xxim.sdk.model.NoticeModel;
import chat.cherish.xxim.sdk.model.NoticeModel_;
import chat.cherish.xxim.sdk.model.SDKContent;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;

public class ConvManager {
    private final SDKManager sdkManager;
    private final MsgManager msgManager;
    private final NoticeManager noticeManager;

    public ConvManager(SDKManager sdkManager, MsgManager msgManager, NoticeManager noticeManager) {
        this.sdkManager = sdkManager;
        this.msgManager = msgManager;
        this.noticeManager = noticeManager;
    }

    // 获取会话列表
    public List<ConvModel> getConvList() {
        List<ConvModel> convList = queryConvList();
        if (convList.isEmpty()) return convList;
        List<String> clientMsgIdList = new ArrayList<>();
        List<String> noticeIdList = new ArrayList<>();
        for (ConvModel convModel : convList) {
            if (convModel.clientMsgId != null) {
                clientMsgIdList.add(convModel.clientMsgId);
            }
            if (convModel.noticeId != null) {
                noticeIdList.add(convModel.noticeId);
            }
        }
        Map<String, MsgModel> msgMap = new HashMap<>();
        Map<String, NoticeModel> noticeMap = new HashMap<>();
        if (!clientMsgIdList.isEmpty()) {
            List<MsgModel> msgModelList = msgManager.getMultipleMsg(
                    clientMsgIdList
            );
            for (MsgModel msgModel : msgModelList) {
                msgMap.put(msgModel.clientMsgId, msgModel);
            }
        }
        if (!noticeIdList.isEmpty()) {
            List<NoticeModel> noticeModelList = noticeManager.getMultipleNotice(
                    noticeIdList
            );
            for (NoticeModel noticeModel : noticeModelList) {
                noticeMap.put(noticeModel.noticeId, noticeModel);
            }
        }
        for (ConvModel convModel : convList) {
            convModel.msgModel = msgMap.get(convModel.clientMsgId);
            convModel.noticeModel = noticeMap.get(convModel.noticeId);
        }
        return convList;
    }

    private List<ConvModel> queryConvList() {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(ConvModel_.hidden, false)
                .and()
                .equal(ConvModel_.deleted, false)
                .orderDesc(ConvModel_.draftModel)
                .orderDesc(ConvModel_.time)
                .build();
        List<ConvModel> convModelList = convQuery.find();
        convQuery.close();
        return convModelList;
    }

    // 获取单条会话
    public ConvModel getSingleConv(String convId) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel != null) {
            if (convModel.clientMsgId != null) {
                convModel.msgModel = msgManager.getSingleMsg(
                        convModel.clientMsgId
                );
            }
            if (convModel.noticeId != null) {
                convModel.noticeModel = noticeManager.getSingleNotice(
                        convModel.noticeId
                );
            }
        }
        return convModel;
    }

    // 设置会话已读
    public void setConvRead(String convId) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        if (convModel.unreadCount == 0) return;
        convModel.unreadCount = 0;
        sdkManager.convBox().put(convModel);
        sdkManager.calculateUnreadCount();
        if (convModel.convType != ConvType.msg) return;
        MsgModel msgModel = msgManager.getFirstMsg(convId);
        if (msgModel == null) return;
        msgManager.sendReadMsg(
                new SDKContent.ReadContent(
                        convId, msgModel.seq
                ),
                new OperateCallback<Boolean>() {
                }
        );
    }

    // 更新会话消息
    public void updateConvMsg(String convId) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        Query<MsgModel> msgQuery = sdkManager.msgBox().query()
                .equal(
                        MsgModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .and()
                .equal(MsgModel_.deleted, false)
                .orderDesc(MsgModel_.seq)
                .build();
        MsgModel msgModel = msgQuery.findFirst();
        msgQuery.close();
        if (msgModel == null) return;
        convModel.clientMsgId = msgModel.clientMsgId;
        convModel.time = msgModel.serverTime;
        convModel.msgModel = msgModel;
        sdkManager.convBox().put(convModel);
        sdkManager.calculateUnreadCount();
    }

    // 删除会话消息
    public void deleteConvMsg(String convId) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        convModel.clientMsgId = null;
        convModel.time = 0;
        convModel.msgModel = null;
        sdkManager.convBox().put(convModel);
        sdkManager.calculateUnreadCount();
    }

    // 更新会话通知
    public void updateConvNotice(String convId) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        Query<NoticeModel> noticeQuery = sdkManager.noticeBox().query()
                .equal(
                        NoticeModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .and()
                .equal(NoticeModel_.deleted, false)
                .orderDesc(NoticeModel_.createTime)
                .build();
        NoticeModel noticeModel = noticeQuery.findFirst();
        noticeQuery.close();
        if (noticeModel == null) return;
        convModel.noticeId = noticeModel.noticeId;
        convModel.time = noticeModel.createTime;
        convModel.noticeModel = noticeModel;
        sdkManager.convBox().put(convModel);
        sdkManager.calculateUnreadCount();
    }

    // 删除会话通知
    public void deleteConvNotice(String convId) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        convModel.noticeId = null;
        convModel.time = 0;
        convModel.noticeModel = null;
        sdkManager.convBox().put(convModel);
        sdkManager.calculateUnreadCount();
    }

    // 设置会话草稿
    public void setConvDraft(String convId, ConvModel.DraftModel draftModel) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        convModel.draftModel = draftModel;
        sdkManager.convBox().put(convModel);
    }

    // 设置会话隐藏
    public void setConvHidden(String convId, boolean hidden) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        convModel.unreadCount = 0;
        convModel.hidden = hidden;
        sdkManager.convBox().put(convModel);
    }

    // 删除会话
    public void deleteConv(String convId, boolean clear) {
        Query<ConvModel> convQuery = sdkManager.convBox().query()
                .equal(
                        ConvModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        ConvModel convModel = convQuery.findFirst();
        convQuery.close();
        if (convModel == null) return;
        if (clear) {
            convModel.clientMsgId = null;
            convModel.noticeId = null;
            convModel.time = 0;
            convModel.msgModel = null;
            convModel.noticeModel = null;
        }
        convModel.unreadCount = 0;
        convModel.draftModel = null;
        convModel.hidden = false;
        convModel.deleted = true;
        sdkManager.convBox().put(convModel);
        sdkManager.calculateUnreadCount();
        if (!clear) return;
        if (convModel.convType == ConvType.msg) {
            msgManager.clearMsg(convId);
        } else if (convModel.convType == ConvType.notice) {
            noticeManager.clearNotice(convId);
        }
    }

    // 获取未读数量
    public long getUnreadCount() {
        Query<ConvModel> convQuery = sdkManager.convBox().query().build();
        long count = convQuery.property(ConvModel_.unreadCount).sum();
        convQuery.close();
        return count;
    }
}
