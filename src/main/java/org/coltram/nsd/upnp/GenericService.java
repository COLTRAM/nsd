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

import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.action.ActionExecutor;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.state.StateVariableAccessor;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.StringDatatype;

import java.beans.PropertyChangeSupport;
import java.util.HashMap;

public class GenericService<T> extends LocalService<T> {
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(GenericService.class.getName());
    private HashMap<String, StateVariable> stateVariables;
    public static StateVariableTypeDetails stateVariableTypeDetails;
    private final PropertyChangeSupport propertyChangeSupport;
    private final String sid;

    public String toString() {
        return sid;
    }

    public static void init() {
        StringDatatype stringDatatype = new StringDatatype();
        stringDatatype.setBuiltin(Datatype.Builtin.getByDescriptorName("string"));
        stateVariableTypeDetails = new StateVariableTypeDetails(stringDatatype);
    }

    public GenericService(ServiceType serviceType, ServiceId serviceId,
                        ActionList actionList, boolean supportsQueryStateVariables) throws ValidationException {
        super(serviceType, serviceId, actionList.actionExecutors(), actionList.stateVariableAccessors(),
                actionList.stringConvertibleTypes(), supportsQueryStateVariables);
        stateVariables = actionList.stateVariables();
        sid = "GServ "+serviceId.toString();
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public org.teleal.cling.model.meta.Action getQueryStateVariableAction() {
        return null;
    }

    @Override
    public StateVariable getStateVariable(String name) {
        return stateVariables.get(name);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public void setEventValue(String eventName, String eventValue) {
        log.fine("updateEvent UPnP "+eventName);
        GenericServiceVariable v = (GenericServiceVariable)stateVariableAccessors.get(stateVariables.get(eventName));
        getPropertyChangeSupport().firePropertyChange(eventName, v.getValue(), eventValue);
        v.setValue(eventValue);
    }
}
