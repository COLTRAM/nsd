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
import org.teleal.cling.model.action.AbstractActionExecutor;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.LocalService;

import java.nio.channels.NotYetConnectedException;
import java.util.Iterator;
import java.util.logging.Logger;

public class GenericServiceActionExecutor extends AbstractActionExecutor {
	private static Logger log = Logger.getLogger(GenericServiceActionExecutor.class.getName());
	private AtomConnection connection;
	private String serviceImplementationName;

	public GenericServiceActionExecutor(AtomConnection connection, String serviceImplementationName) {
		this.connection = connection;
		this.serviceImplementationName = serviceImplementationName;
	}

    @Override
   	public void execute(final ActionInvocation<LocalService> actionInvocation) {
   		execute(actionInvocation, null);
   	}

   	@Override
   	public void execute(final ActionInvocation<LocalService> actionInvocation, Object o) {
   		log.finer("ENTERING execute" + " - " + actionInvocation.getAction().getName());
   		JSONObject object = new JSONObject();
   		try {
   			object.put("purpose", "serviceAction");
   			object.put("implementation", serviceImplementationName);
   			object.put("actionName", actionInvocation.getAction().getName());
   			try {
   				for (ActionArgument<LocalService> argument : actionInvocation.getAction().getInputArguments()) {
   					ActionArgumentValue<LocalService> inputValue = actionInvocation.getInput(argument);
   					object.put(argument.getName(), inputValue.toString());
   				}
   			} catch (NullPointerException e) {
   				log.throwing(GenericServiceActionExecutor.class.getName(), "Exception processing arguments of a call to an exposed service: " + actionInvocation.getAction().getName(), e);
   				return;
   			}
   			log.finer("EXECUTING action: " + object.toString());
   		} catch (JSONException e) {
   			log.throwing(GenericServiceActionExecutor.class.getName(), "execute", e);
   		}
   		syncAction(actionInvocation, connection, object);
   	}

	/**
	 * 
	 * @param actionInvocation
	 * @param connection
	 * @param object
	 */
	public synchronized static void syncAction(final ActionInvocation<LocalService> actionInvocation, AtomConnection connection, JSONObject object) {
		JSONObject call = object;
		log.finer("ENTERING SYNCHRONIZED BLOCK syncAction" + " - " + call.toString());
		if (actionInvocation.getAction().getOutputArguments().length > 0) {
			// set the flag that I am waiting for reply, which will redirect the reply message
			log.finer("calling an action with reply: " + actionInvocation.getAction().getName());
			connection.setWaitingForReply(true);
			// send the action command
			try {
				String s = object.toString();
				connection.getConnection().send(s);
				// ColtramLogger.logln("out>" + s);
			} catch (NotYetConnectedException e) {
				log.throwing(GenericServiceActionExecutor.class.getName(), "syncAction", e);
			}
			// while no reply has been received, wait
			int j = 0;
			long t1 = System.currentTimeMillis();
			log.finer("Start waiting" + call.toString());

			while (connection.isWaitingForReply() && j++ < 2000) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					log.throwing(GenericServiceActionExecutor.class.getName(), "syncAction", e);
				}
			}
			long t2 = System.currentTimeMillis();
			log.finer("End waiting in " + (t2 - t1) + " ms - " + call.toString());

			// get the reply information
			object = connection.getReply();
			if (object != null) {
				connection.setReply(null);
				try {
					// the reply information has one field purpose="reply" and
					// one arguments array
					JSONObject arguments = object.getJSONObject("arguments");
					Iterator keys = arguments.keys();
					while (keys.hasNext()) {
						String name = (String) keys.next();
						try {
							actionInvocation.setOutput(name, arguments.getString(name));
						} catch (IllegalArgumentException e) {
							log.warning("IllegalArgumentException in reply to " + actionInvocation.getAction().getName() + ": " + name + " " + arguments.getString(name));
						}
					}
				} catch (JSONException e) {
					log.warning("Exception processing arguments of a reply from an exposed service: " + actionInvocation.getAction().getName());
				}
				log.finer("end of waitingForReply sequence");
			} else {
				connection.setWaitingForReply(false);
				log.warning("Waited, but no or null reply - " + call.toString());
			}
		} else {
			// just send and do not wait
			log.finer("calling an action without reply option");
			try {
				String s = object.toString();
				connection.getConnection().send(s);
				// ColtramLogger.logln("out>" + s);
			} catch (NotYetConnectedException e) {
				log.throwing(GenericServiceActionExecutor.class.getName(), "syncAction", e);
			}
		}
	}
}
