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

require(["NSDPlusPlusSIO", "when", "jQuery", "bootstrap"], function (NSDPlusPlus, when, $, _) {
    NSDPlusPlus.logger = function (s) {
        var ta = document.getElementById("ta");
        ta.textContent = s + "\n" + ta.textContent;
    };

    var serviceImplementation = new NSDPlusPlus.ServiceImplementation();

    serviceImplementation.lightSwitch = function (newTargetValue) {
        NSDPlusPlus.logger("Sw:" + newTargetValue + "\n");
        if (newTargetValue == false) {
            document.getElementById("light").style.cssText = "background-image: url('res/Lightbulb_off.svg')";
        } else {
            document.getElementById("light").style.cssText = "background-image: url('res/lightOn.svg')";
        }
        return {response: "switched on " + new Date().toLocaleTimeString()};
    };

    serviceImplementation.EVENTS = ["progress"];

    var state = 0;

    function eventing() {
        NSDPlusPlus.updateEvent("progress", state++);
        setTimeout(eventing, 3000);
    }

    require(["domReady"], function (domReady) {
        domReady(function () {
            NSDPlusPlus.connected.then(function () {
                NSDPlusPlus.expose("communicationtest", "upnp", serviceImplementation);
                NSDPlusPlus.logger("UPnP service 'communicationtest' exposed");
            });
            NSDPlusPlus.connect();
            NSDPlusPlus.initialized.then(function () { setTimeout(eventing, 1000); });
        });
    });

});