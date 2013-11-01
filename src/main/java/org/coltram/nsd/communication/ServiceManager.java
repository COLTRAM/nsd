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

import org.coltram.nsd.bonjour.DiscoveredZCService;
import org.coltram.nsd.interfaces.Connection;
import org.java_websocket.WebSocket;
import org.teleal.cling.UpnpService;
import org.teleal.cling.model.Namespace;
import org.teleal.cling.model.NetworkAddress;
import org.teleal.cling.model.meta.*;

import java.nio.channels.NotYetConnectedException;
import java.util.List;

public class ServiceManager {
    //
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(ServiceManager.class.getName());

    private TopManager topManager;

    public ServiceManager(TopManager topManager) {
        this.topManager = topManager;
    }

    /**
     * Find a service from its ID, used for execute action
     *
     * @param serviceId id (NSD-style) of the service that we look for
     * @return the service object
     */
    public Service findService(String serviceId) {
        log.finer("looking for "+serviceId);
        synchronized (topManager.getDevices()) {
            for (Device d : topManager.getDevices()) {
                log.finer("checking device "+d.getIdentity().getUdn().getIdentifierString());
                if (serviceId.startsWith(d.getIdentity().getUdn().getIdentifierString())) {
                    for (Service s : d.getServices()) {
                        log.finer("checking service "+s.getReference().getServiceId().toString());
                        if (serviceId.endsWith(s.getReference().getServiceId().toString())) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find a service from its ID, used for execute action
     *
     * @param serviceId id (NSD-style) of the service that we look for
     * @return the service object
     */
    public DiscoveredZCService findBonjourService(String serviceId) {
        synchronized (topManager.getServices()) {
            for (DiscoveredZCService d : topManager.getServices()) {
                if (serviceId.equals(d.getId())) {
                    return d;
                }
            }
        }
        return null;
    }

    /**
     * Callback when new service are found
     *
     * @param conn the connection to the webapp to inform
     */
    public void signalNewServices(Connection conn) {
        StringBuilder sb = new StringBuilder("{\"purpose\":\"serviceDiscovered\",\"services\":[");
        boolean insertComma = false;
        for (Device d : topManager.getDevices()) {
            Service services[] = d.getServices();
            for (int i = 0; i < services.length; i++) {
                if (insertComma) {
                    sb.append(",");
                }
                insertComma = signalService(services[i], d, sb);
            }
        }
        for (DiscoveredZCService s : topManager.getServices()) {
            if (insertComma) {
                sb.append(",");
            }
            insertComma = signalService(s, sb);
        }
        sb.append("]}");
        String serviceString = sb.toString();
        //System.out.println(serviceString);
        log.finer(serviceString);
        try {
            conn.send(serviceString);
            //ColtramLogger.logln("out>" + serviceString);
        } catch (NotYetConnectedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback when a device is removed, to remove its services
     *
     * @param conn the connection to the webapp to inform
     * @param d    the removed device
     */
    public void signalRemoveServices(Connection conn, Device d) {
        StringBuilder sb = new StringBuilder("{\"purpose\":\"serviceRemoved\",\"services\":[");
        Service services[] = d.getServices();
        for (int i = 0; i < services.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            signalService(services[i], d, sb);
        }
        sb.append("]}");
        String serviceString = sb.toString();
        log.finer(serviceString);
        try {
            conn.send(serviceString);
            //ColtramLogger.logln("out>" + serviceString);
        } catch (NotYetConnectedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback when a bonjour service is removed
     *
     * @param conn    the connection to the webapp to inform
     * @param service the removed service
     */
    public void signalRemoveServices(Connection conn, DiscoveredZCService service) {
        StringBuilder sb = new StringBuilder("{\"purpose\":\"serviceRemoved\",\"services\":[");
        signalService(service, sb);
        sb.append("]}");
        String serviceString = sb.toString();
        log.finer(serviceString);
        try {
            conn.send(serviceString);
            //ColtramLogger.logln("out>" + serviceString);
        } catch (NotYetConnectedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function constructing a service descriptor string to pass to a webapp
     *
     * @param s             the service object
     * @param d             the device object
     * @param stringBuilder the string builder into which to write the information
     */
    public boolean signalService(Service s, Device d, StringBuilder stringBuilder) {
        //System.out.println("signal " + s.getServiceType() + " " + d.getDetails().getFriendlyName() + " " + s.getServiceType().getType());
        log.finer("signal " + s.getServiceType() + " " + d.getDetails().getFriendlyName());
        stringBuilder.append("{\"name\":\"");
        stringBuilder.append(s.getServiceType().getType());
        stringBuilder.append("/");
        stringBuilder.append(d.getDetails().getFriendlyName());
        stringBuilder.append("\",\"type\":\"upnp:");
        stringBuilder.append(s.getServiceType());
        stringBuilder.append("\",\"id\":\"");
        stringBuilder.append(d.getIdentity().getUdn().getIdentifierString());
        stringBuilder.append(s.getReference().getServiceId().toString());
        if (s instanceof RemoteService) {
            stringBuilder.append("\",\"config\":\"");
            stringBuilder.append(((RemoteDevice)d).normalizeURI(((RemoteService) s).getDescriptorURI()));
            stringBuilder.append("\",\"url\":\"");
            stringBuilder.append(((RemoteDevice)d).normalizeURI(((RemoteService) s).getControlURI()));
        } else {
            UpnpService upnpService = topManager.getConnectionManager().getUpnpService();
            Namespace ns = upnpService.getConfiguration().getNamespace();
            List<NetworkAddress> activeStreamServers =
                    upnpService.getRouter().getActiveStreamServers(null);
            if (activeStreamServers.size() == 0) {
                // bug network stopped ?
                throw new RuntimeException("network stopped");
            }
            String address = activeStreamServers.get(0).getAddress().getHostAddress();
            int port = activeStreamServers.get(0).getPort();
            stringBuilder.append("\",\"config\":\"http://");
            stringBuilder.append(address);
            stringBuilder.append(":");
            stringBuilder.append(port);
            stringBuilder.append(ns.getDescriptorPath(s));
            stringBuilder.append("\",\"url\":\"http://");
            stringBuilder.append(address);
            stringBuilder.append(":");
            stringBuilder.append(port);
            stringBuilder.append(ns.getControlPath(s));
        }
        stringBuilder.append("\",\"actionList\":[");
        Action al[] = s.getActions();
        for (int i = 0; i < al.length; i++) {
            if (i > 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("{\"name\":\"");
            stringBuilder.append(al[i].getName());
            stringBuilder.append("\", \"args\":[");
            ActionArgument args[] = al[i].getArguments();
            for (int j = 0; j < args.length; j++) {
                if (j > 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append("{\"name\":\"");
                stringBuilder.append(args[j].getName());
                stringBuilder.append("\",\"dir\":\"");
                stringBuilder.append(args[j].getDirection());
                stringBuilder.append("\"}");
            }
            stringBuilder.append("]}");
        }
        StateVariable stateVariables[] = s.getStateVariables();
        boolean noEvents = true;
        for (StateVariable sv : stateVariables) {
            if (sv.getEventDetails() != null) {
                if (noEvents) {
                    stringBuilder.append("],\"eventList\":[\"");
                    noEvents = false;
                } else {
                    stringBuilder.append("\",\"");
                }
                stringBuilder.append(sv.getName());
            }
        }
        if (!noEvents) {
            stringBuilder.append("\"");
        }
        stringBuilder.append("]}");
        return true;
    }

    /**
     * Function constructing a service descriptor string to pass to a webapp
     *
     * @param s             the service object
     * @param stringBuilder the string builder into which to write the information
     */
    public boolean signalService(DiscoveredZCService s, StringBuilder stringBuilder) {
        //System.out.println("signal " + s.getServiceType() + " " + d.getDetails().getFriendlyName() + " " + s.getServiceType().getType());
        log.finer("signal " + s.getType() + " " + s.getName());
        stringBuilder.append("{\"name\":\"");
        stringBuilder.append(s.getName());
        stringBuilder.append("\",\"type\":\"zeroconf:");
        stringBuilder.append(s.getType());
        stringBuilder.append("\",\"id\":\"");
        stringBuilder.append(s.getId());
        stringBuilder.append("\",\"config\":\"");
        stringBuilder.append(s.getConfig("Desc"));
        stringBuilder.append("\",\"url\":\"");
        stringBuilder.append(s.getUrl());
        stringBuilder.append("\",\"actionList\":");
        stringBuilder.append(s.getActionList());
        String eventList = s.getEventList();
        if (eventList != null) {
            stringBuilder.append(",\"eventList\":");
            stringBuilder.append(eventList);
        }
        stringBuilder.append("}");
        return true;
    }
}
