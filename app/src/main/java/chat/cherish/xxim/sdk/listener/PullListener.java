package chat.cherish.xxim.sdk.listener;

interface IPull {
    void onStart();

    void onEnd();
}

public abstract class PullListener implements IPull {
    @Override
    public void onStart() {
    }

    @Override
    public void onEnd() {
    }
}
