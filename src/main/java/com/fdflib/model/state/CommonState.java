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

package com.fdflib.model.state;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by brian.gormanly on 3/9/15.
 */
public class CommonState {

    public long rid;
    public long id;
    public boolean cf;
    public boolean df;
    public Date arsd;
    public Date ared;
    public long euid;
    public long esid;
    public long tid;    // tenantId

    public List<FdfRelationship> relationships;

    public CommonState() {
        this.rid = -1;
        this.id = -1;
        this.cf = false;
        this.df = false;
        this.arsd = null;
        this.ared = null;
        this.esid = -1;
        this.euid = -1;
        this.tid = 1;

        relationships = new ArrayList<>();
    }
}
