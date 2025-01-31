[description]
Enable the Jetty WebSocket API support for deployed web applications.

[tags]
websocket

[environment]
ee10

[depend]
ee10-annotations
websocket-jetty

[lib]
lib/ee10-websocket/jetty-ee10-websocket-jetty-server-${jetty.version}.jar
lib/ee10-websocket/jetty-ee10-websocket-servlet-${jetty.version}.jar

[jpms]
# The implementation needs to access method handles in
# classes that are in the web application classloader.
add-reads: org.eclipse.jetty.ee10.websocket.jetty.common=ALL-UNNAMED
