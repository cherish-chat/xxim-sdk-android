package chat.cherish.xxim.sdk.callback;

interface IOperate<T> {
    void onSuccess(T t);

    void onError(int code, String error);
}

public abstract class OperateCallback<T> implements IOperate<T> {
    @Override
    public void onSuccess(T t) {
    }

    @Override
    public void onError(int code, String error) {
    }
}
