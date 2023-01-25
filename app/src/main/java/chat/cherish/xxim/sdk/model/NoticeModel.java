package chat.cherish.xxim.sdk.model;

import chat.cherish.xxim.sdk.model.converter.NoticeOptionsConverter;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import pb.Core;

@Entity
public class NoticeModel {
    @Id
    public long id;

    @Index()
    public String convId;
    @Index()
    public String noticeId;
    public int createTime;
    public String title;
    public int contentType;
    public String content;
    @Convert(converter = NoticeOptionsConverter.class, dbType = String.class)
    public NoticeOptionsModel options;
    public String ext;
    public boolean deleted;

    public NoticeModel() {
    }

    public NoticeModel(String convId, String noticeId, int createTime, String title, int contentType,
                       String content, NoticeOptionsModel options, String ext
    ) {
        this.convId = convId;
        this.noticeId = noticeId;
        this.createTime = createTime;
        this.title = title;
        this.contentType = contentType;
        this.content = content;
        this.options = options;
        this.ext = ext;
        this.deleted = false;
    }

    public static NoticeModel fromProto(Core.NoticeData noticeData) {
        NoticeOptionsModel options = new NoticeOptionsModel(
                noticeData.getOptions().getStorageForClient(),
                noticeData.getOptions().getUpdateConvMsg()
        );
        return new NoticeModel(
                noticeData.getConvId(),
                noticeData.getNoticeId(),
                Integer.parseInt(noticeData.getCreateTime()),
                noticeData.getTitle(),
                noticeData.getContentType(),
                noticeData.getContent().toStringUtf8(),
                options,
                noticeData.getExt().toStringUtf8()
        );
    }

    public static class NoticeOptionsModel {
        public boolean storageForClient;
        public boolean updateConvMsg;

        public NoticeOptionsModel() {
        }

        public NoticeOptionsModel(boolean storageForClient, boolean updateConvMsg) {
            this.storageForClient = storageForClient;
            this.updateConvMsg = updateConvMsg;
        }
    }
}
