package chat.cherish.xxim.sdk.model.converter;

import com.alibaba.fastjson2.JSON;

import chat.cherish.xxim.sdk.model.ConvModel;
import io.objectbox.converter.PropertyConverter;

public class DraftConverter implements PropertyConverter<ConvModel.DraftModel, String> {

    @Override
    public ConvModel.DraftModel convertToEntityProperty(String databaseValue) {
        return JSON.parseObject(databaseValue, ConvModel.DraftModel.class);
    }

    @Override
    public String convertToDatabaseValue(ConvModel.DraftModel entityProperty) {
        return JSON.toJSONString(entityProperty);
    }
}
