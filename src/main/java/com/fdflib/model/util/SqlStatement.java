package com.fdflib.model.util;

import com.fdflib.model.state.CommonState;
import com.fdflib.service.impl.FdfCommonServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Corley.Herman1 on 8/16/2016.
 */
public class SqlStatement {
    private List<String> select;
    private List<WhereClause> where;
    private List<String> groupBy;
    private List<String> orderBy;
    private int limit, offset;

    private SqlStatement() {
        select = new ArrayList<>();
        where = new ArrayList<>();
        groupBy = new ArrayList<>();
        orderBy = new ArrayList<>();
        limit = offset = 0;
    }
    private SqlStatement(SqlStatement split) {
        this();
        select.addAll(split.select);
        where.addAll(split.where);
        groupBy.addAll(split.groupBy);
        orderBy.addAll(split.orderBy);
        limit = split.limit;
        offset = split.offset;
    }

    public static SqlStatement build() {
        return new SqlStatement();
    }
    public SqlStatement split() {
        return new SqlStatement(this);
    }

    public SqlStatement select(String selectItem) {
        if(!selectItem.isEmpty()) {
            select.add(selectItem);
        }
        return this;
    }
    public SqlStatement select(List<String> selectList) {
        select.addAll(selectList.stream().filter(selectItem -> !selectItem.isEmpty()).collect(Collectors.toList()));
        return this;
    }

    //Commented out until valueDataType can be set this way.
    /*public SqlStatement where(String clause) {
        return where(new WhereClause(clause));
    }*/
    public SqlStatement where(WhereClause whereClause) {
        if(whereClause != null) {
            where.add(whereClause);
        }
        return this;
    }
    public SqlStatement where(List<WhereClause> whereStatement) {
        where.addAll(whereStatement.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return this;
    }

    public SqlStatement groupBy(String group) {
        if(!group.isEmpty()) {
            groupBy.add(group);
        }
        return this;
    }
    public SqlStatement groupBy(List<String> groupings) {
        groupBy.addAll(groupings.stream().filter(group -> !group.isEmpty()).collect(Collectors.toList()));
        return this;
    }

    public SqlStatement orderBy(String order) {
        if(!order.isEmpty()) {
            orderBy.add(order);
        }
        return this;
    }
    public SqlStatement orderBy(List<String> orderings) {
        orderBy.addAll(orderings.stream().filter(order -> !order.isEmpty()).collect(Collectors.toList()));
        return this;
    }

    public SqlStatement limit(int resultsPerPage, int currentPage) {
        offset = (currentPage - 1) * (limit = resultsPerPage);
        return this;
    }

    public <S extends CommonState> List<S> run(Class<S> entityState) {
        return FdfCommonServices.sqlStatementSelect(entityState, this);
    }

    public String getSelect() {
        StringBuilder sql = new StringBuilder("SELECT ");
        if(!select.isEmpty()) {
            for(int s=0; s<select.size(); s++) {
                if(s > 0) {
                    sql.append(", ");
                }
                if(!select.get(s).contains("DISTINCT")
                        && !select.get(s).contains("*")
                        && !select.get(s).contains("`")) {
                    if(select.get(s).contains("(")) {
                        sql
                                .append(select.get(s).substring(0, select.get(s).indexOf("(") + 1))
                                .append("`")
                                .append(select.get(s).substring(select.get(s).indexOf("(") + 1, select.get(s).indexOf(")") + 1))
                                .append("`")
                                .append(select.get(s).substring(select.get(s).indexOf(")") + 1, select.get(s).length()));
                    }
                    else {
                        sql.append("`").append(select.get(s)).append("`");
                    }
                }
                else {
                    sql.append(select.get(s));
                }
            }
        } else {
            sql.append("*");
        }
        return sql.toString();
    }
    public String getWhere() {
        StringBuilder sql = new StringBuilder();
        for(WhereClause clause : where) {
            sql.append(" ");
            if(where.indexOf(clause) == 0) {
                sql.append("WHERE");
            } else {
                sql.append(clause.conditional.toString());
            }
            sql.append(" ");
            //Check to see if there are any open parenthesis to apply
            clause.groupings.stream().filter(grouping -> grouping.equals(WhereClause.GROUPINGS.OPEN_PARENTHESIS)).forEach(openParen -> sql.append("("));
            //Format clause by datatype
            if(clause.operator != WhereClause.Operators.UNARY) {
                sql.append("`").append(clause.name).append("` ").append(clause.getOperatorString()).append(" ");
                if(clause.value.equals(WhereClause.NULL) || Number.class.isAssignableFrom(clause.valueDataType)) {
                    sql.append(clause.value);
                }
                else if(clause.valueDataType == Boolean.class) {
                    sql.append(clause.value.toLowerCase());
                }
                else { //Includes String, Date, and UUID
                    sql.append("'").append(clause.value.replaceAll("'", "''")).append("'");
                }
            }
            //Check to see if there are any closing parenthesis to apply
            clause.groupings.stream().filter(grouping -> grouping.equals(WhereClause.GROUPINGS.CLOSE_PARENTHESIS)).forEach(closeParen -> sql.append(")"));
        }
        return sql.toString();
    }
    public String getGroupBy() {
        StringBuilder sql = new StringBuilder();
        groupBy.forEach(group -> {
            if(groupBy.indexOf(group) == 0) {
                sql.append(" GROUP BY");
            }
            else {
                sql.append(",");
            }
            sql.append(" `").append(group).append("`");
        });
        return sql.toString();
    }
    public String getOrderBy() {
        StringBuilder sql = new StringBuilder();
        orderBy.forEach(order -> {
            if(orderBy.indexOf(order) == 0) {
                sql.append(" ORDER BY");
            }
            else {
                sql.append(",");
            }
            sql.append(" ").append(order);
        });
        return sql.toString();
    }
    public String getLimit() {
        StringBuilder sql = new StringBuilder();
        if(limit > 0 && offset > -1) {
            sql.append(" LIMIT ").append(limit);
            if(offset > 0) {
                sql.append(" OFFSET ").append(offset);
            }
        }
        return sql.toString();
    }
    public int[] setForManualLimit() {
        int[] manual = {offset, limit};
        limit = offset = 0;
        return manual;
    }
}
