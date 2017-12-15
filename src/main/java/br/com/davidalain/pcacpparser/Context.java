package br.com.davidalain.pcacpparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.davidalain.pcapparser.mqtt.MQTTFragment;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPublishMessage;
import io.pkts.packet.TCPPacket;

public class Context {

	public static final int QOS_QUANTITY = 3;

	private final Map<MQTTPacket,TCPPacket>[] mqttToTcpBrokerSyncMap;
	private final Map<MQTTPacket,MQTTPacket>[] mqttTXvsRXMap;
	
	private List<Long>[] times;
	private Map<Flow, Map<Long/*second*/, Long/*bytes*/> > mapFlowThroughput;

	private MQTTPacket lastMqttReceived;
	private List<MQTTPacket>[] lastMqttReceivedQoS;

	private int packerNumber;
	private long startTimeUs; //used to calculate throughput in each second 
	private long endTimeUs;	//used to calculate throughput in each second

	private String broker1IP;
	private String broker2IP;
	private String client1IP;
	private String client2IP;
	
	private Map<Flow,MQTTFragment> mapMqttFragments; 
	
	public Context(String broker1ip, String broker2ip, String client1ip, String client2ip) {
		this.broker1IP = broker1ip;
		this.broker2IP = broker2ip;
		this.client1IP = client1ip;
		this.client2IP = client2ip;

		this.lastMqttReceivedQoS = new List[QOS_QUANTITY];
		this.mqttToTcpBrokerSyncMap = new HashMap[QOS_QUANTITY];
		this.mqttTXvsRXMap = new HashMap[QOS_QUANTITY];
		
		this.times = new ArrayList[QOS_QUANTITY];
		
		for(int i = 0 ; i < QOS_QUANTITY ; i++) {
			this.mqttToTcpBrokerSyncMap[i] = new HashMap<>();
			this.mqttTXvsRXMap[i] = new HashMap<>();
			
			this.lastMqttReceivedQoS[i] = new ArrayList<>();
			this.times[i] = new ArrayList<>();
		}

		this.mapFlowThroughput = new HashMap<>();
		this.mapMqttFragments = new HashMap<>();
		
//		this.lastMqttReceived = null;

		this.packerNumber = 0;
		this.startTimeUs = 0;
		this.endTimeUs = 0;
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

	public String getClient1IP() {
		return client1IP;
	}

	public void setClient1IP(String client1ip) {
		client1IP = client1ip;
	}

	public String getClient2IP() {
		return client2IP;
	}

	public void setClient2IP(String client2ip) {
		client2IP = client2ip;
	}

	public Map<Flow, Map<Long, Long>> getMapFlowThroughput() {
		return mapFlowThroughput;
	}

	public void setMapFlowThroughput(Map<Flow, Map<Long, Long>> mapFlowThroughput) {
		this.mapFlowThroughput = mapFlowThroughput;
	}

	public Map<MQTTPacket, TCPPacket> getMqttToTcpBrokerSyncMap(int qosIndex) {
		return mqttToTcpBrokerSyncMap[qosIndex];
	}
	
	public Map<MQTTPacket, MQTTPacket> getMqttTXvsRXMap(int qosIndex) {
		return mqttTXvsRXMap[qosIndex];
	}
	
	public MQTTPacket getLastMqttReceived() {
		return lastMqttReceived;
	}

	public void setLastMqttReceived(MQTTPacket lastMqttReceived) {
		this.lastMqttReceived = lastMqttReceived;
	}

	public List<MQTTPacket> getLastMqttReceived(int qos) {
		return this.lastMqttReceivedQoS[qos];
	}
	
//	public void setLastMqttReceivedQoS(List<MQTTPacket> lastMqttReceived, int qos) {
//		this.lastMqttReceivedQoS[qos] = lastMqttReceived;
//	}

	public int getPackerNumber() {
		return packerNumber;
	}

	public void incrementPackerNumber() {
		this.packerNumber++;
	}

	public final List<Long> getTimes(int qosIndex){
		return times[qosIndex];
	}

	//	public long getStartTime() {
	//		return startTime;
	//	}
	//
	//	public void setStartTime(long startTime) {
	//		this.startTime = startTime;
	//	}
	//	
	//	public long getEndTime() {
	//		return endTime;
	//	}
	//
	//	public void setEndTime(long endTime) {
	//		this.endTime = endTime;
	//	}

	public long getStartTimeUs() {
		return startTimeUs;
	}

	public void setStartTimeUs(long startTimeUs) {
		this.startTimeUs = startTimeUs;
	}

	public long getEndTimeUs() {
		return endTimeUs;
	}

	public void setEndTimeUs(long endTimeUs) {
		this.endTimeUs = endTimeUs;
	}

	public Map<Flow, MQTTFragment> getMapMqttFragments() {
		return mapMqttFragments;
	}

	public void setMapMqttFragments(Map<Flow, MQTTFragment> mqttFragments) {
		this.mapMqttFragments = mqttFragments;
	}

	public void addBytes(Flow flow, long arrivalTimeUs, long bytestoAdd) {

		//Guarda o menor tempo
		if(this.startTimeUs == 0)
			this.startTimeUs = arrivalTimeUs;

		//Guarda o marior tempo
		if(arrivalTimeUs > this.endTimeUs)
			this.endTimeUs = arrivalTimeUs;

		long currentSecond = (arrivalTimeUs - startTimeUs) / (1000L * 1000L);

		if(mapFlowThroughput.get(flow) == null) {
			mapFlowThroughput.put(flow, new HashMap<>());
		}

		long value = (mapFlowThroughput.get(flow).get(currentSecond) == null) ?
				0 : mapFlowThroughput.get(flow).get(currentSecond);
		value += bytestoAdd;

		mapFlowThroughput.get(flow).put(currentSecond, value);
	}

}
