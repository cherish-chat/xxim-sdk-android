package chat.cherish.xxim.sdk.callback;

import java.util.concurrent.ConcurrentHashMap;

import chat.cherish.xxim.sdk.common.AesParams;

interface ISubscribe {
    ConcurrentHashMap<String, AesParams> onConvParams();
}

public abstract class SubscribeCallback implements ISubscribe {
    @Override
    public ConcurrentHashMap<String, AesParams> onConvParams() {
        return null;
    }
}
