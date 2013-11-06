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

require.config({
    paths: {
        NSDPlusPlus: "nsdLib",
        NSDPlusPlusSIO: "nsdLibSocketIO",
        jQuery: "../bootstrap/js/jQuery-1.8.2.min",
        bootstrap: "../bootstrap/js/bootstrap.min"
    }
});

require(["NSDPlusPlus", "when", "jQuery", "bootstrap"], function (NSDPlusPlus, when, $, _) {
    NSDPlusPlus.logger = function (s) {
        var ta = document.getElementById("ta");
        ta.textContent = s + "\n" + ta.textContent;
    };

    var service = null;

    window.switchOn = function() {
        if (service != null) {
            service.lightSwitch(true).then(switchCB);
        }
    }

    window.switchOff = function() {
        if (service != null) {
            service.lightSwitch(false).then(switchCB);
        }
    }

    function switchCB(response) {
        document.getElementById("paragraph").textContent = response;
    }

    function CB(services) {
        NSDPlusPlus.logger("CB " + services.length);
        //services.onservicefound.then(onservicefound);
        services.onservicefound = onservicefound;
        if (services.length > 0) {
            // select first of matching services, only one is supposed to match anyway
            service = NSDPlusPlus.bindService(services[0].id);
            document.getElementById("paragraph").textContent = "Service found";
        }
    }

    function onservicefound() {
        NSDPlusPlus.logger("onservicefound callback");
        NSD.getNetworkServices("upnp:urn:coltram-org:service:communicationtest:1").then(CB);
    }

    function connectedCB() {
        NSDPlusPlus.logger("connected");
        NSD.getNetworkServices("upnp:urn:coltram-org:service:communicationtest:1").then(CB);
    }

    require(["domReady"], function (domReady) {
        domReady(function () {
            NSDPlusPlus.connected.then(connectedCB);
            NSDPlusPlus.connect();
        });
    });

});
