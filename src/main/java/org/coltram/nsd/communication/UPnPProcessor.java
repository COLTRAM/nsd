package org.coltram.nsd.communication;

import org.coltram.nsd.types.ActionList;
import org.coltram.nsd.types.GenericService;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;

import java.nio.channels.NotYetConnectedException;
import java.util.logging.Logger;

public class UPnPProcessor {
    private static Logger log = Logger.getLogger(UPnPProcessor.class.getName());

    private GeneralManager generalManager;

    public UPnPProcessor(GeneralManager deviceManager) {
        this.generalManager = deviceManager;
    }

    public void CallAction(String serviceId, String actionName, JSONObject arguments,
                           AtomConnection connection, String callBack, boolean mappedReply) {
        Service s = generalManager.getServiceManager().findService(serviceId);
        if (s == null) {
            // service does not exist (any more ?)
            if (actionName.equals("ping")) {
                // do nothing;
                log.info("pinged unexisting service " + serviceId);
            } else {
                log.info("no service with id " + serviceId + " (" + actionName + ")");
            }
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
        Action action = service.getAction(actionName);
        if (action == null) {
            log.info("unknown action for this service: " + actionName + " " + service.getReference().toString());
            return;
        }
        NSDActionInvocation setTargetInvocation =
                new NSDActionInvocation(action, args);
        generalManager.getConnectionManager().getUpnpService().getControlPoint().execute(
                new ActionCallback(setTargetInvocation) {
                    @Override
                    public void success(ActionInvocation invocation) {
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
            actionList = new ActionList(service.getJSONArray("actionList"), connection, serviceImplementationName);
        } catch (RuntimeException e) {
            // error in service description, translated to runtimeexception, so stop processing
            return;
        }
        try {
            GenericService<GenericService> genericService = new GenericService<GenericService>(ServiceType.valueOf(serviceType.substring(5)),
                    ServiceId.valueOf(serviceId), actionList.actionExecutors(),
                    actionList.stateVariableAccessors(), actionList.stringConvertibleTypes(), false,
                    serviceImplementationName);
            genericService.setManager(new DefaultServiceManager<GenericService>(genericService, GenericService.class));
            LocalDevice newDevice = new LocalDevice(new DeviceIdentity(new UDN(friendlyName)),
                    DeviceType.valueOf(deviceType),
                    new DeviceDetails(friendlyName),
                    genericService);
            generalManager.getConnectionManager().getUpnpService().getRegistry().addDevice(newDevice);
            connection.add(newDevice);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }
}
