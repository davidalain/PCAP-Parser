package br.com.davidalain.pcapparser.mqtt;

import java.util.Comparator;

public class MQTTComparator implements Comparator<MQTTPacket>{
	
	private int compareAllBytes(MQTTPacket o1, MQTTPacket o2) {
		
		System.out.println("compareAllBytes");
		
		int value = 0;
		
		value = o1.data.length - o2.data.length;
		if(value != 0)
			return value;
		
		for(int i = 0 ; i < o1.data.length ; i++) {
			value = o1.data[i] - o2.data[i];
			if(value != 0)
				return value;
		}
		
		return value;
	}

	private int compareOnlyHeader(MQTTPacket o1, MQTTPacket o2) {
		
		int value = 0;

		value = (o1.data[0] - o2.data[0]);
		if(value != 0)
			return value;

		value = (o1.data[1] - o2.data[1]);
		if(value != 0)
			return value;

		return 0;
	}
	
	@Override
	public int compare(MQTTPacket o1, MQTTPacket o2) {
		
		/** Publish vs Publish **/
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBLISH.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) 
		{
			return comparePublishPublish((MQTTPublishMessage)o1, (MQTTPublishMessage)o2);
		}
		
		/** Publish vs PubAck **/
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBLISH.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBACK.value) 
		{
			return comparePublishPubAck((MQTTPublishMessage)o1, (MQTTPubAck)o2);
		}
		
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBACK.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) 
		{
			return -comparePublishPubAck((MQTTPublishMessage)o2, (MQTTPubAck)o1);
		}
		
		/** Publish vs PubComplete **/
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBLISH.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value) 
		{
			return comparePublishPubComplete((MQTTPublishMessage)o1, (MQTTPubComplete)o2);
		}
		
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) 
		{
			return -comparePublishPubComplete((MQTTPublishMessage)o2, (MQTTPubComplete)o1);
		}
		
		return compareAllBytes(o1, o2);
	}

	public int comparePublishPublish(MQTTPublishMessage o1, MQTTPublishMessage o2) {

//		System.out.println("comparePublishPublish");
		
		int value = 0;

		value = compareOnlyHeader( (MQTTPacket)o1, (MQTTPacket)o2 );
		if(value != 0) {
			return value;
		}

		value = (o1.getTopic().compareTo(o2.getTopic()));
		if(value != 0)
			return value;

		value = (o1.getMessage().compareTo(o2.getMessage()));
		if(value != 0)
			return value;

		return 0;
	}

	public int comparePublishPubAck(MQTTPublishMessage o1, MQTTPubAck o2) {
		
//		System.out.println("comparePublishPubAck");

		int value = 0;

		value = o1.getQoS() - 1; //PubAck é resposta do Publish com QoS 1, se não for então é dado como diferente
		if(value != 0) 
			return value;

		value = (o1.getMessageIdentifier() - o2.getMessageIdentifier());
		if(value != 0)
			return value;

		return 0;
	}
	
	public int comparePublishPubComplete(MQTTPublishMessage o1, MQTTPubComplete o2) {

//		System.out.println("comparePublishPubComplete");
		
		int value = 0;

		value = o1.getQoS() - 2; //PubComplete é resposta do Publish com QoS 2, se não for então é dado como diferente
		if(value != 0) 
			return value;

		value = (o1.getMessageIdentifier() - o2.getMessageIdentifier());
		if(value != 0)
			return value;

		return 0;
	}
	
}
