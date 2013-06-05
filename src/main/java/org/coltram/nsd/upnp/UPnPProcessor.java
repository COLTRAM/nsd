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
import org.coltram.nsd.communication.TopManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.types.*;

import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;

public class UPnPProcessor {
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(UPnPProcessor.class.getName());
    private ArrayList<SubscriptionCallback> subscriptions = new ArrayList<SubscriptionCallback>();

    private TopManager coltramManager;
    private GenericService exposedService = null;

    public UPnPProcessor(TopManager deviceManager) {
        this.coltramManager = deviceManager;
    }

    public void UpdateEvent(String eventName, String eventValue) {
        exposedService.setEventValue(eventName, eventValue);
    }

    public void Unsubscribe(String serviceId, String eventName) {
        log.finer("unsubscribe UPnP "+eventName);
        for (SubscriptionCallback cb : subscriptions) {
            if (serviceId.endsWith(cb.getService().getServiceId().getId()) &&
                eventName.equals(cb.getEventName())) {
                cb.end();
                return;
            }
        }
    }

    public void Subscribe(String serviceId, String eventName, String callback, AtomConnection connection) {
        log.finer("subscribe UPnP "+eventName);
        Service service = coltramManager.getServiceManager().findService(serviceId);
        SubscriptionCallback subscriptionCallback = new SubscriptionCallback(service, 600, eventName, connection, callback);
        subscriptions.add(subscriptionCallback);
        coltramManager.getConnectionManager().getUpnpService().getControlPoint().execute(subscriptionCallback);
    }

    public void CallAction(String serviceId, String actionName, JSONObject arguments,
                           AtomConnection connection, String callBack, boolean mappedReply) {
        Service s = coltramManager.getServiceManager().findService(serviceId);
        if (s == null) {
            // service does not exist (any more ?)
                log.info("no service with id " + serviceId + " (" + actionName + ")");
        } else {
            executeAction(s, actionName, arguments, connection, callBack, mappedReply);
        }
    }

    /**
     * Function executing an action on a service, including the reply processing
     * only used in UPnP
     *
     * @param service       the target service
     * @param actionName    the name of the action to call
     * @param args          the arguments of the action
     * @param connection    the incoming connection in case reply is needed
     * @param replyCallBack the reply callback function name
     * @param mappedReply   whether the reply needs to be with separate parameters or one object with attributes
     */
    private void executeAction(final Service service, final String actionName, JSONObject args, final AtomConnection connection,
                               final String replyCallBack, final boolean mappedReply) {
        //
        // to avoid a loop in the logging, any reply from COLTRAMAgentLogger is not logged
        //
        final String serviceType = service.getServiceType().toString();
        org.teleal.cling.model.meta.Action action = service.getAction(actionName);
        if (action == null) {
            log.info("unknown action for this service: " + actionName + " " + service.getReference().toString());
            return;
        }
        NSDActionInvocation setTargetInvocation =
                new NSDActionInvocation(action, args);
        coltramManager.getConnectionManager().getUpnpService().getControlPoint().execute(
                new ActionCallback(setTargetInvocation) {
                    @Override
                    public void success(org.teleal.cling.model.action.ActionInvocation invocation) {
                        ActionArgumentValue[] values = invocation.getOutput();
                        if (values != null && values.length > 0 && !"".equals(replyCallBack)) {
                            //
                            // if there is an output, call reply callback
                            //
                            JSONObject result = new JSONObject();
                            try {
                                result.put("purpose", (mappedReply ? "mappedReply" : "reply"));
                                result.put("callBack", replyCallBack);
                                if (!mappedReply) {
                                    String sid = service.getDevice().getIdentity().getUdn().getIdentifierString();
                                    sid += service.getReference().getServiceId().toString();
                                    result.put("serviceId", sid);
                                    result.put("actionName", actionName);
                                }
                            } catch (JSONException e) {
                                log.finer("JSON error while building reply (constant header)");
                            }
                            for (ActionArgumentValue v : values) {
                                try {
                                    result.put(v.getArgument().getName(), v.getValue());
                                } catch (JSONException e) {
                                    log.finer("JSON error while building reply: " + v.getArgument().getName());
                                }
                            }
                            try {
                                String s = result.toString();
                                connection.getConnection().send(s);
                            } catch (NotYetConnectedException e) {
                                Thread.yield();
                            }
                        }
                        log.finer("Successfully called action!");
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        System.err.println(defaultMsg);
                    }
                }
        );
    }

    public void exposeService(String serviceType, String friendlyName, String deviceType,
                              JSONObject service, String serviceId, AtomConnection connection,
                              String serviceImplementationName) throws JSONException {
        ActionList actionList;
        try {
            actionList = new ActionList(service.getJSONArray("actionList"), connection, serviceImplementationName,
                    service.optJSONArray("eventList"));
        } catch (RuntimeException e) {
            // error in service description, translated to runtimeexception, so stop processing
            return;
        }
        try {
            exposedService = new GenericService<GenericService>(ServiceType.valueOf(serviceType.substring(5)),
                    ServiceId.valueOf(serviceId), actionList, false);
            exposedService.setManager(new ServiceManager<GenericService>(exposedService, GenericService.class));
            LocalDevice newDevice = new LocalDevice(new DeviceIdentity(new UDN(friendlyName)),
                    DeviceType.valueOf(deviceType),
                    new DeviceDetails(friendlyName),
                    exposedService);
            //Logger.logln("exposeService " + serviceType);
            coltramManager.getConnectionManager().getUpnpService().getRegistry().addDevice(newDevice);
            connection.add(newDevice);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }
}
