package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.SystemState;
import com.fdflib.service.impl.StateServices;

import java.util.List;

/**
 * Created by brian.gormanly on 8/22/15.
 */
public class SystemServices implements StateServices {

    public List<FdfEntity<SystemState>> getAllServices() {
        return this.getAll(SystemState.class);
    }

    public void createDefaultService(SystemState state) {
        this.save(SystemState.class, state, 1, 1);
    }

}
