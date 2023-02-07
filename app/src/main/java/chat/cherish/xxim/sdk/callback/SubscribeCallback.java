package chat.cherish.xxim.sdk.callback;

import java.util.List;
import java.util.Map;

import chat.cherish.xxim.sdk.common.AesParams;

interface ISubscribe {
    Map<String, AesParams> onConvParams();
}

public abstract class SubscribeCallback implements ISubscribe {
    @Override
    public Map<String, AesParams> onConvParams() {
        return null;
    }
}
