# nsd

##Network Service Discovery API implementation

I have implemented the Network Service Discovery API [http://www.w3.org/TR/discovery-api/] 
on top of UPnP and Bonjour as a Java proxy for any modern browser, coupled with a JS library. 
The JS library handles the connection with the Java proxy through WebSocket and JSON messages.
This has been tested on PCs and Macs, with recent versions of Chrome, Firefox, Safari and Opera. 
It also works on Windows 7/IE10. Linux should not be a problem, but was not tested.

The project structure is compatible with Maven, and a pom.xml is provided.

## What's new

Version 2:
- Latest NSD draft as of Nov 1, 2013, with one modification: onservice[un]available are not callbacks but promises
- Many bug fixes
- Refactoring

## Quick start

Run the agent in a terminal with the command line:

	java -jar nsd-websocket-jar-with-dependencies.jar

or

	java -jar nsd-socketio-jar-with-dependencies.jar

depending on which communication protocol you want to use between the browser and the proxy.

Running the agent in the terminal is necessary if you want to see the error log. 
Run the agent before you load any of the examples in a browser.

Place the content of the examples directory in the document root of a web server.
Also place a copy of the js directory in the document root.
Also place a copy of the content of bootstrap.zip so that your document root looks like:

    drwxr-xr-x   6 admin   204 Mar 13 11:16 bootstrap
    -rw-r--r--   1 admin  2135 Aug 19 12:58 callBonjour.socketio.html
    -rw-r--r--   1 admin  2035 Aug 19 12:58 callBonjour.websocket.html
    -rw-r--r--   1 admin  2573 Aug 19 12:58 callBonjourEvent.socketio.html
    -rw-r--r--   1 admin  2220 Aug 19 12:58 callUPnP.socketio.html
    -rw-r--r--   1 admin  2120 Aug 19 12:58 callUPnP.websocket.html
    -rw-r--r--   1 admin  2658 Aug 19 12:58 callUPnPEvent.socketio.html
    -rw-r--r--   1 admin  2639 Aug 19 12:58 discovery.socketio.html
    -rw-r--r--   1 admin  2539 Aug 19 12:58 discovery.websocket.html
    -rw-r--r--   1 admin  2043 Aug 19 12:58 exposeBonjour.socketio.html
    -rw-r--r--   1 admin  1943 Aug 19 12:58 exposeBonjour.websocket.html
    -rw-r--r--   1 admin  2048 Aug 19 12:58 exposeBonjourEvent.socketio.html
    -rw-r--r--   1 admin  2034 Aug 19 12:58 exposeUPnP.socketio.html
    -rw-r--r--   1 admin  1934 Aug 19 12:58 exposeUPnP.websocket.html
    -rw-r--r--   1 admin  2039 Aug 19 12:58 exposeUPnPEvent.socketio.html
    drwxr-xr-x  11 admin   374 Aug 19 12:58 js
    drwxr-xr-x   4 admin   136 Aug 19 12:58 res

## More information

Please see these blog posts for more information:

	http://jcdufourd.wp.mines-telecom.fr/2013/05/15/network-service-discovery-api/
    http://jcdufourd.wp.mines-telecom.fr/2013/06/05/new-version-of-nsd-api-implementation/

This NSD agent uses open source libraries:

- Cling [http://4thline.org/projects/cling/], a UPnP implementation by Christian Bauer at Teleal (LGPL).

- JMDNS [http://jmdns.sourceforge.net/], a Bonjour implementation by Arthur van Hoff at Strangeberry (Apache v2 License).

- Java_WebSocket [https://github.com/TooTallNate/Java-WebSocket], a WebSocket server and client implementation by TooTallNate (MIT License).

- JSON [http://www.json.org/java/index.html], a JSON codec implementation from json.org (JSON License).

- netty [http://netty.io/], an asynchronous event-driven network application framework for rapid development of
maintainable high performance protocol servers and clients (Apache License v2).

- netty-socketio [https://github.com/mrniko/netty-socketio], a socket.io server designed on top of netty (Apache License v2).

- when.js [https://github.com/cujojs/when], a Promise/A+ implementation (MIT)

- require.js [http://requirejs.org/], a package system for JS (MIT)

The examples use Bootstrap (Apache v2 license) and jQuery (MIT License).