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
