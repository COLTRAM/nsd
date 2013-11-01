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

import org.coltram.nsd.bonjour.BonjourProcessor;
import org.coltram.nsd.upnp.UPnPProcessor;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unchecked")
public class ProxyMessenger {
    //
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(ProxyMessenger.class.getName());

    private TopManager coltramManager;
    private UPnPProcessor uPnPProcessor;
    private BonjourProcessor bonjourProcessor;

    public ProxyMessenger(TopManager coltramManager) {
        this.coltramManager = coltramManager;
        uPnPProcessor = new UPnPProcessor(coltramManager);
        bonjourProcessor = new BonjourProcessor(coltramManager);
    }

    private static final int protocolUPNP = 0;
    private static final int protocolBonjour = 1;

    private int protocol(String serviceId) {
        if (serviceId.contains("urn:")) {
            return protocolUPNP;
        }
        return protocolBonjour;
    }

    /**
     * Process purpose:callAction messages
     *
     * @param object      the JSON message
     * @param connection  the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processCallAction(JSONObject object, final AtomConnection connection)
            throws JSONException {
        String actionName = object.getString("actionName");
        String serviceId = object.getString("serviceId");
        JSONObject arguments = object.getJSONObject("arguments");
        String callBack = object.optString("replyCallBack");
        switch (protocol(serviceId)) {
            case protocolUPNP:
                uPnPProcessor.callAction(serviceId, actionName, arguments, connection, callBack);
                break;
            case protocolBonjour:
                bonjourProcessor.callAction(object, serviceId, connection, callBack);
                break;
            default:
                log.info("unknown discovery and communication protocol");
                break;
        }
    }
    
    /**
     * 
     * @param object
     * @param connection
     * @return
     * @throws JSONException
     */
	private String getFriendlyName(JSONObject object, AtomConnection connection) throws JSONException {
		final String remoteHost = connection.getConnection().getRemoteHostName();
		final JSONObject service = object.getJSONObject("localService");
		final String uniqueId = service.optString("uniqueId", "id");
		final String name = uniqueId + "@" + remoteHost;
		log.finer("getFriendlyName:" + name);
		return name;
	}

    /**
     * Process purpose:exposeService messages
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processExposeService(JSONObject object, AtomConnection connection) throws JSONException {
    	log.finer("processExposeService" + object.toString());
        JSONObject service = object.getJSONObject("localService");
        String serviceType = service.getString("type");
        String prot = service.getString("protocol");
        int protocol = protocolBonjour;
        if (prot.equalsIgnoreCase("upnp")) protocol = protocolUPNP;
        String deviceType, serviceId=null;
        //String friendlyName = serviceType;
        if (protocol == protocolUPNP) {
            deviceType = "urn:coltram-org:device:"+serviceType+":1";
            serviceId = "urn:coltram-org:serviceId:"+serviceType+":1.1001";
            serviceType = "upnp:urn:coltram-org:service:"+serviceType+":1";
        } else {
            deviceType = serviceType;
            serviceType = "zeroconf:_"+serviceType+"._tcp.local.";
            //friendlyName = "coltram";
        }
        final String friendlyName = getFriendlyName(object, connection);
		if (serviceType.startsWith("zeroconf:")) {
			bonjourProcessor.exposeService(serviceType, friendlyName, deviceType, service, connection);
		} else if (serviceType.startsWith("upnp:")) {
			uPnPProcessor.exposeService(serviceType, friendlyName, deviceType, serviceId, service, connection, object.getString("serviceImplementation"));
		} else {
			log.info("unimplemented service type:" + serviceType);
		}
    }
    
    /**
     * 
     * @param object
     * @param connection
     * @throws JSONException
     */
    private void processUnexposeService(JSONObject object, AtomConnection connection) throws JSONException {
    	log.finer("processUnexposeService" + object.toString());
        String serviceId = connection.getExposedService();
        if (serviceId.contains("upnp")) {
            uPnPProcessor.unexposeService(serviceId, connection);
        } else if (serviceId.startsWith("zeroconf:")) {
                    bonjourProcessor.unexposeService(serviceId, connection);
                } else {
                    log.severe("unknown discovery and communication protocol");
                }
    }

