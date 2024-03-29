package chat.cherish.xxim.sdk.model;

import java.util.List;

import chat.cherish.xxim.sdk.common.AesParams;
import chat.cherish.xxim.sdk.common.SendStatus;
import chat.cherish.xxim.sdk.model.converter.MsgOfflineConverter;
import chat.cherish.xxim.sdk.model.converter.MsgOptionsConverter;
import chat.cherish.xxim.sdk.tool.SDKTool;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import pb.Core;

@Entity
public class MsgModel {
    @Id
    public long id;

    @Index()
    public String clientMsgId;
    public String serverMsgId;
    public long clientTime;
    public long serverTime;
    @Index()
    public String senderId;
    public String senderInfo;
    @Index()
    public String convId;
    public List<String> atUsers;
    @Index()
    public int contentType;
    @Index()
    public String content;
    public long seq;
    @Convert(converter = MsgOptionsConverter.class, dbType = String.class)
    public MsgOptionsModel options;
    @Convert(converter = MsgOfflineConverter.class, dbType = String.class)
    public MsgOfflinePushModel offlinePush;
    public String ext;
    public int sendStatus;
    public int sendProgress;
    public boolean deleted;

    public MsgModel() {
    }

    public MsgModel(String clientMsgId, long clientTime, long serverTime, String senderId, String senderInfo,
                    String convId, List<String> atUsers, int contentType, String content, long seq,
                    MsgOptionsModel options, MsgOfflinePushModel offlinePush, String ext
    ) {
        this.clientMsgId = clientMsgId;
        this.clientTime = clientTime;
        this.serverTime = serverTime;
        this.senderId = senderId;
        this.senderInfo = senderInfo;
        this.convId = convId;
        this.atUsers = atUsers;
        this.contentType = contentType;
        this.content = content;
        this.seq = seq;
        this.options = options;
        this.offlinePush = offlinePush;
        this.ext = ext;
        this.sendStatus = SendStatus.sending;
        this.sendProgress = 0;
        this.deleted = false;
    }

    public MsgModel(String clientMsgId, String serverMsgId, long clientTime, long serverTime, String senderId,
                    String senderInfo, String convId, List<String> atUsers, int contentType, String content,
                    long seq, MsgOptionsModel options, MsgOfflinePushModel offlinePush, String ext
    ) {
        this.clientMsgId = clientMsgId;
        this.serverMsgId = serverMsgId;
        this.clientTime = clientTime;
        this.serverTime = serverTime;
        this.senderId = senderId;
        this.senderInfo = senderInfo;
        this.convId = convId;
        this.atUsers = atUsers;
        this.contentType = contentType;
        this.content = content;
        this.seq = seq;
        this.options = options;
        this.offlinePush = offlinePush;
        this.ext = ext;
        this.sendStatus = SendStatus.sending;
        this.sendProgress = 0;
        this.deleted = false;
    }

    public static MsgModel fromProto(Core.MsgData msgData, AesParams aesParams) {
        MsgOptionsModel options = new MsgOptionsModel(
                msgData.getOptions().getStorageForServer(),
                msgData.getOptions().getStorageForClient(),
                msgData.getOptions().getNeedDecrypt(),
                msgData.getOptions().getOfflinePush(),
                msgData.getOptions().getUpdateConvMsg(),
                msgData.getOptions().getUpdateUnreadCount()
        );
        MsgOfflinePushModel offlinePush = new MsgOfflinePushModel(
                msgData.getOfflinePush().getTitle(),
                msgData.getOfflinePush().getContent(),
                msgData.getOfflinePush().getPayload()
        );
        String content = msgData.getContent().toStringUtf8();
        if (options.needDecrypt && aesParams != null) {
            content = SDKTool.aesDecode(
                    aesParams.key,
                    aesParams.iv,
                    msgData.getContent().toByteArray()
            );
        }
        return new MsgModel(
                msgData.getClientMsgId(),
                msgData.getServerMsgId(),
                Long.parseLong(msgData.getClientTime()),
                Long.parseLong(msgData.getServerTime()),
                msgData.getSenderId(),
                msgData.getSenderInfo().toStringUtf8(),
                msgData.getConvId(),
                msgData.getAtUsersList(),
                msgData.getContentType(),
                content,
                Long.parseLong(msgData.getSeq()),
                options,
                offlinePush,
                msgData.getExt().toStringUtf8()
        );
    }

    @Override
    public String toString() {
        return "MsgModel{" +
                "clientMsgId='" + clientMsgId + '\'' +
                ", serverMsgId='" + serverMsgId + '\'' +
                ", clientTime=" + clientTime +
                ", serverTime=" + serverTime +
                ", senderId='" + senderId + '\'' +
                ", senderInfo='" + senderInfo + '\'' +
                ", convId='" + convId + '\'' +
                ", atUsers=" + atUsers +
                ", contentType=" + contentType +
                ", content='" + content + '\'' +
                ", seq=" + seq +
                ", options=" + options +
                ", offlinePush=" + offlinePush +
                ", ext='" + ext + '\'' +
                ", sendStatus=" + sendStatus +
                ", sendProgress=" + sendProgress +
                ", deleted=" + deleted +
                '}';
    }

    public static class MsgOptionsModel {
        public boolean storageForServer;
        public boolean storageForClient;
        public boolean needDecrypt;
        public boolean offlinePush;
        public boolean updateConvMsg;
        public boolean updateUnreadCount;

        public MsgOptionsModel() {
        }

        public MsgOptionsModel(boolean storageForServer, boolean storageForClient, boolean needDecrypt, boolean offlinePush, boolean updateConvMsg, boolean updateUnreadCount) {
            this.storageForServer = storageForServer;
            this.storageForClient = storageForClient;
            this.needDecrypt = needDecrypt;
            this.offlinePush = offlinePush;
            this.updateConvMsg = updateConvMsg;
            this.updateUnreadCount = updateUnreadCount;
        }

        @Override
        public String toString() {
            return "MsgOptionsModel{" +
                    "storageForServer=" + storageForServer +
                    ", storageForClient=" + storageForClient +
                    ", needDecrypt=" + needDecrypt +
                    ", offlinePush=" + offlinePush +
                    ", updateConvMsg=" + updateConvMsg +
                    ", updateUnreadCount=" + updateUnreadCount +
                    '}';
        }
    }

    public static class MsgOfflinePushModel {
        public String title;
        public String content;
        public String payload;

        public MsgOfflinePushModel() {
        }

        public MsgOfflinePushModel(String title, String content, String payload) {
            this.title = title;
            this.content = content;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "MsgOfflinePushModel{" +
                    "title='" + title + '\'' +
                    ", content='" + content + '\'' +
                    ", payload='" + payload + '\'' +
                    '}';
        }
    }
}
