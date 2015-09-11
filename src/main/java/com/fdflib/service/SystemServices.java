package com.fdflib.service;

import com.fdflib.model.entity.FdfEntity;
import com.fdflib.model.state.SystemState;
import com.fdflib.service.impl.StateServices;

import java.util.List;

/**
 * Created by brian.gormanly on 8/22/15.
 */
public class SystemServices {

    public static List<FdfEntity<SystemState>> getAllServices() {
        return StateServices.getAll(SystemState.class);
    }

    public static void createDefaultService(SystemState state) {
        StateServices.save(SystemState.class, state, 1, 1);
    }

}
