# EMQ PCAP Parser

MQTT, Message Queue Telemetry Transport, is a lightweight publish/subscribe messaging protocol,  useful for use with low power sensors, but is applicable to many scenarios. [EMQ](http://emqtt.io/ "EMQ") implements a MQTT broker with many features. A main feature of EMQ is the possibility to create a MQTT infrastructure using a MQTT broker cluster, which is a distributed system that represents one logical MQTT broker. These broker nodes runs on different machines connected over a network, behaving as a single MQTT Broker to MQTT clients. This clusteized approach improves scalability, resilience, fault tolorance and so on.

This project aims at evaluate the overhead brought by the additional synchronization messages used to synchronize brokers in a cluster.

### Features

-  Analyze synchronization time between brokers on a cluster
-  Analyze RTT on end to end messages

### How to use

    $ java -jar mqttAnalyzer.jar -sync <file.pcap> -b <broker IP>

In order to use you must record the TCP traffic in every machine running a broker using tcpdump. This will allow you to calculate the synchronization overhead.


    $ java -jar mqttAnalyzer.jar -rtt <file.pcap> -c <client IP>	

In order to calculate the whole end to end time, you need to run every application on the same machine and record the tcp traffic using tcpdump from the machine that hosts the MQTT clients.

### Examples

Inside the pcap_files folder you will find some sample .pcap files and in the plots folder you will find some results.