    /**
     * Function processing JSON messages sent to the proxy by a webapp
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    public void dispatcher(JSONObject object, Object connection) throws JSONException {
        AtomConnection ac = null;
            for (AtomConnection atomConnection : coltramManager.getConnectionManager().getConnections()) {
                if (atomConnection.getConnection().is(connection)) {
                    ac = atomConnection;
                    break;
                }
            }
        log.finer(object.toString());
        String purpose = object.getString("purpose");
        if (purpose.compareTo("callAction") == 0) {
            processCallAction(object, ac);
        } else if (purpose.compareTo("exposeService") == 0) {
            processExposeService(object, ac);
        } else if (purpose.compareTo("unexposeService") == 0) {
            processUnexposeService(object, ac);
        } else if (purpose.compareTo("updateEvent") == 0) {
            processUpdateEvent(object, ac);
        } else if (purpose.compareTo("subscribe") == 0) {
            processSubscribe(object, ac);
        } else if (purpose.compareTo("unsubscribe") == 0) {
            processUnsubscribe(object, ac);
        } else if (purpose.compareTo("reply") == 0) {
            // this is a reply sent by a Bonjour service
            processReply(object, ac);
        } else {
            //
            // message not understood
            //
            //ColtramLogger.logln("websockets message to proxy not understood: " + purpose);
            log.info("Purpose " + purpose + " not understood");
        }
    }

    private void processReply(JSONObject object, AtomConnection connection) throws JSONException {
        boolean notBonjour = connection.bonjourServices().isEmpty();
        if (notBonjour && object.getString("purpose").equals("reply")) {
            log.finer("REPLY RECEIVED : " + object.toString());
           	if (connection.isWaitingForReply()) {
           		log.finer("get the reply for this wait" + object.toString());
                   connection.setReply(object);
                   connection.setWaitingForReply(false);
           	} else {
           		log.warning("getting a reply when not expecting one" + object.toString());
           	}
        } else {
            bonjourProcessor.processReply(object);
        }
    }

    /**
     * Process purpose:updateEvent messages
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processUpdateEvent(JSONObject object, AtomConnection connection) throws JSONException {
        String eventName = object.getString("eventName");
        String eventValue = object.getString("eventValue");
        String serviceId = connection.getExposedService();
        log.finer("send upd "+eventName+" "+eventValue);
        switch (protocol(serviceId)) {
            case protocolUPNP:
                uPnPProcessor.updateEvent(eventName, eventValue, serviceId);
                break;
            case protocolBonjour:
                bonjourProcessor.updateEvent(eventName, eventValue, serviceId);
                break;
            default:
                log.info("unknown discovery and communication protocol");
                break;
        }
    }

    /**
     * Process purpose:subscribe messages
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processSubscribe(JSONObject object, AtomConnection connection) throws JSONException {
        String eventName = object.getString("eventName");
        String callback = object.getString("callback");
        String serviceId = object.getString("serviceId");
        switch (protocol(serviceId)) {
            case protocolUPNP:
                uPnPProcessor.subscribe(serviceId, eventName, callback, connection);
                break;
            case protocolBonjour:
                bonjourProcessor.xscribe(object, serviceId, connection);
                break;
            default:
                log.info("unknown discovery and communication protocol");
                break;
        }
    }

    /**
     * Process purpose:unsubscribe messages
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processUnsubscribe(JSONObject object, AtomConnection connection) throws JSONException {
        String eventName = object.getString("eventName");
        String serviceId = object.getString("serviceId");
        switch (protocol(serviceId)) {
            case protocolUPNP:
                uPnPProcessor.unsubscribe(serviceId, eventName);
                break;
            case protocolBonjour:
                bonjourProcessor.xscribe(object, serviceId, connection);
                break;
            default:
                log.info("unknown discovery and communication protocol");
                break;
        }
    }

}
