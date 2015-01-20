##nppush

Nppush is a small tool that constantly updates a wordpress post with content loaded from some source destination (e.g. an etherpad).


###prerequisites

- java installed (jre)
- java truststore with certificates included if wordpress is behind https
- a configuration file containing urls, properties and credentials (start with ```application.dev.conf```)

### java keystore

- import your cert into java keystore ```keytool -importcert -file certificate.cer -keystore keystore.jks -alias "Alias"```


### build app

- ! jdk required
- update the config file (please see ```application.conf``` as a starter) according to your needs
- start sbt with these two parameters
 - ```-Djavax.net.ssl.trustStore=<your path the the keystore>```
 - ```-Dconfig.file=application.conf```
- from inside sbt run ```run``` to run the app
- from inside sbt run ```assembly``` to create a single jar containing all libs

### run the app
- if you have created a the application via ```sbt assembly``` or if you have the final jar file run
- ```java -Djavax.net.ssl.trustStore=<path to your truststore> -Dconfig.file=<path to your config> -jar nppush-assembly-0.0.1.jar```
