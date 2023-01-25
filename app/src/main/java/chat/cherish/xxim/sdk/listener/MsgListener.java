package chat.cherish.xxim.sdk.listener;

import java.util.List;

import chat.cherish.xxim.sdk.model.MsgModel;

interface IMsg {
    void onReceive(List<MsgModel> msgModelList);
}

public abstract class MsgListener implements IMsg {
    @Override
    public void onReceive(List<MsgModel> msgModelList) {
    }
}
