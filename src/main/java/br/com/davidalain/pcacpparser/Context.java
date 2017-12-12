package br.com.davidalain.pcacpparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import io.pkts.packet.TCPPacket;

public class Context {
	
	private Map<MQTTPacket,TCPPacket> mqttToTcpBrokerSyncMap;
	private MQTTPacket lastMqttReceived;
	private List<Long> times;
	
	private String client1IP;
	private String broker1IP;
	private String broker2IP;
	
	private int packerNumber;
	
	public Context(String client1IP, String broker1IP, String broker2IP) {
		this.mqttToTcpBrokerSyncMap = new HashMap<>();
		this.lastMqttReceived = null;
		
		this.client1IP = client1IP;
		this.broker1IP = broker1IP;
		this.broker2IP = broker2IP;
		
		this.packerNumber = 0;
		this.times = new ArrayList<>();
	}

	public Map<MQTTPacket, TCPPacket> getMqttToTcpBrokerSyncMap() {
		return mqttToTcpBrokerSyncMap;
	}

	public void setMqttToTcpBrokerSyncMap(Map<MQTTPacket, TCPPacket> mqttToTcpBrokerSyncMap) {
		this.mqttToTcpBrokerSyncMap = mqttToTcpBrokerSyncMap;
	}

	public MQTTPacket getLastMqttReceived() {
		return lastMqttReceived;
	}

	public void setLastMqttReceived(MQTTPacket lastMqttReceived) {
		this.lastMqttReceived = lastMqttReceived;
	}

	public String getClient1IP() {
		return client1IP;
	}

	public void setClient1IP(String client1ip) {
		client1IP = client1ip;
	}

	public String getBroker1IP() {
		return broker1IP;
	}

	public void setBroker1IP(String broker1ip) {
		broker1IP = broker1ip;
	}

	public String getBroker2IP() {
		return broker2IP;
	}

	public void setBroker2IP(String broker2ip) {
		broker2IP = broker2ip;
	}

	public int getPackerNumber() {
		return packerNumber;
	}

	public void incrementPackerNumber() {
		this.packerNumber++;
	}
	
	public final List<Long> getTimes(){
		return times;
	}
	
	
	
	

}
