##nppush

Nppush is a small tool that constantly updates a wordpress post with content loaded from some source destination (e.g. an etherpad).


###prerequisites

- java installed (jre)
- java truststore with certificates included if wordpress is behind https
- a configuration file containing urls, properties and credentials (start with ```application.dev.conf```)

start the app with ```java -Djavax.net.ssl.trustStore=<path to your truststore> -Dconfig.file=<path to your config> -jar nppush-assembly-0.0.1.jar```

### create a java truststore

