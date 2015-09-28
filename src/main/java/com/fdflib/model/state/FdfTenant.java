package com.fdflib.model.state;

/**
 * FdfTenant object manages native 4dflib multi-tenant data.
 *
 * Default behavior allows a single tenant default tenant to be used, if the invoking app wishes to use the
 * multi-tenant functionality within 4dflib they simply call all service methods with the tenantId parameter.
 */
public class FdfTenant extends CommonState {

    public String name;
    public String description;
    public String webURL;
    public Boolean isPrimary;

    public FdfTenant() {
        super();

        name = "";
        description = "";
        webURL = "";
        isPrimary = false;

    }


}
