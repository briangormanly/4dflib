/**
 * 4DFLib
 * Copyright (c) 2015-2016 Brian Gormanly
 * 4dflib.com
 *
 * 4DFLib is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

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
