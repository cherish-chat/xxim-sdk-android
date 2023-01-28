package chat.cherish.xxim.sdk.manager;

import java.util.List;

import chat.cherish.xxim.sdk.common.ContentType;
import chat.cherish.xxim.sdk.model.NoticeModel;
import chat.cherish.xxim.sdk.model.NoticeModel_;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;

public class NoticeManager {
    private final SDKManager sdkManager;

    public NoticeManager(SDKManager sdkManager) {
        this.sdkManager = sdkManager;
    }

    // 获取通知列表
    public List<NoticeModel> getNoticeList(String convId, int offset, int limit) {
        Query<NoticeModel> noticeQuery = sdkManager.noticeBox().query()
                .equal(
                        NoticeModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .and()
                .equal(
                        NoticeModel_.deleted, false
                )
                .orderDesc(NoticeModel_.createTime)
                .build();
        List<NoticeModel> noticeModelList = noticeQuery.find(offset, limit);
        noticeQuery.close();
        return noticeModelList;
    }

    // 获取单条通知
    public NoticeModel getSingleNotice(String noticeId) {
        Query<NoticeModel> noticeQuery = sdkManager.noticeBox().query()
                .equal(
                        NoticeModel_.noticeId, noticeId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        NoticeModel noticeModel = noticeQuery.findFirst();
        noticeQuery.close();
        return noticeModel;
    }

    // 获取多条通知
    public List<NoticeModel> getMultipleNotice(List<String> noticeIdList) {
        QueryBuilder<NoticeModel> noticeBuilder = sdkManager.noticeBox().query();
        for (String noticeId : noticeIdList) {
            noticeBuilder.equal(
                    NoticeModel_.noticeId, noticeId,
                    QueryBuilder.StringOrder.CASE_SENSITIVE
            ).or();
        }
        Query<NoticeModel> noticeQuery = noticeBuilder.build();
        List<NoticeModel> noticeModelList = noticeQuery.find();
        noticeQuery.close();
        return noticeModelList;
    }

    // 删除通知
    public void deleteNotice(String noticeId) {
        Query<NoticeModel> noticeQuery = sdkManager.noticeBox().query()
                .equal(
                        NoticeModel_.noticeId, noticeId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        NoticeModel noticeModel = noticeQuery.findFirst();
        noticeQuery.close();
        if (noticeModel == null) return;
        noticeModel.contentType = ContentType.unknown;
        noticeModel.content = "";
        noticeModel.deleted = true;
        sdkManager.noticeBox().put(noticeModel);
    }

    // 清空通知
    public void clearNotice(String convId) {
        Query<NoticeModel> noticeQuery = sdkManager.noticeBox().query()
                .equal(
                        NoticeModel_.convId, convId,
                        QueryBuilder.StringOrder.CASE_SENSITIVE
                )
                .build();
        List<NoticeModel> list = noticeQuery.find();
        noticeQuery.close();
        if (list.isEmpty()) return;
        for (NoticeModel noticeModel : list) {
            noticeModel.contentType = ContentType.unknown;
            noticeModel.content = "";
            noticeModel.deleted = true;
        }
        sdkManager.noticeBox().put(list);
    }

}
