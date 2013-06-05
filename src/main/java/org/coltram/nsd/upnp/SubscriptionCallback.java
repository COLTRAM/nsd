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

import org.coltram.nsd.communication.AtomConnection;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;

import java.util.Map;

public class SubscriptionCallback extends org.teleal.cling.controlpoint.SubscriptionCallback {
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(SubscriptionCallback.class.getName());
    private String eventName;
    private AtomConnection connection;
    private String callback;

    public String toString() {
        return "SubCB "+eventName+" by "+connection.getId()+" to "+service.toString();
    }

    public SubscriptionCallback(Service service, int timeOut, String eventName, AtomConnection connection, String callback) {
        super(service, timeOut);
        this.eventName = eventName;
        this.connection = connection;
        this.callback = callback;
    }

    public String getEventName() {
        return eventName;
    }

    @Override
    public void established(GENASubscription sub) {
        log.finer("subscription established");
    }

    @Override
    protected void failed(GENASubscription subscription,
                          UpnpResponse responseStatus,
                          Exception exception,
                          String defaultMsg) {
        log.finer("subscription failed " + defaultMsg);
    }

    @Override
    public void ended(GENASubscription sub,
                      CancelReason reason,
                      UpnpResponse response) {
        log.finer("subscription ended");
        if (response != null) log.finer(response.getStatusMessage() + " " + response.getResponseDetails());
    }

    public void eventReceived(GENASubscription sub) {
        log.finer("event UPnP " + eventName);
        Map<String, StateVariableValue> values = sub.getCurrentValues();
        StateVariableValue value = values.get(eventName);
        JSONObject obj = new JSONObject();
        try {
            obj.put("purpose", "updateEvent");
            obj.put("eventValue", value.toString());
            obj.put("callback", callback);
            connection.getConnection().send(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
    }
}
