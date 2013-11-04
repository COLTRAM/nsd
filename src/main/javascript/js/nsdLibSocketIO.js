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
"use strict";

require.config({
    paths: {
        socketio: "js/socket.io.js"
    }
});

define("NSDPlusPlusSIO", ["when", "monitor/console", "socketio"], function (when, cons, sio) {
    var connectionInterface = null;
    var eventValues = [];
    //noinspection UnnecessaryLocalVariableJS
    var discoveredServices = [];
    //noinspection UnnecessaryLocalVariableJS
    var subscribeCallbacks = [];
    var initialized = when.defer();
    var connected = when.defer();
    var my = {
        discoveredServices: discoveredServices,
        initialized: initialized.promise,
        connected: connected.promise
    };

    var serviceImplementations = [];

    //
    // add function to Array, to remove the first occurence of obj in the array
    //
    Array.prototype.remove = function (obj) {
        var i = this.indexOf(obj);
        if (i >= 0) {
            this.splice(i, 1);
        }
    };

    my.isDiscovered = function (service) {
        if (service == null) {
            throw "testing null service for past discovery";
        }
        for (var i = 0; i < discoveredServices.length; i++) {
            //my.logger("checking "+discoveredServices[i].id+" "+service.id+" - "+(discoveredServices[i].id == service.id));
            if (discoveredServices[i].id == service.id) {
                return true;
            }
        }
        return false;
    };

    //
    // add NSD fields not present in Java
    //
    my.addDiscoveredService = function (serviceFromJava) {
        //my.logger("addDiscoveredService "+serviceFromJava.id);
        if (serviceFromJava == null) {
            throw new Error("service received from agent is null");
        }
        discoveredServices.push(serviceFromJava);
        // my.logger("discovered: "+serviceFromJava.type);
    };

    /////////////////////////////////////////////////////
    //// Objects ////////////////////////////////////////
    /////////////////////////////////////////////////////

    //
    // class Action
    //
    //noinspection JSUnusedLocalSymbols
    function Action(name, args) {
        this.name = name;
        this.args = args;
    }

    //
    // class Argument
    //
    //noinspection JSUnusedLocalSymbols
    function Argument(name, relatedStateVar, direction) {
        this.name = name;
        //noinspection JSUnusedGlobalSymbols
        this.relatedStateVariable = relatedStateVar;
        this.dir = direction;
    }

    my.ServiceImplementation = function () {
    };

    //my.nbOfServiceImplementations = function () {
    //    return serviceImplementations.length;
    //};

    my.getImplementation = function (implementationId) {
        //console.log("getImplementation", serviceImplementations, implementationId);
        return serviceImplementations[implementationId];
    };

    /////////////////////////////////////////////////////
    //// Connect ////////////////////////////////////////
    /////////////////////////////////////////////////////

    function errorFunction(msg) {
        my.logger("Error " + msg);
        for (var i in msg) {
            //noinspection JSUnfilteredForInLoop
            if (!(typeof msg[i] == 'function')) {
                //noinspection JSUnfilteredForInLoop
                my.logger(">" + i + ":" + msg[i]);
            }
        }
    }

    //
    // first call to lib: connection to the proxy
    //
    my.connect = function (host) {
        my.logger("connecting to " + host);
        try {
            connectionInterface = io.connect(host == null ? "http://localhost:57005/" : "http://" + host + ":57005/");
        } catch (e) {
            my.logger("error creating WebSocket " + e);
            my.logger("cannot continue...");
            return;
        }
        connectionInterface.on('connect', socketConnected);
        connectionInterface.on('connect_failed', function () {my.logger("connectionInterface.io connection failed")});
        connectionInterface.on('message', msgHandler);
        connectionInterface.on('disconnect', socketClosed);
    };

    //
    // internal: when a connectionInterface is connected
    //
    function socketConnected() {
        //
        // then resolve the connected promise
        //
        connected.resolve();
    }

    //
    // internal: message handler
    //
    function msgHandler(e) {
        var obj;
        try {
            // obj = JSON.parse(e);
            eval('obj = ' + e); // works better on long strings ?
        } catch (error) {
            my.logger("+------+");
            my.logger("|" + error + " " + e);
            my.logger("+------+");
        }
        // my.resetLog();
        // my.logger(obj.purpose+" "+(e.data+"").substr(0, 400));
        messageHandlers[obj.purpose](obj);
    }

    //
    // internal: msg handling functions
    //
    var messageHandlers = {};

    //
    // allow to define message handlers from outside the module
    //
    my.addMessageHandler = function (name, func) {
        messageHandlers[name] = func;
    };

    //
    // internal: when a connectionInterface is closed
    //
    function socketClosed() {
        my.logger("Disconnected");
    }

    //noinspection JSUnusedLocalSymbols
    my.logger = function (s) {};

    my.resetLog = function () {};

    // allows to keep a unique reference to the server service object
    var exposeduniqueSequence = 1;
    var exposeduniqueTimestamp = new Date().getTime();
    var getExposedUniqueId = function (type, protocol) {
        var id = type + "_" + protocol + "_" + exposeduniqueTimestamp + "_" + (exposeduniqueSequence++);
        return id;
    };

    /////////////////////////////////////////////////////
    //// Expose  ////////////////////////////////////////
    /////////////////////////////////////////////////////
    //
    // expose a service
    //
    my.expose = function (type, protocol, serviceImplementation) {
        if (arguments.length < 3) {
            throw "my.expose called with not enough parameters";
        }
        var obj = {};
        obj.purpose = "exposeService";
        obj.localService = {};
        obj.localService.uniqueId = getExposedUniqueId(type, protocol);
        obj.localService.type = type;
        obj.localService.protocol = protocol;
        obj.localService.actionList = computeServiceDescriptionFromImplementation(serviceImplementation);
        obj.localService.eventList = null;
        if (serviceImplementation.EVENTS) {
            obj.localService.eventList = serviceImplementation.EVENTS;
            for (var i = 0; i < serviceImplementation.EVENTS.length; i++) {
                eventValues[serviceImplementation.EVENTS[i]] = null;
            }
        }
        //
        // assumption for the moment:
        // - max one service per atom
        // - the developer has to create a my.ServiceImplementation object to implement the actions
        //
        serviceImplementation.service = obj.localService;
        serviceImplementations.push(serviceImplementation);
        obj.serviceImplementation = "" + (serviceImplementations.length - 1);
        connectionInterface.send(JSON.stringify(obj));
        return obj.localService.uniqueId;
    };

    my.unexpose = function (uniqueId) {
        //console.log("unexpose: ", uniqueId);
        var obj = {};
        obj.purpose = "unexposeService";
        obj.localService = {};
        obj.localService.uniqueId = uniqueId;
        connectionInterface.send(JSON.stringify(obj));
    };

    //
    //
    //
    function computeServiceDescriptionFromImplementation(implementation) {
        var result = [];
        for (var f in implementation) {
            try {
                if (implementation.hasOwnProperty(f) && typeof implementation[f] == 'function') {
                    // create the action
                    var action = {};
                    action.name = f;
                    action.args = [];
                    result.push(action);
                    var source = implementation[f].toString();
                    //my.logger(f + " :" + source);
                    // find the IN args
                    var beginArgs = source.indexOf('(');
                    var endArgs = source.indexOf(')');
                    // if there are in args (no () )
                    if (endArgs > beginArgs + 1) {
                        var args = source.substring(beginArgs + 1, endArgs).split(',');
                        for (var i = 0; i < args.length; i++) {
                            var arg = {};
                            arg.name = args[i].trim();
                            arg.dir = "IN";
                            action.args.push(arg);
                        }
                    }
                    // find the OUT args
                    var out = source.indexOf("return");
                    // if there is a return
                    if (out >= 0) {
                        var outbrace = source.indexOf('{', out);
                        var outcolon = source.indexOf(';', out);
                        // and if there is a {} before the ;, so there are OUT args
                        if (outbrace < outcolon) {
                            var outargs = source.substring(outbrace + 1, source.indexOf('}', outbrace + 1)).split(',');
                            // this is like the inside of a JSON object
                            for (var j = 0; j < outargs.length; j++) {
                                var arg1 = {}, k = outargs[j].split(':');
                                arg1.name = k[0].trim();
                                arg1.dir = "OUT";
                                action.args.push(arg1);
                            }
                        }
                    }
                }
            } catch (error) {
                throw "error parsing service implementation at " + f + " :" + error;
            }
        }
        return result;
    }

    /////////////////////////////////////////////////////
    //// updateEvent ////////////////////////////////////
    /////////////////////////////////////////////////////
    my.updateEvent = function (eventName, eventValue) {
        // propagate the value to the agent
        if (eventValues[eventName] != eventValue) {
            var obj = {};
            obj.purpose = "updateEvent";
            obj.eventName = eventName;
            obj.eventValue = JSON.stringify(eventValue);
            connectionInterface.send(JSON.stringify(obj));
            eventValues[eventName] = eventValue;
        }
    };

    /////////////////////////////////////////////////////
    //// Bind    ////////////////////////////////////////
    /////////////////////////////////////////////////////
    function getServiceByServiceId(serviceId) {
        for (var i = 0; i < discoveredServices.length; i++) {
            if (discoveredServices[i].id == serviceId) {
                return discoveredServices[i];
            }
        }
        return null;
    }

    var deferredTable = [];

    function getTokenForDeferred(deferred) {
        deferredTable.push(deferred);
        return (deferredTable.length - 1) + "";
    }

    function getDeferredFromToken(token) {
        return deferredTable[+token];
    }

    //
    // bind service, creating a proxy object with one function per action
    //
    my.bindService = function (serviceId) {
        var service = getServiceByServiceId(serviceId);
        if (service == null) {
            my.logger("service with id " + serviceId + " not found in bindService");
            return null;
        } //else {
        //Coltram.logger("service "+serviceId);
        //for (var i = 0; i < service.eventList.length; i++) {
        //    Coltram.logger(service.eventList[i]);
        //}
        //}
        if (service.actionList == null) {
            my.logger("trying to bind to a service with no actionList: serviceId=" + serviceId);
            return null;
        }
        // create proxy object
        var proxy = {};
        for (var i = 0; i < service.actionList.length; i++) {
            proxy[service.actionList[i].name] = createProxyFunction(serviceId, service.actionList[i].name, service.actionList[i].args);
        }
        if (service.eventList) {
            proxy.subscribe = function (eventList, serviceId, conn) {
                return function (eventName, callback) {
                    if (typeof callback != 'function') {
                        //my.logger("callback " + callback + " should be a function in subscribe");
                        throw new Error("callback " + callback + " should be a function in subscribe");
                    }
                    if (eventList.indexOf(eventName) >= 0) {
                        var obj = {};
                        obj.purpose = "subscribe";
                        obj.serviceId = serviceId;
                        obj.eventName = eventName;
                        obj.callback = subscribeCallbacks.push(callback) - 1;
                        var s = JSON.stringify(obj);
                        //my.logger(s);
                        connectionInterface.send(s);
                    } else {
                        //my.logger("event " + eventName + " is not part of the interface of this service");
                        throw new Error("event " + eventName + " is not part of the interface of this service");
                    }
                }
            }(service.eventList, serviceId, connectionInterface);
            proxy.unsubscribe = function (eventList, serviceId, conn) {
                return function (eventName, callback) {
                    if (eventList.indexOf(eventName) >= 0) {
                        var obj = {};
                        obj.purpose = "unsubscribe";
                        obj.serviceId = serviceId;
                        obj.eventName = eventName;
                        var s = JSON.stringify(obj);
                        //my.logger(s);
                        connectionInterface.send(s);
                    } else {
                        //my.logger("event " + eventName + " is not part of the interface of this service");
                        throw new Error("event " + eventName + " is not part of the interface of this service");
                    }
                }
            }(service.eventList, serviceId, connectionInterface);
        } else {
            proxy.subscribe = function () {
                //my.logger("this service does not have events");
                throw new Error("this service does not have events");
            };
            proxy.unsubscribe = function () {
                //my.logger("this service does not have events");
                throw new Error("this service does not have events");
            };
        }
        return proxy;
    };

    function createProxyFunction(serviceId, actionName, args) {
        return function () {
            //my.logger("enter proxy "+actionName);
            var obj = {};
            obj.purpose = 'callAction';
            obj.serviceId = serviceId;
            obj.actionName = actionName;
            obj["arguments"] = {};
            var j = 0;
            for (var i = 0; i < args.length; i++) {
                if (args[i].dir.toLowerCase() == "in") {
                    if (j >= arguments.length) {
                        my.logger("not enough arguments provided to a proxy function: " + actionName);
                        return;
                    }
                    obj["arguments"][args[i].name] = JSON.stringify(arguments[j++]);
                }
            }
            if (j < arguments.length) {
                my.logger("too many arguments provided to a proxy function: " +
                        actionName + " " + (arguments.length - j));
                return;
            }
            var deferred = when.defer();
            obj.replyCallBack = getTokenForDeferred(deferred);
            var s = JSON.stringify(obj);
            connectionInterface.send(s);
            return deferred.promise;
        };
    }

    /////////////////////////////////////////////////////
    //// Messages from Agent ////////////////////////////
    /////////////////////////////////////////////////////

    messageHandlers.updateEvent = function (obj) {
        var i = +obj.callback;
        if (i < 0 || i >= subscribeCallbacks.length) {
            throw new Error("subscribe callback index out of range: " + i);
        }
        my.logger("receive upd " + obj.eventName + " |" + obj.eventValue + "|");
        subscribeCallbacks[i](obj.eventValue ? JSON.parse(obj.eventValue) : null);
    };

    //
    // message sent by the agent to inform the atom about ids and names
    //
    messageHandlers.initialize = function (obj) {
        my.hostName = obj.hostName;
        my.agentHostName = obj.agentHostName;
        //
        // then resolve the initialized promise
        //
        initialized.resolve();
    };

    //
    // message sent by the agent getting a reply to a callAction
    // variant with reply arguments as individual parameters to a function
    //
    messageHandlers.reply = function (obj) {
        var deferred = getDeferredFromToken(obj.callBack);
        var service = getServiceByServiceId(obj.serviceId);
        if (service == null) {
            my.logger("reply from an unknown service " + obj.serviceId + " " + obj.actionName);
            return;
        }
        for (var i = 0; i < service.actionList.length; i++) {
            if (service.actionList[i].name == obj.actionName) {
                var replyArgs = [];
                var al = service.actionList[i].args;
                for (var j = 0; j < al.length; j++) {
                    // one path to here has arguments directly in obj, the other in obj.arguments
                    var property = obj[al[j].name];
                    if (property) {
                        replyArgs.push(property);
                    } else if (obj["arguments"]) {
                        property = obj["arguments"][al[j].name];
                        if (property) {
                            replyArgs.push(property);
                        }
                    }
                }
                deferred.resolve(replyArgs);
                return;
            }
        }
        my.logger("reply with an unknown actionName " + obj.actionName + " " + obj.serviceId);
    };

    //
    // message sent by the agent when someone else calls an action of a service exposed by this atom
    //
    messageHandlers.serviceAction = function (obj) {
        var impl = serviceImplementations[parseInt(obj.implementation)];
        serviceActionInternal(obj, impl, obj);
    };

    function serviceActionInternal(obj, impl, args2) {
        //my.logger("service action "+obj.actionName);
        if (typeof impl[obj.actionName] == 'function') {
            // prepare the arguments from the service action description, one arg per argument described as 'in'
            var args = [];
            for (var i = 0; i < impl.service.actionList.length; i++) {
                var action = impl.service.actionList[i];
                if (action.name == obj.actionName) {
                    for (var j in action.args) {
                        if (action.args.hasOwnProperty(j)) {
                            var arg = action.args[j];
                            if (arg.dir.toLowerCase() == "in") {
                                args.push(JSON.parse(args2[arg.name]));
                            }
                        }
                    }
                }
            }
            var result = impl[obj.actionName].apply(obj, args);
            //my.logger("service action result:"+JSON.stringify(result));
            if (typeof result != 'undefined') {
                processResult(result, obj);
            }
        }
        else {
            my.logger("unimplemented action " + obj.actionName + " in locally exposed service");
        }
    }

    //
    // message sent by the agent when someone else calls an action of a bonjour service exposed by this atom
    //
    messageHandlers.callAction = function (obj) {
        // when someone else calls an action of a service exposed by this atom
        // TODO: this code assumes one service per page, so the serviceID is ignored --> allow more services and deal with serviceId
        serviceActionInternal(obj, serviceImplementations[0], obj["arguments"]);
    };

    function processResult(result, obj) {
        // check that all returned values are strings
        for (var i in result) {
            if (result.hasOwnProperty(i) && typeof result[i] != 'string') {
                result[i] = "" + result[i];
                //my.logger("reply argument " + i + " of " + obj.actionName + " has a non-string value");
            }
        }
        var answer = {};
        answer.purpose = 'reply';
        answer["arguments"] = result;
        answer.callBack = obj.replyCallBack;
        if (obj.hasOwnProperty("address")) {
            answer.address = obj.address;
            answer.replyPort = obj.replyPort;
            if (obj.hasOwnProperty("originAtom")) {
                answer.originAtom = obj.originAtom;
            }
            answer.serviceId = obj.serviceId;
            answer.actionName = obj.actionName;
        }
        connectionInterface.send(JSON.stringify(answer));
    }

    //
    // end internal: msg handlers
    //

    //
    // looser version of getNetworkServices, searching for a substring of the type
    // to be independent from the actual protocol
    //
    my.discover = function (serviceTypeFragment) {
        //my.logger("discover");
        var deferred = when.defer();
        setTimeout(waiter1(serviceTypeFragment, deferred), 1);
        //my.logger("returning "+deferred.promise);
        return deferred.promise;
    };

    function waiter1(st, de) {
        return function () {reallyDiscover(st, de);};
    }

    function reallyDiscover(serviceTypeFragment, deferred) {
        //my.logger("reallyDiscover");
        var discoveredCallback = null;
        for (var i = 0; i < specificServiceDiscoveredCallbacks.length; i++) {
            if (serviceTypeFragment == specificServiceDiscoveredCallbacks[i].type) {
                discoveredCallback = specificServiceDiscoveredCallbacks[i];
                break;
            }
        }
        var networkServices = (discoveredCallback ?
                discoveredCallback.networkServices.refresh() :
                new NetworkServices());
        // look for services already discovered
        getAlreadyDiscoveredServices1(serviceTypeFragment, deferred, networkServices);
        // register a callback for future discoveries
        registerSpecificServicesCallback1(serviceTypeFragment, networkServices);
        //networkServicesArray.push(result);
    }

    function getAlreadyDiscoveredServicesInternal1(serviceTypeFragment, result) {
        //my.logger("getAlreadyDiscoveredServicesInternal1");
        for (var i = 0; i < discoveredServices.length; i++) {
            var service = discoveredServices[i];
            if (service.type.indexOf(serviceTypeFragment) >= 0) {
                //possibly the service is already in the result
                var absent = true;
                for (var j = 0; j < result.length; j++) {
                    absent &= (result[j].id != service.id);
                }
                if (absent) {
                    result.push(service);
                    result.servicesAvailable++;
                }
            }
        }
    }

    //noinspection JSUnusedLocalSymbols
    function getAlreadyDiscoveredServices1(serviceTypeFragment, deferred, result) {
        //my.logger("getAlreadyDiscoveredServices1");
        if (typeof serviceTypeFragment == "string") {
            // single type requested
            getAlreadyDiscoveredServicesInternal1(serviceTypeFragment, result);
        } else {
            // multiple types requested
            for (var i = 0; i < serviceTypeFragment.length; i++) {
                getAlreadyDiscoveredServicesInternal1(serviceTypeFragment[i], result);
            }
        }
        my.userValidation(result, deferred);
    }

    function registerSpecificServicesCallback1(serviceTypeFragment, networkServices) {
        //my.logger("registerSpecificServicesCallback1");
        if (typeof serviceTypeFragment == "string") {
            if (newServiceTypeCallback(serviceTypeFragment)) {
                specificServiceDiscoveredCallbacks.push(new SpecificServiceCallBack1(serviceTypeFragment, networkServices));
            }
        } else {
            // multiple types requested
            for (var i = 0; i < serviceTypeFragment.length; i++) {
                if (newServiceTypeCallback(serviceTypeFragment)) {
                    specificServiceDiscoveredCallbacks.push(new SpecificServiceCallBack1(serviceTypeFragment[i], networkServices));
                }
            }
        }
    }

    function SpecificServiceCallBack1(serviceTypeFragment, networkServices) {
        this.type = serviceTypeFragment;
        this.networkServices = networkServices;
    }

    SpecificServiceCallBack1.prototype.compare = function (service) {
        return service.type.indexOf(this.type) >= 0;
    };

    ///////////////////////////////////////////////////////////////////////////////
    ////// section for Network Service Discovery //////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    var specificServiceDiscoveredCallbacks = []; //, networkServicesArray = [];

    my.getNetworkServices = function (serviceType) {
        my.logger("getNS " + serviceType);
        var deferred = when.defer();
        setTimeout(waiter(serviceType, deferred), 1);
        //my.logger("returning from getNS");
        return deferred.promise;
    };

    // if you want to implement user validation, overload this function
    my.userValidation = function (ns, deferred) {
        //my.logger("userValidation "+ns);
        deferred.resolve(ns);
    };

    function waiter(st, de) {
        return function () {reallyGetNS(st, de)};
    }

    function reallyGetNS(serviceType, deferred) {
        //my.logger("reallyGetNS "+serviceType);
        var discoveredCallback = null;
        for (var i = 0; i < specificServiceDiscoveredCallbacks.length; i++) {
            if (serviceType == specificServiceDiscoveredCallbacks[i].type) {
                discoveredCallback = specificServiceDiscoveredCallbacks[i];
                break;
            }
        }
        var networkServices = (discoveredCallback ?
                discoveredCallback.networkServices.refresh() :
                new NetworkServices());
        // look for services already discovered
        getAlreadyDiscoveredServices(serviceType, deferred, networkServices);
        // register a callback for future discoveries
        registerSpecificServicesCallback(serviceType, networkServices);
        //networkServicesArray.push(result);
    }

    function getAlreadyDiscoveredServicesInternal(serviceType, result) {
        //my.logger("getAlreadyDiscoveredServicesInternal");
        for (var i = 0; i < discoveredServices.length; i++) {
            var service = discoveredServices[i];
            if (service.type == serviceType) {
                //possibly the service is already in the result
                var absent = true;
                for (var j = 0; j < result.length; j++) {
                    absent &= (result[j].id != service.id);
                }
                if (absent) {
                    result.push(service);
                    result.servicesAvailable++;
                }
            }
        }
    }

    //noinspection JSUnusedLocalSymbols
    function getAlreadyDiscoveredServices(serviceType, deferred, result) {
        //my.logger("getAlreadyDiscoveredServices");
        if (typeof serviceType == "string") {
            // single type requested
            getAlreadyDiscoveredServicesInternal(serviceType, result);
        } else {
            // multiple types requested
            for (var i = 0; i < serviceType.length; i++) {
                getAlreadyDiscoveredServicesInternal(serviceType[i], result);
            }
        }
        my.userValidation(result, deferred);
    }

    function registerSpecificServicesCallback(serviceType, networkServices) {
        //my.logger("registerSpecificServicesCallback");
        if (typeof serviceType == "string") {
            if (newServiceTypeCallback(serviceType)) {
                specificServiceDiscoveredCallbacks.push(new SpecificServiceCallBack(serviceType, networkServices));
            }
        } else {
            // multiple types requested
            for (var i = 0; i < serviceType.length; i++) {
                if (newServiceTypeCallback(serviceType[i])) {
                    specificServiceDiscoveredCallbacks.push(new SpecificServiceCallBack(serviceType[i], networkServices));
                }
            }
        }
    }

    function newServiceTypeCallback(serviceType) {
        for (var i = 0; i < specificServiceDiscoveredCallbacks.length; i++) {
            if (serviceType == specificServiceDiscoveredCallbacks[i].type) {
                return false;
            }
        }
        return true;
    }

    function SpecificServiceCallBack(serviceType, networkServices) {
        this.type = serviceType;
        this.networkServices = networkServices;
    }

    SpecificServiceCallBack.prototype.compare = function (service) {
        return this.type == service.type;
    };

    NetworkServices.prototype = [];

    NetworkServices.prototype.refresh = function () {
        this.onserviceavailableDeferred = when.defer();
        this.onserviceavailable = this.onserviceavailableDeferred.promise;
        this.onserviceunavailableDeferred = when.defer();
        this.onserviceunavailable = this.onserviceunavailableDeferred.promise;
        return this;
    };

    function NetworkServices() {
        this.servicesAvailable = 0;
        this.refresh();
    }

    my.wrapInNetworkServices = function (service) {
        var ns = new NetworkServices();
        ns.push(service);
        ns.servicesAvailable = 1;
        return ns;
    };

    my.getEmptyNetworkServices = function (service) {
        return new NetworkServices();
    };

    //noinspection JSUnusedGlobalSymbols
    NetworkServices.prototype.getServiceById = function (id) {
        for (var service in this) {
            if (this.hasOwnProperty(service) && service.id == id) {
                return service;
            }
        }
        return null;
    };

    //
    // internal: call all callbacks for service discovery
    // obj is an array of services
    //
    my.callSpecificServiceDiscoveredCallbacks = function (newlyDiscoveredServices) {
        for (var cb = 0; cb < specificServiceDiscoveredCallbacks.length; cb++) {
            for (var i = 0; i < newlyDiscoveredServices.length; i++) {
                var newService = newlyDiscoveredServices[i];
                if (specificServiceDiscoveredCallbacks[cb].compare(newService)) {
                    var networkServices = specificServiceDiscoveredCallbacks[cb].networkServices;
                    // we know it is a new newService, otherwise it would have been filtered in the caller
                    networkServices.servicesAvailable++;
                    networkServices.onserviceavailableDeferred.resolve();
                    // if it is this newService, mark as online and call onserviceonline
                    for (var j = 0; j < networkServices.length; j++) {
                        if (networkServices[j].id == newService.id) {
                            networkServices[j].online = true;
                            if (typeof networkServices[j].onserviceonline == 'function') {
                                networkServices[j].onserviceonline.call(null);
                            }
                        }
                    }
                }
            }
        }
    };

    //
    // internal: call all callbacks for service removal
    // obj is an array of services
    //
    my.callServiceRemovedCallbacks = function (obj) {
        for (var cb = 0; cb < specificServiceDiscoveredCallbacks.length; cb++) {
            for (var i = 0; i < obj.length; i++) {
                var service = obj[i];
                if (specificServiceDiscoveredCallbacks[cb].compare(obj[i])) {
                    var networkServices = specificServiceDiscoveredCallbacks[cb].networkServices;
                    // we know it is a removed service, otherwise it would have been filtered in the caller
                    networkServices.servicesAvailable--;
                    networkServices.onserviceunavailableDeferred.resolve();
                    // if it is this service, mark as online=false and call onserviceoffline
                    for (var j = 0; j < networkServices.length; j++) {
                        if (networkServices[j].id == service.id) {
                            networkServices[j].online = false;
                            if (typeof networkServices[j].onserviceoffline == 'function') {
                                networkServices[j].onserviceoffline.call(null);
                            }
                        }
                    }
                }
            }
        }
    };

    /////////////////////////////////////////////////////
    //// Events  ////////////////////////////////////////
    /////////////////////////////////////////////////////

    //
    // event callbacks
    //
    var serviceDiscoveredCallbacks = [];
    var serviceRemovedCallbacks = [];
    // end event callbacks

    //
    // add event listener (generic mechanism)
    //
    my.addEventListener = function (eventType, callBack) {
        if (typeof callBack != 'function') {
            return;
        }
        switch (eventType) {
            case "serviceDiscovered":
                serviceDiscoveredCallbacks.push(callBack);
                break;
            case "serviceRemoved":
                serviceRemovedCallbacks.push(callBack);
                break;
            case "connected":
                throw new Error("connected is now implemented as a promise");
            case "initialized":
                throw new Error("initialized is now implemented as a promise");
            default:
                my.logger("unknown event " + eventType);
                break;
        }
    };

    //
    // remove event listener (generic mechanism)
    //
    my.removeEventListener = function (eventType, callBack) {
        if (typeof callBack != 'function') {
            return;
        }
        switch (eventType) {
            case "serviceDiscovered":
                serviceDiscoveredCallbacks.remove(callBack);
                break;
            case "serviceRemoved":
                serviceRemovedCallbacks.remove(callBack);
                break;
            case "connected":
                throw new Error("connected is now implemented as a promise");
            case "initialized":
                throw new Error("initialized is now implemented as a promise");
            default:
                my.logger("unknown event " + eventType);
                break;
        }
    };

    my.addMessageHandler("serviceDiscovered", function (obj) {
        //my.logger("service discovered " + obj.services.length);
        var i, newServices = [];
        for (i = 0; i < obj.services.length; i++) {
            if (!my.isDiscovered(obj.services[i])) {
                my.addDiscoveredService(obj.services[i]);
                //my.logger("D:" + obj.services[i].type + " " + obj.services[i].name);
                newServices.push(obj.services[i]);
            }
        }
        if (newServices.length > 0) {
            callServiceDiscoveredCallbacks(newServices);
        }
    });

    //
    // special remove for array of objects
    //
    function removeSpecial(array, object) {
        for (var i = 0; i < array.length; i++) {
            if (array[i].id == object.id &&
                    array[i].name == object.name &&
                    array[i].type == object.type) {
                array.splice(i, 1);
                //my.logger("success removing service " +object.id);
                return;
            }
        }
        //my.logger("could not remove service " +object.id);
    }

    my.addMessageHandler("serviceRemoved", function (obj) {
        var i, removedServices = [];
        for (i = 0; i < obj.services.length; i++) {
            if (my.isDiscovered(obj.services[i])) {
                removeSpecial(discoveredServices, obj.services[i]);
                removedServices.push(obj.services[i]);
            }
        }
        //my.logger("R:"+obj.purpose+" "+obj.name);
        if (removedServices.length > 0) {
            callServiceRemovedCallbacks(removedServices);
        }
    });

    //
    // internal: call all callbacks for service discovery
    //
    function callServiceDiscoveredCallbacks(obj) {
        for (var cb in serviceDiscoveredCallbacks) {
            if (serviceDiscoveredCallbacks.hasOwnProperty(cb) &&
                    typeof serviceDiscoveredCallbacks[cb] == 'function') {
                serviceDiscoveredCallbacks[cb].call(obj);
            }
        }
        my.callSpecificServiceDiscoveredCallbacks(obj);
    }

    //
    // internal: call all callbacks for service removal
    //
    function callServiceRemovedCallbacks(obj) {
        for (var cb in serviceRemovedCallbacks) {
            if (serviceRemovedCallbacks.hasOwnProperty(cb) &&
                    typeof serviceRemovedCallbacks[cb] == 'function') {
                serviceRemovedCallbacks[cb].call(obj);
            }
        }
        my.callServiceRemovedCallbacks(obj);
    }

    window.NSD = {
        getNetworkServices: my.getNetworkServices
    };

    return my;
});