package chat.cherish.xxim.sdk.listener;

import chat.cherish.xxim.sdk.model.NoticeModel;

interface INotice {
    boolean onReceive(NoticeModel noticeModel);
}

public abstract class NoticeListener implements INotice {
    @Override
    public boolean onReceive(NoticeModel noticeModel) {
        return false;
    }
}
