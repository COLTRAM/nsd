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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;

import java.util.logging.Logger;

public class Action<S extends Service> extends org.teleal.cling.model.meta.Action {
    private static Logger log = Logger.getLogger(Action.class.getName());
    private static int stateVariableIndex = 0;

    public Action(String name, ActionArgument[] args) {
        super(name, args);
    }

    public static ActionArgument[] getArguments(JSONArray array) {
        ActionArgument[] args = new ActionArgument[array.length()];
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                args[i] = new ActionArgument(object.getString("name"), "sv" + (stateVariableIndex++),
                        ("in".equalsIgnoreCase(object.getString("dir")) ? ActionArgument.Direction.IN :
                                ActionArgument.Direction.OUT));
            }
        } catch (JSONException e) {
            log.info("Error building service description from JSON (in argument list) "+e.getMessage());
            throw new RuntimeException();
        }
        return args;
    }
}
