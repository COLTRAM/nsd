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

import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.action.ActionExecutor;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.state.StateVariableAccessor;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.StringDatatype;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class GenericService<T> extends LocalService<T> {
    public Hashtable<String, StateVariable> stateVariables = new Hashtable<String, StateVariable>();
    public static StateVariableTypeDetails stateVariableTypeDetails;
    public String callbackName;

    public static void init() {
        StringDatatype stringDatatype = new StringDatatype();
        stringDatatype.setBuiltin(Datatype.Builtin.getByDescriptorName("string"));
        stateVariableTypeDetails = new StateVariableTypeDetails(stringDatatype);
    }

    public GenericService(ServiceType serviceType, ServiceId serviceId,
                        Map<Action, ActionExecutor> actionExecutors,
                        Map<StateVariable, StateVariableAccessor> stateVariableAccessors,
                        Set<Class> stringConvertibleTypes,
                        boolean supportsQueryStateVariables,
                        String callbackName) throws ValidationException {
        super(serviceType, serviceId, actionExecutors, stateVariableAccessors,
                stringConvertibleTypes, supportsQueryStateVariables);
        this.callbackName = callbackName;
    }

    public org.teleal.cling.model.meta.Action getQueryStateVariableAction() {
        return null;
    }

    @Override
    public StateVariable<LocalService> getStateVariable(String name) {
        StateVariable sv = stateVariables.get(name);
        if (sv != null) return sv;
        sv = new StateVariable(name, stateVariableTypeDetails);
        stateVariables.put(name, sv);
        return sv;
    }
}
