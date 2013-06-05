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

import org.coltram.nsd.interfaces.Connection;
import org.coltram.nsd.bonjour.DiscoveredZCService;
import org.coltram.nsd.bonjour.LocalExposedBonjourService;
import org.coltram.nsd.types.LocalHost;
import org.json.JSONException;
import org.teleal.cling.UpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;

public class ConnectionManager {
    private final ArrayList<AtomConnection> connections = new ArrayList<AtomConnection>();

    private TopManager topManager;

    //
    private JmDNS jmdns = null;
    private UpnpService upnpService = null;

    public ConnectionManager(TopManager topManager) {
        this.topManager = topManager;
    }

    /**
     * Returns a way to iterate on connections
     *
     * @return a list of connections
     */
    public ArrayList<AtomConnection> getConnections() {
        return connections;
    }

    /**
     * Required to be able to expose (add) services, you have to add a new device for each new service
     *
     * @param cp the UpnpService object
     */
    @SuppressWarnings("unchecked")
    public void setUpnpService(UpnpService cp) {
        upnpService = cp;
    }

    public JmDNS getJmdns() {
        return jmdns;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public void setBonjourService(JmDNS jmdns) {
        this.jmdns = jmdns;
    }

    public void execute(ActionCallback ab) {
        upnpService.getControlPoint().execute(ab);
    }

    /**
     * Add a managed connection to a webapp
     *
     * @param connection the connection object
     */
    public void add(Connection connection) {
        //ColtramLogger.logln("add websocket connection");
        synchronized (connections) {
            AtomConnection ac = new AtomConnection(connection);
            connections.add(ac);
            //StringBuilder s = new StringBuilder("{\"purpose\":\"initialize\",\"myAgentService\":");
            StringBuilder s = new StringBuilder("{\"purpose\":\"initialize\"");
            //topManager.getServiceManager().signalService(agentService, thisDevice, s);
            s.append(",\"atomId\":\"");
            s.append(ac.getId());
            s.append("\",\"hostName\":\"");
            try {
                s.append(LocalHost.name);
                s.append("\"}");
                connection.send(s.toString());
                //ColtramLogger.logln("out>" + s);
            } catch (NotYetConnectedException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            topManager.getServiceManager().signalNewServices(connection);
        }
    }

    /**
     * Remove a managed connection to a webapp
     *
     * @param connection the connection object
     */
    public void remove(Object connection) {
        //ColtramLogger.logln("remove websocket connection");
        synchronized (connections) {
            for (AtomConnection ac : connections) {
                if (ac.getConnection().is(connection)) {
                    for (LocalDevice d : ac.devices()) {
                        upnpService.getRegistry().removeDevice(d);
                    }
                    for (LocalExposedBonjourService s : ac.bonjourServices()) {
                        jmdns.unregisterService(s.getServiceInfo());
                        s.stop();
                    }
                    connections.remove(ac);
                    return;
                }
            }
        }
    }

    public AtomConnection getConnectionByAtomId(String atomId) throws JSONException {
        for (AtomConnection ac : connections) {
            if (ac.getId().equals(atomId)) {
                return ac;
            }
        }
        return null;
    }

    /**
     * Add a device to the set of managed devices
     *
     * @param d the new device object
     */
    public void add(Device d) {
        synchronized (topManager.getDevices()) {
            if (topManager.getDevices().contains(d)) {
                return;
            }
            topManager.getDevices().add(d);
            synchronized (connections) {
                for (AtomConnection connection : connections) {
                    topManager.getServiceManager().signalNewServices(connection.getConnection());
                }
            }
        }
    }


    /**
     * Add a Bonjour service to the set of managed services
     *
     * @param event the service event object
     */
    public void add(ServiceEvent event) {
        @SuppressWarnings("deprecation")
        DiscoveredZCService zcService = new DiscoveredZCService(event.getInfo(), event.getName(), event.getType());
        synchronized (topManager.getServices()) {
            if (topManager.getServices().contains(zcService)) {
                return;
            }
            topManager.getServices().add(zcService);
            synchronized (connections) {
                for (AtomConnection connection : connections) {
                    topManager.getServiceManager().signalNewServices(connection.getConnection());
                }
            }
        }
    }

    /**
     * Remove a device from the set of managed devices
     *
     * @param event the service event object
     */
    public void remove(ServiceEvent event) {
        DiscoveredZCService zcService = null;
        synchronized (topManager.getServices()) {
            for (DiscoveredZCService s : topManager.getServices()) {
                if (s.getId().equals(event.getInfo().getQualifiedName()) &&
                        s.getName().equals(event.getName()) &&
                        s.getType().equals(event.getType())) {
                    topManager.getServices().remove(s);
                    zcService = s;
                    break;
                }
            }
            if (zcService != null) {
                synchronized (connections) {
                    for (AtomConnection connection : connections) {
                        topManager.getServiceManager().signalRemoveServices(connection.getConnection(), zcService);
                    }
                }
            }
        }
    }

    /**
     * Remove a device from the set of managed devices
     *
     * @param d the device object to remove
     */
    public void remove(Device d) {
        synchronized (topManager.getDevices()) {
            for (Device d1 : topManager.getDevices()) {
                if (d1 == d) {
                    synchronized (connections) {
                        for (AtomConnection connection : connections) {
                            topManager.getServiceManager().signalRemoveServices(connection.getConnection(), d);
                        }
                    }
                    topManager.getDevices().remove(d1);
                    return;
                }
            }
        }
    }


    public AtomConnection getAtomConnection(Object connection) {
        synchronized (connections) {
            for (AtomConnection ac : connections) {
                if (ac.getConnection().is(connection)) {
                    return ac;
                }
            }
        }
        return null;
    }

    public Connection getConnection(String connectionId) {
        for (AtomConnection ac : connections) {
            if (connectionId.equals(ac.getId())) {
                return ac.getConnection();
            }
        }
        return null;
    }
}
