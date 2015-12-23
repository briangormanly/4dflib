package com.fdflib.util;

import java.lang.reflect.Field;

/**
 * Created by brian.gormanly on 12/17/15.
 */
public class FdfUtil {

    private static final String TYPE_NAME_PREFIX = "class ";

    public static String getClassName(Class className) {

        String classString = className.toString();

        if (classString.startsWith(TYPE_NAME_PREFIX)) {
            return classString.substring(TYPE_NAME_PREFIX.length());
        }

        return classString;
    }

    public static String getClassName(String className) {

        if (className.startsWith(TYPE_NAME_PREFIX)) {
            return className.substring(TYPE_NAME_PREFIX.length());
        }

        return className;
    }

    public static Class getClassByFullyQualifiedName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
