package chat.cherish.xxim.sdk.manager;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import chat.cherish.xxim.core.callback.RequestCallback;
import chat.cherish.xxim.sdk.callback.OperateCallback;
import chat.cherish.xxim.sdk.common.ContentType;
import chat.cherish.xxim.sdk.model.MsgModel;
import chat.cherish.xxim.sdk.model.MsgModel_;
import chat.cherish.xxim.sdk.model.RecordModel;
import chat.cherish.xxim.sdk.model.RecordModel_;
import chat.cherish.xxim.sdk.model.SDKContent;
import chat.cherish.xxim.sdk.tool.SDKTool;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import pb.Core;

public class MsgManager {
    private SDKManager sdkManager;

    public MsgManager(SDKManager sdkManager) {
        this.sdkManager = sdkManager;
    }

    // 获取消息列表
    public void getMsgList(String convId, Integer contentType, Integer maxSeq, int size,
                           OperateCallback<List<MsgModel>> callback
    ) {
        boolean includeUpper = maxSeq == null;
        if (maxSeq == null) {
            MsgModel msgModel = getFirstMsg(convId);
            if (msgModel != null) {
                maxSeq = msgModel.seq;
            } else {
                Query<RecordModel> recordQuery = sdkManager.recordBox().query()
                        .equal(
                                RecordModel_.convId, convId,
                                QueryBuilder.StringOrder.CASE_SENSITIVE
                        )
                        .build();
                RecordModel recordModel = recordQuery.findFirst();
                recordQuery.close();
                if (recordModel != null) {
                    maxSeq = recordModel.maxSeq;
                }
            }
        }
        if (maxSeq == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        Query<RecordModel> recordQuery = sdkManager.recordBox().query()
                .equal(
                        RecordModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        RecordModel recordModel = recordQuery.findFirst();
        recordQuery.close();
        int minSeq = maxSeq - size;
        if (recordModel != null) {
            minSeq = Math.max(minSeq, recordModel.minSeq);
        } else {
            minSeq = Math.max(minSeq, 0);
        }
        if (minSeq < 0) minSeq = 0;
        if (maxSeq <= minSeq) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        List<String> expectList = SDKTool.generateSeqList(minSeq, maxSeq);
        List<MsgModel> list = getMsgList(
                convId,
                contentType,
                minSeq,
                maxSeq,
                true,
                null
        );
        if (expectList.size() - list.size() != 0) {
            List<String> seqList = new ArrayList<>();
            for (String seq : expectList) {
                int index = -1;
                for (int i = 0; i < list.size(); i++) {
                    if (TextUtils.equals(seq, String.valueOf(list.get(i).seq))) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    seqList.add(seq);
                }
            }
            if (!seqList.isEmpty()) {
                List<Core.BatchGetMsgListByConvIdReq.Item> items = new ArrayList<>();
                items.add(Core.BatchGetMsgListByConvIdReq.Item.newBuilder()
                        .setConvId(convId)
                        .addAllSeqList(seqList)
                        .build());
                int finalMinSeq = minSeq;
                Integer finalMaxSeq = maxSeq;
                sdkManager.pullMsgDataList(items, new RequestCallback<Core.GetMsgListResp>() {
                    @Override
                    public void onSuccess(Core.GetMsgListResp resp) {
                        callback.onSuccess(getMsgList(
                                convId,
                                contentType,
                                finalMinSeq,
                                finalMaxSeq,
                                includeUpper,
                                false
                        ));
                    }

                    @Override
                    public void onError(int code, String error) {
                        callback.onSuccess(getMsgList(
                                convId,
                                contentType,
                                finalMinSeq,
                                finalMaxSeq,
                                includeUpper,
                                false
                        ));
                    }
                });
                return;
            }
        }
        callback.onSuccess(getMsgList(
                convId,
                contentType,
                minSeq,
                maxSeq,
                includeUpper,
                false
        ));
    }

    private List<MsgModel> getMsgList(String convId, Integer contentType,
                                      int minSeq, int maxSeq, boolean includeUpper, Boolean deleted
    ) {
        QueryBuilder<MsgModel> msgBuilder = sdkManager.msgBox().query();
        msgBuilder.equal(
                MsgModel_.convId, convId,
                QueryBuilder.StringOrder.CASE_SENSITIVE
        );
        if (contentType != null) {
            msgBuilder.and().equal(
                    MsgModel_.contentType, contentType
            );
        }
        msgBuilder.and().less(MsgModel_.seq, minSeq);
        if (includeUpper) {
            msgBuilder.and().greaterOrEqual(MsgModel_.seq, maxSeq);
        } else {
            msgBuilder.and().greater(MsgModel_.seq, maxSeq);
        }
        if (deleted != null) {
            msgBuilder.and().equal(
                    MsgModel_.deleted, deleted
            );
        }
        msgBuilder.orderDesc(MsgModel_.seq);
        return msgBuilder.build().find();
    }

    // 获取首个消息
    public MsgModel getFirstMsg(String convId) {
        Query<MsgModel> msgQuery = sdkManager.msgBox().query()
                .equal(
                        MsgModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .orderDesc(MsgModel_.seq)
                .build();
        MsgModel msgModel = msgQuery.findFirst();
        msgQuery.close();
        return msgModel;
    }

    // 获取单条消息
    public MsgModel getSingleMsg(String clientMsgId) {
        Query<MsgModel> msgQuery = sdkManager.msgBox().query()
                .equal(
                        MsgModel_.clientMsgId, clientMsgId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        MsgModel msgModel = msgQuery.findFirst();
        msgQuery.close();
        return msgModel;
    }

    // 获取多条消息
    public List<MsgModel> getMultipleMsg(List<String> clientMsgIdList) {
        QueryBuilder<MsgModel> msgBuilder = sdkManager.msgBox().query();
        for (String clientMsgId : clientMsgIdList) {
            msgBuilder.equal(
                    MsgModel_.clientMsgId, clientMsgId,
                    QueryBuilder.StringOrder.CASE_SENSITIVE
            ).and();
        }
        Query<MsgModel> msgQuery = msgBuilder.build();
        List<MsgModel> msgModelList = msgQuery.find();
        msgQuery.close();
        return msgModelList;
    }

    // 拉取云端消息
    public void pullCloudMsg(String clientMsgId, OperateCallback<MsgModel> callback) {
        sdkManager.pullMsgDataById(null, clientMsgId, callback);
    }

    // 发送正在输入
    public void sendTyping(String convId, SDKContent.TypingContent content,
                           OperateCallback<List<MsgModel>> callback) {
        sendTyping(convId, content, "", callback);
    }

    // 发送正在输入
    public void sendTyping(String convId, SDKContent.TypingContent content, String ext,
                           OperateCallback<List<MsgModel>> callback) {
        List<MsgModel> msgModelList = new ArrayList<>();
        msgModelList.add(
                sdkManager.createMsg(
                        null,
                        convId,
                        new ArrayList<>(),
                        ContentType.typing,
                        content.toJson(),
                        new MsgModel.MsgOptionsModel(
                                false, false, false, false, false, false
                        ),
                        new MsgModel.MsgOfflinePushModel(
                                "", "", ""
                        ),
                        ext
                )
        );
        sendMsgList(msgModelList, 0, callback);
    }

    // 发送消息列表
    public void sendMsgList(List<MsgModel> msgModelList, int deliverAfter,
                            OperateCallback<List<MsgModel>> callback) {
        sdkManager.sendMsgList(msgModelList, deliverAfter, callback);
    }

    // 发送消息列表
    public void sendMsgList(String senderInfo, List<MsgModel> msgModelList, int deliverAfter,
                            OperateCallback<List<MsgModel>> callback) {
        sdkManager.sendMsgList(senderInfo, msgModelList, deliverAfter, callback);
    }

    // 更新消息
    public void upsertMsg(MsgModel msgModel, boolean includeMsgConv) {
        sdkManager.upsertMsg(msgModel, includeMsgConv);
    }

    // 删除消息
    public void deleteMsg(String clientMsgId) {
        Query<MsgModel> msgQuery = sdkManager.msgBox().query()
                .equal(
                        MsgModel_.clientMsgId, clientMsgId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        MsgModel msgModel = msgQuery.findFirst();
        msgQuery.close();
        if (msgModel == null) return;
        msgModel.contentType = ContentType.unknown;
        msgModel.content = "";
        msgModel.deleted = true;
        sdkManager.msgBox().put(msgModel);
    }

    // 清空消息
    public void clearMsg(String convId) {
        Query<MsgModel> msgQuery = sdkManager.msgBox().query()
                .equal(
                        MsgModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        List<MsgModel> list = msgQuery.find();
        msgQuery.close();
        if (list.isEmpty()) return;
        for (MsgModel msgModel : list) {
            msgModel.contentType = ContentType.unknown;
            msgModel.content = "";
            msgModel.deleted = true;
        }
        sdkManager.msgBox().put(list);
    }

}
