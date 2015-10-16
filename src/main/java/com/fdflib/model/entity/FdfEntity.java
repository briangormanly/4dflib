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

package com.fdflib.model.entity;

import com.fdflib.model.state.CommonState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by brian.gormanly on 6/12/15.
 */
public class FdfEntity<S extends CommonState> {
    public S current;
    public List<S> history;
    public long entityId = -1;

    public FdfEntity() {
        current = null;
        history = new ArrayList<>();
    }

    public FdfEntity(S currentState, List<S> historyStates) {
        entityId = currentState.id;
        current = currentState;
        history = historyStates;
    }

    public S getStateByRid(long rid) {
        S thisState = null;

        if(this.current != null) {
            if (this.current.rid == rid) {
                thisState = this.current;
            }
        }
        if(this.history != null && this.history.size() > 0) {
            for(S historyState : this.history) {
                if (historyState.rid == rid) {
                    thisState = historyState;
                }
            }
        }

        return thisState;
    }

    /**
     * Returns the most recent state for the entity if there is a current state it will be returned, otherwise the
     * newest historical state will be returned.
     *
     * @return the most recent state for the entity
     */
    public S getMostRecentState() {

        if(this.current != null) {
            return this.current;
        }
        else {
            // there was no current state, see if we can get a state from history
            if(this.history != null && this.history.size() > 0) {
                // find the historical entry with the most recent endDate
                S lastState = null;
                for(S historicalState: this.history) {
                    if(lastState == null) {
                        lastState = historicalState;
                    }
                    else {
                        // if the state ared we are looking at is greater then then the last one make it the last
                        if(historicalState.ared.getTime() > lastState.ared.getTime()) {
                            // the state being looked at is more recent
                            lastState = historicalState;

                        }
                    }
                }
                return lastState;
            }
        }
        return null;
    }

    public S getStateOfEntityAt(Date date) {
        S thisState = null;

        if(this.current != null) {
            if (this.current.arsd.before(date) && this.current.ared.after(date)) {
                thisState = this.current;
            }
        }
        if(this.history != null && this.history.size() > 0) {
            for(S historyState : this.history) {
                if (historyState.arsd.before(date) && historyState.ared.after(date)) {
                    thisState = historyState;
                }
            }
        }

        return thisState;
    }
}
