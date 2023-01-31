package chat.cherish.xxim.sdk.manager;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

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
    private final SDKManager sdkManager;

    public MsgManager(SDKManager sdkManager) {
        this.sdkManager = sdkManager;
    }

    // 获取消息列表
    public void getMsgList(String convId, Integer contentType, Long maxSeq, int size,
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
        long minSeq = maxSeq - size;
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
        List<MsgModel> list = queryMsgList(
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
                long finalMinSeq = minSeq;
                long finalMaxSeq = maxSeq;
                sdkManager.pullMsgDataList(items, new RequestCallback<Core.GetMsgListResp>() {
                    @Override
                    public void onSuccess(Core.GetMsgListResp resp) {
                        callback.onSuccess(queryMsgList(
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
                        callback.onSuccess(queryMsgList(
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
        callback.onSuccess(queryMsgList(
                convId,
                contentType,
                minSeq,
                maxSeq,
                includeUpper,
                false
        ));
    }

    private List<MsgModel> queryMsgList(String convId, Integer contentType,
                                        long minSeq, long maxSeq, boolean includeUpper, Boolean deleted
    ) {
        QueryBuilder<MsgModel> msgBuilder = sdkManager.msgBox().query();
        msgBuilder.equal(
                MsgModel_.convId, convId,
                QueryBuilder.StringOrder.CASE_SENSITIVE
        );
        if (contentType != null) {
            msgBuilder.and().equal(MsgModel_.contentType, contentType);
        }
        List<String> seqList = SDKTool.generateSeqList(minSeq, includeUpper ? maxSeq : maxSeq - 1);
        long[] values = new long[seqList.size()];
        for (int i = 0; i < seqList.size(); i++) {
            values[i] = Long.parseLong(seqList.get(i));
        }
        msgBuilder.and().in(MsgModel_.seq, values);
        if (deleted != null) {
            msgBuilder.and().equal(MsgModel_.deleted, deleted);
        }
        msgBuilder.orderDesc(MsgModel_.seq);
        Query<MsgModel> msgQuery = msgBuilder.build();
        List<MsgModel> msgModelList = msgQuery.find();
        msgQuery.close();
        return msgModelList;
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
        int index = 0;
        for (String clientMsgId : clientMsgIdList) {
            ++index;
            msgBuilder.equal(
                    MsgModel_.clientMsgId, clientMsgId,
                    QueryBuilder.StringOrder.CASE_SENSITIVE
            );
            if (index < clientMsgIdList.size()) {
                msgBuilder.or();
            }
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

    // 发送已读消息
    public void sendRead(String convId, SDKContent.ReadContent content,
                         OperateCallback<List<MsgModel>> callback) {
        sendRead(convId, content, true, true, "", callback);
    }

    // 发送已读消息
    public void sendRead(String convId, SDKContent.ReadContent content, boolean storageForServer, boolean storageForClient,
                         String ext, OperateCallback<List<MsgModel>> callback) {
        List<MsgModel> msgModelList = new ArrayList<>();
        msgModelList.add(
                sdkManager.createMsg(
                        null,
                        convId,
                        new ArrayList<>(),
                        ContentType.read,
                        content.toJson(),
                        new MsgModel.MsgOptionsModel(
                                storageForServer, storageForClient, false, false, false, false
                        ),
                        new MsgModel.MsgOfflinePushModel(
                                "", "", ""
                        ),
                        ext
                )
        );
        sendMsgList(msgModelList, 0, callback);
    }

    // 发送撤回消息
    public void sendRevoke(String clientMsgId, SDKContent.RevokeContent content,
                           OperateCallback<List<MsgModel>> callback) {
        sendRevoke(clientMsgId, content, "", callback);
    }

    // 发送撤回消息
    public void sendRevoke(String clientMsgId, SDKContent.RevokeContent content, String ext,
                           OperateCallback<List<MsgModel>> callback) {
        MsgModel msgModel = getSingleMsg(clientMsgId);
        if (msgModel == null) return;
        content.contentType = msgModel.contentType;
        content.content = msgModel.content;
        msgModel.contentType = ContentType.revoke;
        msgModel.content = content.toJson();
        msgModel.offlinePush.content = content.content;
        msgModel.ext = ext;
        List<MsgModel> msgModelList = new ArrayList<>();
        msgModelList.add(msgModel);
        sendMsgList(msgModelList, 0, callback);
    }

    // 发送提示消息
    public void sendTip(String convId, SDKContent.TipContent content,
                        OperateCallback<List<MsgModel>> callback) {
        sendTip(convId, content, "", callback);
    }

    // 发送提示消息
    public void sendTip(String convId, SDKContent.TipContent content, String ext,
                        OperateCallback<List<MsgModel>> callback) {
        List<MsgModel> msgModelList = new ArrayList<>();
        msgModelList.add(
                sdkManager.createMsg(
                        null,
                        convId,
                        new ArrayList<>(),
                        ContentType.tip,
                        content.toJson(),
                        new MsgModel.MsgOptionsModel(
                                true, true, false, false, false, false
                        ),
                        new MsgModel.MsgOfflinePushModel(
                                "", "", ""
                        ),
                        ext
                )
        );
        sendMsgList(msgModelList, 0, callback);
    }

    // 创建文本消息
    public MsgModel createText(String senderInfo, String convId, List<String> atUsers, String text,
                               MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.text, text, options, offlinePush, ext);
    }

    // 创建图片消息
    public MsgModel createImage(String senderInfo, String convId, List<String> atUsers, SDKContent.ImageContent content,
                                MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.image, content.toJson(), options, offlinePush, ext);
    }

    // 创建语音消息
    public MsgModel createAudio(String senderInfo, String convId, List<String> atUsers, SDKContent.AudioContent content,
                                MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.audio, content.toJson(), options, offlinePush, ext);
    }

    // 创建视频消息
    public MsgModel createVideo(String senderInfo, String convId, List<String> atUsers, SDKContent.VideoContent content,
                                MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.video, content.toJson(), options, offlinePush, ext);
    }

    // 创建文件消息
    public MsgModel createFile(String senderInfo, String convId, List<String> atUsers, SDKContent.FileContent content,
                               MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.file, content.toJson(), options, offlinePush, ext);
    }

    // 创建位置消息
    public MsgModel createLocation(String senderInfo, String convId, List<String> atUsers, SDKContent.LocationContent content,
                                   MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.location, content.toJson(), options, offlinePush, ext);
    }

    // 创建名片消息
    public MsgModel createCard(String senderInfo, String convId, List<String> atUsers, SDKContent.CardContent content,
                               MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.card, content.toJson(), options, offlinePush, ext);
    }

    // 创建合并消息
    public MsgModel createMerge(String senderInfo, String convId, List<String> atUsers, SDKContent.MergeContent content,
                                MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.merge, content.toJson(), options, offlinePush, ext);
    }

    // 创建表情消息
    public MsgModel createEmoji(String senderInfo, String convId, List<String> atUsers, SDKContent.EmojiContent content,
                                MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.emoji, content.toJson(), options, offlinePush, ext);
    }

    // 创建表情消息
    public MsgModel createCommand(String senderInfo, String convId, List<String> atUsers, SDKContent.CommandContent content,
                                  MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.command, content.toJson(), options, offlinePush, ext);
    }

    // 创建富文本消息
    public MsgModel createRichTxt(String senderInfo, String convId, List<String> atUsers, SDKContent.RichTxtContent content,
                                  MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.richTxt, content.toJson(), options, offlinePush, ext);
    }

    // 创建标记消息
    public MsgModel createMarkdown(String senderInfo, String convId, List<String> atUsers, SDKContent.MarkdownContent content,
                                   MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.markdown, content.toJson(), options, offlinePush, ext);
    }

    // 创建自定义消息
    public MsgModel createCustom(String senderInfo, String convId, List<String> atUsers, SDKContent.CustomContent content,
                                 MsgModel.MsgOptionsModel options, MsgModel.MsgOfflinePushModel offlinePush, String ext) {
        if (options == null) {
            options = new MsgModel.MsgOptionsModel(
                    true, true, true, true, true, true
            );
        }
        return sdkManager.createMsg(senderInfo, convId, atUsers, ContentType.custom, content.toJson(), options, offlinePush, ext);
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
