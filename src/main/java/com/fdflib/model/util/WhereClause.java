/**
 * 4DFLib
 * Copyright (c) 2015 Brian Gormanly
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

package com.fdflib.model.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to specify criteria that returned rows in queries will be constrained by.
 *
 * Created by brian.gormanly on 6/9/15.
 */
public class WhereClause {

    /**
     * field name
     */
    public String name = "";

    /**
     * Data type of primary value
     */
    public Type valueDataType;

    /**
     * Primary value to check against.
     */
    public String value = "";

    /**
     * Data type of secondary value
     */
    public Type value2DataType;

    /**
     * Used for Between opertator (ex. BETWEEN 'value1' AND 'value2')
     */
    public String value2 = null;

    /**
     * Constant value of NULL
     */
    public final static String NULL = "NULL";

    /**
     * Optional conditional used between predicates, not needed for single predicate.  If no conditional is
     * specified, AND is assumed.
     */
    public CONDITIONALS conditional;

    /**
     * Optional operator used to define what type of filtering is to be applied.  Uses the OPERATORS enumeration
     * for available options.  If not definded, EQUAL (=) is assumed.
     */
    public Operators operator;

    /**
     * Optional ist that contains groupings
     * Ex. you might add to Open Parenthesis to the list to represent
     * Where id = ? AND <strong>((</strong> ...
     */
    public List<GROUPINGS> groupings = new ArrayList<>();

    /**
     * ENUM of available Conditionals
     */
    public enum CONDITIONALS {
        AND, OR, NOT
    }

    /**
     * ENUM used to group clauses
     */
    public enum GROUPINGS {
        OPEN_PARENTHESIS, CLOSE_PARENTHESIS
    }

    public enum Operators {
        EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, BETWEEN, LIKE, IN, IS, IS_NOT, UNARY
    }

    /**
     * Default constructor
     */
    public WhereClause() {
        conditional = CONDITIONALS.AND;
        operator = Operators.EQUAL;
    }

    public String getOperatorString() {

        switch (this.operator) {
            case EQUAL:
                return "=";

            case NOT_EQUAL:
                return "<>";

            case GREATER_THAN:
                return ">";

            case LESS_THAN:
                return "<";

            case GREATER_THAN_OR_EQUAL:
                return ">=";

            case LESS_THAN_OR_EQUAL:
                return "<=";

            case IN:
                return "IN";

            case BETWEEN:
                return "BETWEEN";

            case LIKE:
                return "LIKE";

            case IS:
                return "IS";

            case IS_NOT:
                return "IS NOT";

        }
        return null;
    }
}
