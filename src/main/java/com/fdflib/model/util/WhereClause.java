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
    public Class valueDataType;

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
    /**
     * Optional constructor that takes the String representation of a WhereClause, such as "AND df &lt;&gt; true.
     * However, there is a problem in the fact that there is no easy way to relay the value type. So for now, this bit
     * of code is commented out.
     *
     * Complex regex is in case the smart people who enter the clause don't understand how to be decent human beings
     * and instead just don't care about styling. This catches the three cases shown here, "AND)(df) (&lt;&gt;) true".
     * Will probably be needed to update for spacing in case someone thinks "AND df&lt;&gt;true" works, which, to their
     * credit, makes sense in this case. If they go "AND dfIS NOTtrue" then they're a hopeless case. If they enter
     * multiple clauses, such as "WHERE df &lt;&gt; true AND cf = true", then this only parses the "WHERE df &lt;&gt; true" due
     * to the fact that you are creating 1 WhereClause in the WhereClause constructor, not 17 thousand.
     */
    /*WhereClause(String where) {
        String[] clause = where.split("(( )?\\(|(\\))? (\\()?|\\)( )?) ");
        switch(clause[0].toUpperCase()) {
            case "NOT":
                conditional = CONDITIONALS.NOT;
                break;
            case "OR":
                conditional = CONDITIONALS.OR;
                break;
            case "AND":
                conditional = CONDITIONALS.AND;
                break;
            default:
                //If case "WHERE" or left out because it is the first clause and they didn't think they needed to add it
                conditional = CONDITIONALS.AND;
        }
        name = clause[1];
        switch(clause[2].toUpperCase()) {
            case "=":
                operator = Operators.EQUAL;
                break;
            case "<>":
                operator = Operators.NOT_EQUAL;
                break;
            case ">":
                operator = Operators.GREATER_THAN;
                break;
            case "<":
                operator = Operators.LESS_THAN;
                break;
            case ">=":
                operator = Operators.GREATER_THAN_OR_EQUAL;
                break;
            case "<=":
                operator = Operators.LESS_THAN_OR_EQUAL;
                break;
            case "IN":
                operator = Operators.IN;
                break;
            case "BETWEEN":
                operator = Operators.BETWEEN;
                break;
            case "LIKE":
                operator = Operators.LIKE;
                break;
            case "IS":
                if(clause[3].toUpperCase().equals("NOT")) {
                    operator = Operators.IS_NOT;
                    value = clause[4];
                    return;
                }
                operator = Operators.IS;
                break;
            default:
                //You know, in case they decide to go "WHERE df true" like the special people they would be...
                operator = Operators.EQUAL;
        }
        value = clause[3];
        if(clause[2].equalsIgnoreCase("BETWEEN")) {
            if(clause[4].equalsIgnoreCase("AND")) {
                value2 = clause[5];
            }
            else {
                value2 = clause[4];
            }
        }
    }*/

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
            default:
                return null;
        }
    }
}
