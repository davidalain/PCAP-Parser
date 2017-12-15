package br.com.davidalain.pcapparser.mqtt;

import java.util.Comparator;

public class MQTTComparator implements Comparator<MQTTPacket>{

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
			return compare((MQTTPublishMessage)o1, (MQTTPublishMessage)o2);
		}
		
		/** Publish vs PubAck **/
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBLISH.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBACK.value) 
		{
			return compare((MQTTPublishMessage)o1, (MQTTPubAck)o2);
		}
		
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBACK.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) 
		{
			return compare((MQTTPubAck)o1, (MQTTPublishMessage)o2);
		}
		
		/** Publish vs PubComplete **/
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBLISH.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value) 
		{
			return compare((MQTTPublishMessage)o1, (MQTTPubComplete)o2);
		}
		
		if(
				o1.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value &&
				o2.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) 
		{
			return compare((MQTTPubComplete)o1, (MQTTPublishMessage)o2);
		}
		
		return compareOnlyHeader(o1, o2);
	}

	public int compare(MQTTPublishMessage o1, MQTTPublishMessage o2) {

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

	public int compare(MQTTPublishMessage o1, MQTTPubAck o2) {

		int value = 0;

		value = compareOnlyHeader( (MQTTPacket)o1, (MQTTPacket)o2 );
		if(value != 0) 
			return value;

		value = (o1.getMessageIdentifier() - o2.getMessageIdentifier());
		if(value != 0)
			return value;

		return 0;
	}
	
	public int compare(MQTTPubComplete o1, MQTTPublishMessage o2) {
		return -compare(o2, o1);
	}
	
	public int compare(MQTTPublishMessage o1, MQTTPubComplete o2) {

		int value = 0;

		value = compareOnlyHeader( (MQTTPacket)o1, (MQTTPacket)o2 );
		if(value != 0) 
			return value;

		value = (o1.getMessageIdentifier() - o2.getMessageIdentifier());
		if(value != 0)
			return value;

		return 0;
	}
	
	public int compare(MQTTPubAck o1, MQTTPublishMessage o2) {
		return -compare(o2, o1);
	}


}
