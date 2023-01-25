package chat.cherish.xxim.sdk.listener;

interface IConv {
    void onUpdate();
}

public abstract class ConvListener implements IConv {
    @Override
    public void onUpdate() {
    }
}
