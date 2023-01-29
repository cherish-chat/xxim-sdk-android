package chat.cherish.xxim.sdk.model.converter;

import com.alibaba.fastjson.JSON;

import chat.cherish.xxim.sdk.model.NoticeModel;
import io.objectbox.converter.PropertyConverter;

public class NoticeOptionsConverter implements PropertyConverter<NoticeModel.NoticeOptionsModel, String> {

    @Override
    public NoticeModel.NoticeOptionsModel convertToEntityProperty(String databaseValue) {
        return JSON.parseObject(databaseValue, NoticeModel.NoticeOptionsModel.class);
    }

    @Override
    public String convertToDatabaseValue(NoticeModel.NoticeOptionsModel entityProperty) {
        return JSON.toJSONString(entityProperty);
    }
}
