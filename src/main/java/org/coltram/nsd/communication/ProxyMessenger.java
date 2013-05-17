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

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class ProxyMessenger {
    //
    private static Logger log = Logger.getLogger(ProxyMessenger.class.getName());

    private GeneralManager GeneralManager;
    private UPnPProcessor uPnPProcessor;
    private BonjourProcessor bonjourProcessor;

    public ProxyMessenger(GeneralManager GeneralManager) {
        this.GeneralManager = GeneralManager;
        uPnPProcessor = new UPnPProcessor(GeneralManager);
        bonjourProcessor = new BonjourProcessor(GeneralManager);
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
     * @param mappedReply whether the reply needs to be with separate parameters or one object with attributes
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processCallAction(JSONObject object, final AtomConnection connection, boolean mappedReply)
            throws JSONException {
        String actionName = object.getString("actionName");
        String serviceId = object.getString("serviceId");
        JSONObject arguments = object.getJSONObject("arguments");
        String callBack = object.optString("replyCallBack");
        switch (protocol(serviceId)) {
            case protocolUPNP:
                uPnPProcessor.CallAction(serviceId, actionName, arguments, connection, callBack, mappedReply);
                break;
            case protocolBonjour:
                bonjourProcessor.CallAction(object, serviceId, connection, callBack);
                break;
            default:
                log.info("unknown discovery and communication protocol");
                break;
        }
    }

    /**
     * Process purpose:exposeService messages
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    private void processExposeService(JSONObject object, AtomConnection connection) throws JSONException {
        JSONObject service = object.getJSONObject("localService");
        String serviceType = service.getString("type");
        String prot = service.getString("protocol");
        int protocol = protocolBonjour;
        if (prot.equalsIgnoreCase("upnp")) protocol = protocolUPNP;
        String serviceId, deviceType;
        String friendlyName = serviceType;
        if (protocol == protocolUPNP) {
            serviceId = "urn:coltram-org:serviceId:"+serviceType+":1.1001";
            deviceType = "urn:coltram-org:device:"+serviceType+":1";
            serviceType = "upnp:urn:coltram-org:service:"+serviceType+":1";
        } else {
            serviceId = serviceType+":1.1001";
            deviceType = serviceType;
            serviceType = "zeroconf:_"+serviceType+"._tcp.local.";
            friendlyName = "coltram";
        }
        friendlyName += "@" + GeneralManager.getConnectionManager().getHostName();
        if (serviceType.startsWith("zeroconf:")) {
            bonjourProcessor.exposeService(serviceType, friendlyName, deviceType,
                                           service, serviceId, connection);
        } else if (serviceType.startsWith("upnp:")) {
            uPnPProcessor.exposeService(serviceType, friendlyName, deviceType,
                                        service, serviceId, connection, object.getString("serviceImplementation"));
        } else {
            log.info("unimplemented service type:" + serviceType);
        }
    }

    /**
     * Function processing JSON messages sent to the proxy by a webapp
     *
     * @param object     the JSON message
     * @param connection the incoming connection, to be able to reply if necessary
     * @throws JSONException as usual when manipulating JSON objects
     */
    public void dispatcher(JSONObject object, WebSocket connection) throws JSONException {
        AtomConnection ac = null;
            for (AtomConnection atomConnection : GeneralManager.getConnectionManager().getConnections()) {
                if (atomConnection.getConnection() == connection) {
                    ac = atomConnection;
                    break;
                }
            }
        log.finer(object.toString());
        String purpose = object.getString("purpose");
        if (purpose.compareTo("callAction") == 0) {
            processCallAction(object, ac, false);
        } else if (purpose.compareTo("callAction2") == 0) {
            processCallAction(object, ac, true);
        } else if (purpose.compareTo("exposeService") == 0) {
            processExposeService(object, ac);
        } else if (purpose.compareTo("reply") == 0) {
            // this is a reply sent by a Bonjour service
            bonjourProcessor.processReply(object);
        } else {
            //
            // message not understood
            //
            //ColtramLogger.logln("websockets message to proxy not understood: " + purpose);
            log.info("Purpose " + purpose + " not understood");
        }
    }

}
