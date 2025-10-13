//package com.crewvy.common.converter;
//
//import com.crewvy.common.entity.Bool;
//import jakarta.persistence.AttributeConverter;
//import jakarta.persistence.Converter;
//
//@Converter(autoApply = true)
//public class BoolConverter implements AttributeConverter<Bool, String> {
//    @Override
//    public String convertToDatabaseColumn(Bool attribute) {
//        return attribute != null ? attribute.getCodeValue() : null;
//    }
//
//    @Override
//    public Bool convertToEntityAttribute(String dbData) {
//        return dbData != null ? Bool.fromCode(dbData) : null;
//    }
//}