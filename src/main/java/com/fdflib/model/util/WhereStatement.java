package com.fdflib.model.util;

import com.fdflib.model.state.CommonState;
import com.fdflib.persistence.FdfPersistence;
import com.fdflib.util.GeneralConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Corley.Herman1 on 4/22/2016.
 */
public class WhereStatement {
    protected final List<WhereClause> whereStatement = new ArrayList<>();

    public void add(WhereClause whereClause) {
        if(whereClause != null) {
            whereStatement.add(whereClause);
        } else {
            System.out.println("Yeah, this shouldn't happen.");
        }
    }

    public void addByRid(long rid) {
        WhereClause whereRid = new WhereClause();
        whereRid.name = "rid";
        whereRid.operator = WhereClause.Operators.EQUAL;
        whereRid.value = Long.toString(rid);
        whereRid.valueDataType = Long.class;
        whereStatement.add(whereRid);
    }
    public void addById(long id) {
        WhereClause whereId = new WhereClause();
        whereId.name = "id";
        whereId.operator = WhereClause.Operators.EQUAL;
        whereId.value = Long.toString(id);
        whereId.valueDataType = Long.class;
        whereStatement.add(whereId);
    }
    public void addByCf() {
        WhereClause whereCf = new WhereClause();
        whereCf.name = "cf";
        whereCf.operator = WhereClause.Operators.EQUAL;
        whereCf.value = "1";
        whereCf.valueDataType = Integer.class;
        whereStatement.add(whereCf);
    }
    public void addNotCf() {
        WhereClause whereCf = new WhereClause();
        whereCf.name = "cf";
        whereCf.operator = WhereClause.Operators.NOT_EQUAL;
        whereCf.value = "1";
        whereCf.valueDataType = Integer.class;
        whereStatement.add(whereCf);
    }
    public void addByDf() {
        WhereClause whereDf = new WhereClause();
        whereDf.name = "df";
        whereDf.operator = WhereClause.Operators.NOT_EQUAL;
        whereDf.value = "1";
        whereDf.valueDataType = Integer.class;
        whereStatement.add(whereDf);
    }
    public void addByArsdBefore(Date date) {
        if(date != null) {
            WhereClause whereStartBefore = new WhereClause();
            whereStartBefore.name = "arsd";
            whereStartBefore.operator = WhereClause.Operators.LESS_THAN_OR_EQUAL;
            whereStartBefore.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereStartBefore.valueDataType = Date.class;
            whereStatement.add(whereStartBefore);
        }
    }
    public void addByArsdAfter(Date date) {
        if(date != null) {
            WhereClause whereStartAfter = new WhereClause();
            whereStartAfter.name = "arsd";
            whereStartAfter.operator = WhereClause.Operators.GREATER_THAN_OR_EQUAL;
            whereStartAfter.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereStartAfter.valueDataType = Date.class;
            whereStatement.add(whereStartAfter);
        }
    }
    public void addByAredBefore(Date date) {
        WhereClause whereEndBefore = new WhereClause();
        whereEndBefore.name = "ared";
        if(date != null) {
            whereEndBefore.operator = WhereClause.Operators.LESS_THAN_OR_EQUAL;
            whereEndBefore.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereEndBefore.valueDataType = Date.class;
        }
        else {
            whereEndBefore.operator = WhereClause.Operators.IS_NOT;
            whereEndBefore.value = WhereClause.NULL;
        }
        whereStatement.add(whereEndBefore);
    }
    public void addByAredAfter(Date date) {
        WhereClause whereCurrent = new WhereClause();
        if(date != null) {
            WhereClause whereEndAfter = new WhereClause();
            whereEndAfter.groupings.add(WhereClause.GROUPINGS.OPEN_PARENTHESIS);
            whereEndAfter.name = "ared";
            whereEndAfter.operator = WhereClause.Operators.GREATER_THAN_OR_EQUAL;
            whereEndAfter.value = GeneralConstants.DB_DATE_FORMAT.format(date);
            whereEndAfter.valueDataType = Date.class;
            whereStatement.add(whereEndAfter);

            whereCurrent.conditional = WhereClause.CONDITIONALS.OR;
            whereCurrent.groupings.add(WhereClause.GROUPINGS.CLOSE_PARENTHESIS);
        }
        whereCurrent.name = "ared";
        whereCurrent.operator = WhereClause.Operators.IS;
        whereCurrent.value = WhereClause.NULL;
        whereStatement.add(whereCurrent);
    }
    public void addAtDate(Date date) {
        addByArsdBefore(date);
        addByAredAfter(date);
    }
    public void addByTid(long tid) {
        WhereClause whereTid = new WhereClause();
        whereTid.name = "tid";
        whereTid.operator = WhereClause.Operators.EQUAL;
        whereTid.value = Long.toString(tid);
        whereTid.valueDataType = Long.class;
        whereStatement.add(whereTid);
    }
    public void addByEuid(long euid) {
        WhereClause whereEuid = new WhereClause();
        whereEuid.name = "euid";
        whereEuid.operator = WhereClause.Operators.EQUAL;
        whereEuid.value = Long.toString(euid);
        whereEuid.valueDataType = Long.class;
        whereStatement.add(whereEuid);
    }
    public void addByEsid(long esid) {
        WhereClause whereEsid = new WhereClause();
        whereEsid.name = "esid";
        whereEsid.operator = WhereClause.Operators.EQUAL;
        whereEsid.value = Long.toString(esid);
        whereEsid.valueDataType = Long.class;
        whereStatement.add(whereEsid);
    }

    public <S extends CommonState> List<S> run(Class<S> entityState) {
        List<WhereClause> query = new ArrayList<>(whereStatement);
        whereStatement.clear();
        return FdfPersistence.getInstance().selectQuery(entityState, null, query);
    }

    public void reset() {
        whereStatement.clear();
    }
    public String toString() {
        String where = "WHERE 1=1";
        for(WhereClause clause : whereStatement) {
            where += "\n  " + clause.conditional.name() + " " + clause.name + " " + clause.getOperatorString() + " " + clause.value;
        }
        return where;
    }
    public List<WhereClause> asList() {
        return whereStatement;
    }
}
