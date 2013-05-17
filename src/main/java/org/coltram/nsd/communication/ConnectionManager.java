package org.coltram.nsd.communication;

import org.coltram.nsd.services.BonjourService;
import org.coltram.nsd.types.ZCService;
import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.teleal.cling.UpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import java.net.*;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Logger;

public class ConnectionManager {
    private static Logger log = Logger.getLogger(ConnectionManager.class.getName());
    private final ArrayList<AtomConnection> connections = new ArrayList<AtomConnection>();

    private GeneralManager generalManager;

    private String hostName;

    //
    private JmDNS jmdns = null;
    private UpnpService upnpService = null;

    public ConnectionManager(GeneralManager GeneralManager) {
        this.generalManager = GeneralManager;
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
            boolean already = false;
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (!i.isLoopbackAddress() &&
                            !i.isMulticastAddress() &&
                            !i.isAnyLocalAddress() &&
                            !i.isSiteLocalAddress() &&
                            !i.isLinkLocalAddress() &&
                            i instanceof Inet4Address) {
                        if (already) {
                            log.info("multiple network interfaces: " + i.getHostAddress());
                        } else {
                            already = true;
                            hostName = i.getCanonicalHostName();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Thread.yield();
        }
    }

    public String getHostName() { return hostName; }


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

    @SuppressWarnings("unused")
    public void execute(ActionCallback ab) {
        upnpService.getControlPoint().execute(ab);
    }

    /**
     * Add a managed connection to a webapp
     *
     * @param connection the connection object
     */
    public void add(WebSocket connection) {
        //ColtramLogger.logln("add websocket connection");
        synchronized (connections) {
            AtomConnection ac = new AtomConnection(connection);
            connections.add(ac);
            //StringBuilder s = new StringBuilder("{\"purpose\":\"initialize\",\"myAgentService\":");
            StringBuilder s = new StringBuilder("{\"purpose\":\"initialize\"");
            //generalManager.getServiceManager().signalService(agentService, thisDevice, s);
            s.append(",\"atomId\":\"");
            s.append(ac.getId());
            s.append("\",\"hostName\":\"");
            try {
                s.append(getHostName());
                s.append("\"}");
                connection.send(s.toString());
                //ColtramLogger.logln("out>" + s);
            } catch (NotYetConnectedException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            generalManager.getServiceManager().signalNewServices(connection);
        }
    }

    /**
     * Remove a managed connection to a webapp
     *
     * @param connection the connection object
     */
    public void remove(WebSocket connection) {
        //ColtramLogger.logln("remove websocket connection");
        synchronized (connections) {
            for (AtomConnection ac : connections) {
                if (connection == ac.getConnection()) {
                    for (LocalDevice d : ac.devices()) {
                        upnpService.getRegistry().removeDevice(d);
                    }
                    for (BonjourService s : ac.bonjourServices()) {
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
        synchronized (generalManager.getDevices()) {
            if (generalManager.getDevices().contains(d)) {
                return;
            }
            generalManager.getDevices().add(d);
            synchronized (connections) {
                for (AtomConnection connection : connections) {
                    generalManager.getServiceManager().signalNewServices(connection.getConnection());
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
        ZCService zcService = new ZCService(event.getInfo(), event.getName(), event.getType());
        synchronized (generalManager.getServices()) {
            if (generalManager.getServices().contains(zcService)) {
                return;
            }
            generalManager.getServices().add(zcService);
            synchronized (connections) {
                for (AtomConnection connection : connections) {
                    generalManager.getServiceManager().signalNewServices(connection.getConnection());
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
        ZCService zcService = null;
        synchronized (generalManager.getServices()) {
            for (ZCService s : generalManager.getServices()) {
                if (s.getId().equals(event.getInfo().getQualifiedName()) &&
                        s.getName().equals(event.getName()) &&
                        s.getType().equals(event.getType())) {
                    generalManager.getServices().remove(s);
                    zcService = s;
                    break;
                }
            }
            if (zcService != null) {
                synchronized (connections) {
                    for (AtomConnection connection : connections) {
                        generalManager.getServiceManager().signalRemoveServices(connection.getConnection(), zcService);
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
        synchronized (generalManager.getDevices()) {
            for (Device d1 : generalManager.getDevices()) {
                if (d1 == d) {
                    synchronized (connections) {
                        for (AtomConnection connection : connections) {
                            generalManager.getServiceManager().signalRemoveServices(connection.getConnection(), d);
                        }
                    }
                    generalManager.getDevices().remove(d1);
                    return;
                }
            }
        }
    }


    public AtomConnection getAtomConnection(WebSocket connection) {
        synchronized (connections) {
            for (AtomConnection ac : connections) {
                if (connection == ac.getConnection()) {
                    return ac;
                }
            }
        }
        return null;
    }
}
