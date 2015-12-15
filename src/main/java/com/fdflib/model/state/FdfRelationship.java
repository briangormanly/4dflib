package com.fdflib.model.state;

import java.lang.reflect.Field;

/**
 * Created by brian.gormanly on 12/15/15.
 */
public class FdfRelationship<T> {

    public T RelatedClass;
    public RelationshipType relationshipType;
    public Field foriegnKey;
    public String notes ="";


}