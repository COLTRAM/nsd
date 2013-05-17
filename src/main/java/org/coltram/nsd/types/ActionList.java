/*
 * Copyright (c) 2012-2013. Telecom ParisTech/TSI/MM/GPAC Jean-Claude Dufourd
 * This code was developed with the Coltram project, funded by the French ANR.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * This notice must stay in all subsequent versions of this code.
 */

package org.coltram.nsd.types;

import org.coltram.nsd.communication.AtomConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.action.ActionExecutor;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.state.StateVariableAccessor;

import java.util.*;
import java.util.logging.Logger;

public class ActionList extends ArrayList<Action> {
    //
    private static Logger log = Logger.getLogger(ActionList.class.getName());

    private AtomConnection connection;
    private String callbackName;
    private HashMap<String, StateVariable> stateVariables = new HashMap<String, StateVariable>();

    public ActionList(JSONArray array, AtomConnection connection, String callbackName) {
        this.connection = connection;
        this.callbackName = callbackName;
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                add(new Action<GenericService>(object.getString("name"),
                        Action.getArguments(object.getJSONArray("args"))));
            }
        } catch (JSONException e) {
            log.info("Error building service description from JSON (probably in action name) "+e.getMessage());
            throw new RuntimeException();
        }
    }

    public Map<org.teleal.cling.model.meta.Action, ActionExecutor> actionExecutors() {
        HashMap<org.teleal.cling.model.meta.Action, ActionExecutor> map = new HashMap<org.teleal.cling.model.meta.Action, ActionExecutor>();
        GenericServiceActionExecutor genericServiceActionExecutor = new GenericServiceActionExecutor(connection, callbackName);
        for (Action action : this) {
            map.put(action, genericServiceActionExecutor);
        }
        return map;
    }

    public Map<StateVariable, StateVariableAccessor> stateVariableAccessors() {
        HashMap<StateVariable, StateVariableAccessor> map = new HashMap<StateVariable, StateVariableAccessor>();
        for (Action action : this) {
            for (ActionArgument arg : action.getArguments()) {
                String name = arg.getRelatedStateVariableName();
                StateVariable sv = stateVariables.get(name);
                if (sv == null) {
                    sv = new StateVariable(name, GenericService.stateVariableTypeDetails);
                    stateVariables.put(name, sv);
                }
                if (map.get(sv) != null) {
                    continue;
                }
                map.put(sv, new GenericServiceVariable());
            }
        }
        return map;
    }

    public Set<Class> stringConvertibleTypes() {
        HashSet<Class> set = new HashSet<Class>();
        return set;
    }
}
