package com.fdflib.util;

import java.lang.reflect.Field;

/**
 * Created by brian.gormanly on 12/17/15.
 */
public class FdfUtil {

    private static final String TYPE_NAME_PREFIX = "class ";

    public static String getClassName(String className) {

        if (className.startsWith(TYPE_NAME_PREFIX)) {
            return className.substring(TYPE_NAME_PREFIX.length());
        }

        return className;
    }
}
