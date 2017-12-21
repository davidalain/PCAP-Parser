package br.com.davidalain.pcacpparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.davidalain.pcapparser.mqtt.MQTTFragment;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPublishMessage;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.impl.ApplicationPacket;

public class Context {

	public static final int QOS_QUANTITY = 3;

	private final Map<MQTTPacket,ApplicationPacket>[] mqttToTcpBrokerSyncMap;
	private final Map<MQTTPacket,MQTTPacket>[] mqttTXvsRXMap;
	
	private List<Long>[] times;
	private Map<Flow, Map<Long/*second*/, Long/*bytes*/> > mapFlowThroughput;

	private List<MQTTPublishMessage>[] lastPublishSentToBrokerQoS;

	private int packerNumber;
	private long startTimeUs; //used to calculate throughput in each second 
	private long endTimeUs;	//used to calculate throughput in each second

	private String brokerIP;
	private String clientIP;
	
	private Map<Flow,MQTTFragment> mapMqttFragments; 
	
	public Context(final String brokerIP, String clientIP) {
		this.brokerIP = brokerIP;
		this.clientIP = clientIP;

		this.lastPublishSentToBrokerQoS = new List[QOS_QUANTITY];
		this.mqttToTcpBrokerSyncMap = new HashMap[QOS_QUANTITY];
		this.mqttTXvsRXMap = new HashMap[QOS_QUANTITY];
		this.times = new ArrayList[QOS_QUANTITY];
		
		for(int i = 0 ; i < QOS_QUANTITY ; i++) {
			this.mqttToTcpBrokerSyncMap[i] = new HashMap<>();
			this.mqttTXvsRXMap[i] = new HashMap<>();
			
			this.lastPublishSentToBrokerQoS[i] = new ArrayList<>();
			this.times[i] = new ArrayList<>();
		}

		this.mapFlowThroughput = new HashMap<>();
		this.mapMqttFragments = new HashMap<>();
		
		this.packerNumber = 0;
		this.startTimeUs = 0;
		this.endTimeUs = 0;
	}
	
	public String getBrokerIP() {
		return brokerIP;
	}

	public void setBrokerIP(String broker1ip) {
		brokerIP = broker1ip;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setClientIP(String client1ip) {
		clientIP = client1ip;
	}

	public Map<Flow, Map<Long, Long>> getMapFlowThroughput() {
		return mapFlowThroughput;
	}

//	public void setMapFlowThroughput(Map<Flow, Map<Long, Long>> mapFlowThroughput) {
//		this.mapFlowThroughput = mapFlowThroughput;
//	}

	public Map<MQTTPacket, ApplicationPacket> getMqttToTcpBrokerSyncMap(int qosIndex) {
		return mqttToTcpBrokerSyncMap[qosIndex];
	}
	
	public Map<MQTTPacket, MQTTPacket> getMqttTXvsRXMap(int qosIndex) {
		return mqttTXvsRXMap[qosIndex];
	}

	public List<MQTTPublishMessage> getLastPublishSentToBroker(int qosIndex) {
		return this.lastPublishSentToBrokerQoS[qosIndex];
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

	public void addBytesToFlow(PacketBuffer transportPacketBuffer) throws IOException {

		TCPPacket tcpPacket = (TCPPacket) transportPacketBuffer.getPacket();
//		Buffer tcpBuffer = transportPacketBuffer.getPayloadBuffer();
		
		int length = tcpPacket.getParentPacket().getParentPacket().getPayload().getArray().length;
		
		addBytesToFlow(new Flow(tcpPacket), tcpPacket.getArrivalTime(), length); 
	}
	
	public void addBytesToFlow(Flow flow, long arrivalTimeUs, long bytestoAdd) {

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
