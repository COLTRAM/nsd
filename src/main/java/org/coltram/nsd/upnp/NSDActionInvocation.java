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

package org.coltram.nsd.upnp;

import java.util.Iterator;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.types.InvalidValueException;

public class NSDActionInvocation extends ActionInvocation {
    private static Logger log = Logger.getLogger(NSDActionInvocation.class.getName());

    @SuppressWarnings("unchecked")
    public NSDActionInvocation(org.teleal.cling.model.meta.Action action, JSONObject args) {
        super(action);
        log.finest("setInput action:"+action.getName());
        try {
            // Throws InvalidValueException if the value is of wrong type
            Iterator keys = args.keys();
            while (keys.hasNext()) {
                String name = (String)keys.next(), value = args.get(name).toString();
                log.finest("setInput "+name+" "+value);
                setInput(name, value);
            }
        } catch (JSONException e) {
        	log.throwing(this.getClass().getName(), "NSDActionInvocation", e);
        } catch (InvalidValueException ex) {
        	log.throwing(this.getClass().getName(), "NSDActionInvocation", ex);
        }
    }
}
