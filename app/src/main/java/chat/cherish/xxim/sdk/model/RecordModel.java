package chat.cherish.xxim.sdk.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import pb.Core;

@Entity
public class RecordModel {
    @Id
    public long id;

    @Index()
    public String convId;
    public long minSeq;
    public long maxSeq;
    public long updateTime;
    public long seq;

    public RecordModel() {
    }

    public RecordModel(String convId, long minSeq, long maxSeq, long updateTime) {
        this.convId = convId;
        this.minSeq = minSeq;
        this.maxSeq = maxSeq;
        this.updateTime = updateTime;
        this.seq = 0;
    }

    public static RecordModel fromProto(Core.BatchGetConvSeqResp.ConvSeq convSeq) {
        return new RecordModel(
                convSeq.getConvId(),
                Long.parseLong(convSeq.getMinSeq()),
                Long.parseLong(convSeq.getMaxSeq()),
                Long.parseLong(convSeq.getUpdateTime())
        );
    }

    @Override
    public String toString() {
        return "RecordModel{" +
                "convId='" + convId + '\'' +
                ", minSeq=" + minSeq +
                ", maxSeq=" + maxSeq +
                ", updateTime=" + updateTime +
                ", seq=" + seq +
                '}';
    }
}
