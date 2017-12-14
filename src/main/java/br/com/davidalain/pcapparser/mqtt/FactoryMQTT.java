package br.com.davidalain.pcapparser.mqtt;

public class FactoryMQTT {
	
	public MQTTPacket getMQTTPacket(byte[] data, long arrivalTime) {
		
		if(!MQTTPacket.isMQTT(data)) 
			return null;
			
		switch (MQTTPacket.readMQTTPacketTypeEnum(data)) {
		case CONNECT: 		return new MQTTPublishMessage(data, arrivalTime);
		case CONNACK:		return new MQTTConnectAck(data, arrivalTime);
		case PUBLISH:		return new MQTTPublishMessage(data, arrivalTime);
		case PUBACK:		return new MQTTPacket(data, arrivalTime); //FIXME
		case PUBREC:		return new MQTTPacket(data, arrivalTime); //FIXME
		case PUBREL:		return new MQTTPacket(data, arrivalTime); //FIXME
		case PUBCOMP:		return new MQTTPacket(data, arrivalTime); //FIXME
		case SUBSCRIBE:		return new MQTTSubscribeRequest(data, arrivalTime);
		case SUBACK:		return new MQTTSubscribeAck(data, arrivalTime);
		case UNSUBSCRIBE:	return new MQTTPacket(data, arrivalTime); //FIXME
		case UNSUBACK:		return new MQTTPacket(data, arrivalTime); //FIXME
		case PINGREQ:		return new MQTTPingRequest(data, arrivalTime);
		case PINGRESP:		return new MQTTPingResponse(data, arrivalTime);
		case DISCONNECT:	return new MQTTPacket(data, arrivalTime); //FIXME
			
		default:
			break;
		}
		
		System.err.println("Erro estranho. Não é nenhum dos tipos acima. Algum tipo foi esquecido de ser checado.");
		return null;
	}
	
}
