package br.com.davidalain.pcacpparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import io.pkts.packet.TCPPacket;

public class Context {
	
	public static final int QOS_QUANTITY = 3;
	
	private Map<MQTTPacket,TCPPacket>[] mqttToTcpBrokerSyncMap;
	private List<Long>[] times;
	private Map<Flow, Map<Long/*second*/, Long/*bytes*/> > mapFlowThroughput;
	
	private MQTTPacket lastMqttReceived;
	
	private String client1IP;
	private String broker1IP;
	private String broker2IP;
	
	private int packerNumber;
	private long startTimeUs; //used to calculate throughput in each second 
	private long endTimeUs;	//used to calculate throughput in each second

	public Context(String client1IP, String broker1IP, String broker2IP) {
		this.mqttToTcpBrokerSyncMap = new HashMap[QOS_QUANTITY];
		this.times = new ArrayList[QOS_QUANTITY];
		for(int i = 0 ; i < QOS_QUANTITY ; i++) {
			this.mqttToTcpBrokerSyncMap[i] = new HashMap<>();
			this.times[i] = new ArrayList<>();
		}
		
		this.mapFlowThroughput = new HashMap<>();
		
		this.lastMqttReceived = null;
		
		this.client1IP = client1IP;
		this.broker1IP = broker1IP;
		this.broker2IP = broker2IP;
		
		this.packerNumber = 0;
		this.startTimeUs = 0;
		this.endTimeUs = 0;
	}
	
	public Map<Flow, Map<Long, Long>> getMapFlowThroughput() {
		return mapFlowThroughput;
	}

	public void setMapFlowThroughput(Map<Flow, Map<Long, Long>> mapFlowThroughput) {
		this.mapFlowThroughput = mapFlowThroughput;
	}

	public Map<MQTTPacket, TCPPacket> getMqttToTcpBrokerSyncMap(int index) {
		return mqttToTcpBrokerSyncMap[index];
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
