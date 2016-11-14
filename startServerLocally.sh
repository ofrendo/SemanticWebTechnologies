#java -Xmx1024m -Djavax.net.ssl.keyStore=keystore/truststore.ts -Djavax.net.ssl.keyStorePassword=Welcome2  -jar target/dependency/webapp-runner.jar target/*.war --enable-ssl

java -Xmx1024m -jar target/dependency/jetty-runner.jar --config jettyConfig.xml target/*.war