package chat.cherish.xxim.sdk.model;

import chat.cherish.xxim.sdk.model.converter.DraftConverter;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Transient;

@Entity
public class ConvModel {
    @Id
    public long id;

    @Index()
    public String convId;
    @Index()
    public int convType;
    @Index()
    public String clientMsgId;
    @Index()
    public String noticeId;
    public long time;
    @Index()
    public long unreadCount;
    @Convert(converter = DraftConverter.class, dbType = String.class)
    public DraftModel draftModel;
    public boolean hidden;
    public boolean deleted;

    @Transient
    public MsgModel msgModel;
    @Transient
    public NoticeModel noticeModel;

    public ConvModel() {
    }

    public ConvModel(String convId, int convType) {
        this.convId = convId;
        this.convType = convType;
        this.time = 0;
        this.unreadCount = 0;
        this.hidden = false;
        this.deleted = false;
    }

    @Override
    public String toString() {
        return "ConvModel{" +
                "convId='" + convId + '\'' +
                ", convType=" + convType +
                ", clientMsgId='" + clientMsgId + '\'' +
                ", noticeId='" + noticeId + '\'' +
                ", time=" + time +
                ", unreadCount=" + unreadCount +
                ", draftModel=" + draftModel +
                ", hidden=" + hidden +
                ", deleted=" + deleted +
                ", msgModel=" + msgModel +
                ", noticeModel=" + noticeModel +
                '}';
    }

    public static class DraftModel {
        public String content;
        public String ext;

        public DraftModel() {
        }

        public DraftModel(String content, String ext) {
            this.content = content;
            this.ext = ext;
        }

        @Override
        public String toString() {
            return "DraftModel{" +
                    "content='" + content + '\'' +
                    ", ext='" + ext + '\'' +
                    '}';
        }
    }
}
