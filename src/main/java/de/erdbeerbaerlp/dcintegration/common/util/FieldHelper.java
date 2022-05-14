package de.erdbeerbaerlp.dcintegration.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class FieldHelper {


    public static void makeNonFinal(Field field) throws NoSuchFieldException, IllegalAccessException {
        int mods = field.getModifiers();
        if (Modifier.isFinal(mods)) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
    }

}