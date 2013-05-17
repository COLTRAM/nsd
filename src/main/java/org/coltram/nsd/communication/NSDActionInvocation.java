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

package org.coltram.nsd.communication;

import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.types.InvalidValueException;

import java.util.Iterator;
import java.util.logging.Logger;

public class NSDActionInvocation extends ActionInvocation {
    private static Logger log = Logger.getLogger(NSDActionInvocation.class.getName());

    @SuppressWarnings("unchecked")
    public NSDActionInvocation(Action action, JSONObject args) {
        super(action);
        log.finer("setInput action:"+action.getName());
        try {
            // Throws InvalidValueException if the value is of wrong type
            Iterator keys = args.keys();
            while (keys.hasNext()) {
                String name = (String)keys.next(), value = args.get(name).toString();
                log.finer("setInput "+name+" "+value);
                setInput(name, value);
            }
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        } catch (InvalidValueException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
