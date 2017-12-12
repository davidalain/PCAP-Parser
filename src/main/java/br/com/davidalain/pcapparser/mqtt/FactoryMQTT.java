package br.com.davidalain.pcapparser.mqtt;

public class FactoryMQTT {
	
	private int getMessageType(byte[] data) {
		return ((data[0] & 0xF0) >> 4);
	}

	public MQTTPacket getMQTTPacket(byte[] data, long arrivalTime) {
		
		if(!MQTTPacket.isMQTT(data))
			return null;
		
		switch (getMessageType(data)) {
		case MQTTPacket.MessageType.PUBLISH_MESSAGE:		return new MQTTPublishMessage(data, arrivalTime);
		case MQTTPacket.MessageType.CONNECT_ACK:			return new MQTTConnectAck(data, arrivalTime);
		case MQTTPacket.MessageType.CONNECT_COMMAND:		return new MQTTConnectCommand(data, arrivalTime);
		case MQTTPacket.MessageType.SUBSCRIBE_ACK:			return new MQTTSubscribeAck(data, arrivalTime);
		case MQTTPacket.MessageType.SUBSCRIBE_REQUEST:		return new MQTTSubscribeRequest(data, arrivalTime);
			
		default:
			break;
		}
		
		return null;
	}
	
}
