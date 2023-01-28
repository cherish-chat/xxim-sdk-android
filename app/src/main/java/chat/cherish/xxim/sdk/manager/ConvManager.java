package chat.cherish.xxim.sdk.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.cherish.xxim.sdk.model.ConvModel;
import chat.cherish.xxim.sdk.model.ConvModel_;
import chat.cherish.xxim.sdk.model.MsgModel;
import chat.cherish.xxim.sdk.model.NoticeModel;
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
        QueryBuilder<ConvModel> convBuilder = sdkManager.convBox().query();
        convBuilder.equal(
                ConvModel_.hidden, false
        );
        convBuilder.and().equal(
                ConvModel_.deleted, false
        );
        convBuilder.orderDesc(ConvModel_.draftModel);
        convBuilder.orderDesc(ConvModel_.time);
        Query<ConvModel> convQuery = convBuilder.build();
        List<ConvModel> convModelList = convQuery.find();
        convQuery.close();
        return convModelList;
    }
}
