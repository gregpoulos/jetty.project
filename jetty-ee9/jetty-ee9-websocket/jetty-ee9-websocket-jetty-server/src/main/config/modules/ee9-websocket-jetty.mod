[description]
Enable the Jetty WebSocket API support for deployed web applications.

[tags]
websocket

[environment]
ee9

[depend]
ee9-annotations

[lib]
lib/jetty-websocket-core-common-${jetty.version}.jar
lib/jetty-websocket-core-server-${jetty.version}.jar
lib/ee9-websocket/jetty-ee9-websocket-servlet-${jetty.version}.jar
lib/ee9-websocket/jetty-ee9-websocket-jetty-api-${jetty.version}.jar
lib/ee9-websocket/jetty-ee9-websocket-jetty-common-${jetty.version}.jar
lib/ee9-websocket/jetty-ee9-websocket-jetty-server-${jetty.version}.jar

[jpms]
# The implementation needs to access method handles in
# classes that are in the web application classloader.
add-reads: org.eclipse.jetty.websocket.jetty.common=ALL-UNNAMED
