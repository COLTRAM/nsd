package org.coltram.nsd.communication;

import org.coltram.nsd.types.GenericService;
import org.coltram.nsd.types.ZCService;
import org.teleal.cling.model.meta.Device;
import pygmy.core.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class GeneralManager {
    private ConnectionManager connectionManager;
    private ServiceManager serviceManager;
    //
    private final ArrayList<Device> devices;
    private final ArrayList<ZCService> services;

    private static final String webrootName = "tmpwww";
    private final static String webrootPort = "50005";
    private static String localhost = null;

    public GeneralManager() {
        //
        // setup of internal delegates
        //
        connectionManager = new ConnectionManager(this);
        serviceManager = new ServiceManager(this);
        //
        // setup of web server for bonjour descriptions and other non-UPnP related served filed
        //
        Properties properties = new Properties();
        properties.setProperty("endpoints", "http");
        properties.setProperty("urlhome", "/");
        properties.setProperty("http.port", webrootPort);
        properties.setProperty("http.class", "pygmy.core.SingleThreadedHttpEndPoint");
        properties.setProperty("handler", "coltramhandler");
        properties.setProperty("coltramhandler.class", "pygmy.handlers.FileHandler");
        properties.setProperty("coltramhandler.root", webrootName);
        properties.setProperty("coltramhandler.url-prefix", "/");
        properties.setProperty("mime.json", "application/json");
        properties.setProperty("mime.txt", "text/plain");
        localhost = connectionManager.getHostName();
        File webroot = new File(webrootName);
        try {
            if (webroot.exists()) {
                if (webroot.isDirectory()) {
                    //noinspection ConstantConditions
                    for (File f : webroot.listFiles()) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }
                //noinspection ResultOfMethodCallIgnored
                webroot.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            webroot.mkdir();
        } catch (NullPointerException e) {
            Thread.yield();
        }
        Server webServer = new Server(properties);
        webServer.start();
        //
        // setup of lists
        //
        devices = new ArrayList<Device>();
        services = new ArrayList<ZCService>();
        GenericService.init();
    }

    /**
     * add a resource to the web server
     *
     * @param name    the name of the resource, from which its URL is constructed
     * @param content the content of the resource
     * @return the URL of the resource
     */
    public String addResource(String name, byte content[]) {
        File res = new File(webrootName + File.separatorChar + name);
        if (res.exists()) {
            //noinspection ResultOfMethodCallIgnored
            res.delete();
        }
        try {
            FileOutputStream fos = new FileOutputStream(res);
            fos.write(content);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "http://" + localhost + ":" + webrootPort + "/" + name;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public Collection<Device> getDevices() {
        return devices;
    }

    public Collection<ZCService> getServices() {
        return services;
    }
}
