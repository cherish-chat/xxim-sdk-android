package chat.cherish.xxim.sdk.listener;

interface IUnread {
    void onUnreadCount(int count);
}

public abstract class UnreadListener implements IUnread {
    @Override
    public void onUnreadCount(int count) {
    }
}
