package chat.cherish.xxim.sdk.listener;

interface IUnread {
    void onUnreadCount(long count);
}

public abstract class UnreadListener implements IUnread {
    @Override
    public void onUnreadCount(long count) {
    }
}
