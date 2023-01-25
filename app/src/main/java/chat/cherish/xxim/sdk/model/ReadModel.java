package chat.cherish.xxim.sdk.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class ReadModel {
    @Id
    public long id;

    @Index()
    public String senderId;
    @Index()
    public String convId;
    public int seq;

    public ReadModel() {
    }

    public ReadModel(String senderId, String convId, int seq) {
        this.senderId = senderId;
        this.convId = convId;
        this.seq = seq;
    }
}
