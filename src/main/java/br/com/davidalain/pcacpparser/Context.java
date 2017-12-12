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
	
	private int packerNumber;
	private long startTimeUs; //used to calculate throughput in each second 
	private long endTimeUs;	//used to calculate throughput in each second

	public Context() {
		this.mqttToTcpBrokerSyncMap = new HashMap[QOS_QUANTITY];
		this.times = new ArrayList[QOS_QUANTITY];
		for(int i = 0 ; i < QOS_QUANTITY ; i++) {
			this.mqttToTcpBrokerSyncMap[i] = new HashMap<>();
			this.times[i] = new ArrayList<>();
		}
		
		this.mapFlowThroughput = new HashMap<>();
		
		this.lastMqttReceived = null;
		
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
