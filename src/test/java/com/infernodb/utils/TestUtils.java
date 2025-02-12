package com.infernodb.utils;

import java.lang.reflect.Field;
import java.util.logging.Logger;

public class TestUtils {

    public static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }


    @SuppressWarnings("unchecked")
    public static <T, R> R getPrivateField(Class<T> clazz, String fieldName, Class<R> valueClass) {
        try {
            var field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (R) field.get(null);
        } catch (Exception e) {
            Logger.getLogger(TestUtils.class.getName()).severe(e.getMessage());
        }
        return null;
    }
}
