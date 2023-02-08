package chat.cherish.xxim.sdk.listener;

import chat.cherish.xxim.sdk.model.MsgModel;
import chat.cherish.xxim.sdk.model.NoticeModel;
import chat.cherish.xxim.sdk.model.SDKContent;

interface INotice {
    boolean onReadMsg(SDKContent.ReadContent readContent);

    boolean onEditMsg(MsgModel msgModel);

    boolean onReceive(NoticeModel noticeModel);
}

public abstract class NoticeListener implements INotice {
    @Override
    public boolean onReadMsg(SDKContent.ReadContent readContent) {
        return false;
    }

    @Override
    public boolean onEditMsg(MsgModel msgModel) {
        return false;
    }

    @Override
    public boolean onReceive(NoticeModel noticeModel) {
        return false;
    }
}
