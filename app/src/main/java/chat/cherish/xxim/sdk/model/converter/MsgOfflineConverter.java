package chat.cherish.xxim.sdk.model.converter;

import com.alibaba.fastjson.JSON;

import chat.cherish.xxim.sdk.model.MsgModel;
import io.objectbox.converter.PropertyConverter;

public class MsgOfflineConverter implements PropertyConverter<MsgModel.MsgOfflinePushModel, String> {

    @Override
    public MsgModel.MsgOfflinePushModel convertToEntityProperty(String databaseValue) {
        return JSON.parseObject(databaseValue, MsgModel.MsgOfflinePushModel.class);
    }

    @Override
    public String convertToDatabaseValue(MsgModel.MsgOfflinePushModel entityProperty) {
        return JSON.toJSONString(entityProperty);
    }
}
