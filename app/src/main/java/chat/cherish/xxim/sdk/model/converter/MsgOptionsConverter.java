package chat.cherish.xxim.sdk.model.converter;

import com.alibaba.fastjson2.JSON;

import chat.cherish.xxim.sdk.model.MsgModel;
import io.objectbox.converter.PropertyConverter;

public class MsgOptionsConverter implements PropertyConverter<MsgModel.MsgOptionsModel, String> {

    @Override
    public MsgModel.MsgOptionsModel convertToEntityProperty(String databaseValue) {
        return JSON.parseObject(databaseValue, MsgModel.MsgOptionsModel.class);
    }

    @Override
    public String convertToDatabaseValue(MsgModel.MsgOptionsModel entityProperty) {
        return JSON.toJSONString(entityProperty);
    }
}
