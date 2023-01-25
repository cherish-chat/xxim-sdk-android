package chat.cherish.xxim.sdk.model;

import java.util.List;

import chat.cherish.xxim.sdk.common.AESParams;
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
    public int clientTime;
    public int serverTime;
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
    public int seq;
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

    public MsgModel(String clientMsgId, int clientTime, int serverTime, String senderId, String senderInfo,
                    String convId, List<String> atUsers, int contentType, String content, int seq,
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

    public MsgModel(String clientMsgId, String serverMsgId, int clientTime, int serverTime, String senderId,
                    String senderInfo, String convId, List<String> atUsers, int contentType, String content,
                    int seq, MsgOptionsModel options, MsgOfflinePushModel offlinePush, String ext
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

    public static MsgModel fromProto(Core.MsgData msgData, AESParams aesParams) {
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
                Integer.parseInt(msgData.getClientTime()),
                Integer.parseInt(msgData.getServerTime()),
                msgData.getSenderId(),
                msgData.getSenderInfo().toStringUtf8(),
                msgData.getConvId(),
                msgData.getAtUsersList(),
                msgData.getContentType(),
                content,
                Integer.parseInt(msgData.getSeq()),
                options,
                offlinePush,
                msgData.getExt().toStringUtf8()
        );
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
    }
}
