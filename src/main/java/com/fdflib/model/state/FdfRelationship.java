package com.fdflib.model.state;

import java.lang.reflect.Field;

/**
 * Created by brian.gormanly on 12/15/15.
 */

/**
 * Defines Relationship between entities
 * @param <T>
 */
public class FdfRelationship<T> {

    /**
     * Type of Class that is related to the current Class, this class should have a FdfRelationship on it's side as
     * well.
     */
    public T RelatedClass;

    /**
     * The relationship type of the current entities part of the relationship.  Options would be ONE or MANY.
     * Example: If the current class is User and the RelatedClass is Phone.class and you wish the relationship to
     * be a ONE to MANY where one user has many phone numbers, you would select ONE for the FdfRelationship in the
     * User relationships List and MANY in the corresponding FdfRelationship of the Phone class.
     */
    public RelationshipType relationshipType;

    /**
     * The key for this side of the relationship.
     * Example: if the current class is User and the RelatedClass is Phone.class, and they have a ONE (User) to
     * MANY (Phone) relationship.  If we assuming that the User class has a field called userId and it is desired that
     * the Phone entities relationship would be stored in a foreign key also called userId then both the User and Phone
     * entities would have a classKey of userId.  If the User entity had an id field as the primary but it was desired
     * that the Phone entity would persist the data in a userId entity then the User would have a classKey of id and
     * the Phone would have userId.
     */
    public Field classKey;

    /**
     * If true, persists the Class type of the related class as well as the key.
     * Example: We have a User entity, Company entity and an Address entity.  Both the User and Company entities have a
     * one to many relationship with the Address Entity.  So either a User or a Company can have many addresses.  In
     * this case a unique Address record is retrieved by combining both the primary entities Id and the primary
     * entities type.  So our User and Company FdfRelationship would have a retainClassType of false since
     * Addresses do not have users or companies, Address would have a FdfRelationship for both User and Company and
     * both would have an retainClassType set to true.
     */
    public Boolean retainClassType;

    /**
     * Notes about this relationship
     */
    public String notes ="";


}