package org.coltram.nsd.communication;

import org.coltram.nsd.services.BonjourService;
import org.coltram.nsd.types.ZCService;
import org.json.JSONException;
import org.json.JSONObject;
import pygmy.core.UUID;

import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetConnectedException;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

public class BonjourProcessor {
    private static Logger log = Logger.getLogger(BonjourProcessor.class.getName());

    private GeneralManager generalManager;

    public BonjourProcessor(GeneralManager GeneralManager) {
        this.generalManager = GeneralManager;
    }

    public void CallAction(JSONObject object, String serviceId,
                           final AtomConnection connection, String callBack) throws JSONException {
        final ZCService cbs = generalManager.getServiceManager().findBonjourService(serviceId);
        if (cbs == null) {
            log.info("no service with id "+serviceId+" in CallAction");
            return;
        }
        if (cbs.isLocal()) {
            // find the ColtramBonjourService in question
            BonjourService localcbs = BonjourService.getServiceById(serviceId);
            // send the info to that service
            object.put("address", "localhost");
            object.put("port", cbs.getPort());
            object.put("originAtom", connection.getId());
            localcbs.notifyListeners(object.toString());
        } else try {
            Socket socket = cbs.getSocket();
            final int port = socket.getPort();
            final InetAddress inetAddress = socket.getInetAddress();
            if (callBack != null) {
                // wait for reply on the same socket
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            ServerSocket serverSocket = connection.getServerSocket(port, inetAddress);
                            System.out.println("start server for reply " + serverSocket.getLocalPort());
                            Socket socket = serverSocket.accept();
                            BufferedReader dis
                                      = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            connection.getConnection().send(dis.readLine());
                        } catch (NotYetConnectedException e) {
                            Thread.yield();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                Thread.yield();
            }
            String ia = inetAddress.toString();
            if (ia.startsWith("/")) {
                ia = ia.substring(1);
            }
            object.put("address", ia);
            object.put("port", port + "");
            object.put("originAtom", connection.getId());
            DataOutputStream dos = cbs.getSocketDOS();
            System.out.println("send info to server " + object.toString());
            dos.writeBytes(object.toString());
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int bonjourServicePort = 0xDEAD + 1;

    private static final char[] _nibbleToHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static String toHex(byte[] code) {
        StringBuilder result = new StringBuilder(2 * code.length);

        for (byte aCode : code) {
            int b = aCode & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }

    public void exposeService(String serviceType, String friendlyName, String deviceType,
                              JSONObject service, String serviceId, AtomConnection connection) throws JSONException {
        try {
            String type = serviceType.substring(9);
            final HashMap<String, String> values = new HashMap<String, String>();
            values.put("DName", friendlyName);
            String desc = service.getJSONArray("actionList").toString();
            values.put("Desc", generalManager.addResource(UUID.createUUID().toString() + ".json", desc.getBytes()));
            values.put("txtvers", "1");
            Random random = new Random();
            byte[] name = new byte[6];
            random.nextBytes(name);
            ServiceInfo si = ServiceInfo.create(type, deviceType+"_"+toHex(name), bonjourServicePort, 0, 0, values);
            BonjourService coltramBonjourService =
                    new BonjourService(bonjourServicePort++, si, serviceId);
            generalManager.getConnectionManager().getJmdns().registerService(si);
            connection.add(coltramBonjourService);
            coltramBonjourService.start();
            log.fine("register " + si.getQualifiedName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process purpose:reply messages
     * only happens in Bonjour
     *
     * @param object the JSON message
     * @throws JSONException as usual when manipulating JSON objects
     */
    public void processReply(JSONObject object) throws JSONException {
        String address = object.getString("address");
        if ("localhost".equals(address)) {
            AtomConnection connection = generalManager.getConnectionManager().getConnectionByAtomId(object.getString("originAtom"));
            try {
                connection.getConnection().send(object.toString());
            } catch (NotYetConnectedException e) {
                Thread.yield();
            }
        } else {
            try {
                InetAddress inetAddress = InetAddress.getByName(address);
                int port = object.getInt("port");
                Socket socket = new Socket(inetAddress, port);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                BufferedWriter bw = new BufferedWriter(outputStreamWriter);
                System.out.println("sending reply");
                bw.write(object.toString());
                bw.flush();
                bw.close();
                outputStreamWriter.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
