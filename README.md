nsd
===

Network Service Discovery API implementation

I have implemented the Network Service Discovery API [http://www.w3.org/TR/discovery-api/] 
on top of UPnP and Bonjour as a Java proxy for any modern browser, coupled with a JS library. 
The JS library handles the connection with the Java proxy through WebSocket and JSON messages.
This has been tested on PCs and Macs, with recent versions of Chrome, Firefox, Safari and Opera. 
It also works on Windows 7/IE10. Linux should not be a problem, but was not tested.

The project structure is compatible with Maven, and a pom.xml is provided.

Run the agent in a terminal with the command line:

	java -jar nsd-websocket-jar-with-dependencies.jar
or
	java -jar nsd-socketio-jar-with-dependencies.jar

depending on which communication protocol you want to use between the browser and the proxy.

Running the agent in the terminal is necessary if you want to see the error log. 
Run the agent before you load any of the examples in a browser.

Place the content of the examples directory in the document root of a web server. 

Please see these blog posts for more information:

	http://jcdufourd.wp.mines-telecom.fr/2013/05/15/network-service-discovery-api/
    http://jcdufourd.wp.mines-telecom.fr/2013/06/05/new-version-of-nsd-api-implementation/
