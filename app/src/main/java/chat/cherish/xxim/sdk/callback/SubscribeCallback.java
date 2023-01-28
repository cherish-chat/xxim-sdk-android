package chat.cherish.xxim.sdk.callback;

import java.util.List;
import java.util.Map;

import chat.cherish.xxim.sdk.common.AesParams;

interface ISubscribe {
    List<String> onConvIdList();

    Map<String, AesParams> onConvAesParams(List<String> convIdList);
}

public abstract class SubscribeCallback implements ISubscribe {
    @Override
    public List<String> onConvIdList() {
        return null;
    }

    @Override
    public Map<String, AesParams> onConvAesParams(List<String> convIdList) {
        return null;
    }
}
