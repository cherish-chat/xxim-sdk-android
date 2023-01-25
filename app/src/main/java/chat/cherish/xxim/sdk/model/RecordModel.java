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
    public int minSeq;
    public int maxSeq;
    public int updateTime;
    public int seq;

    public RecordModel() {
    }

    public RecordModel(String convId, int minSeq, int maxSeq, int updateTime) {
        this.convId = convId;
        this.minSeq = minSeq;
        this.maxSeq = maxSeq;
        this.updateTime = updateTime;
        this.seq = 0;
    }

    public static RecordModel fromProto(Core.BatchGetConvSeqResp.ConvSeq convSeq) {
        return new RecordModel(
                convSeq.getConvId(),
                Integer.parseInt(convSeq.getMinSeq()),
                Integer.parseInt(convSeq.getMaxSeq()),
                Integer.parseInt(convSeq.getUpdateTime())
        );
    }
}
