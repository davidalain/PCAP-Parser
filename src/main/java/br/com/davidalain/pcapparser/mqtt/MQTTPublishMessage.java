package br.com.davidalain.pcapparser.mqtt;

public class MQTTPublishMessage extends MQTTPacket{

	public MQTTPublishMessage(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}

	public int getTopicLength() {
		return ((data[2] << 8) | data[3]);
	}

	public final byte[] getTopicArray() {
		final byte[] array = new byte[getTopicLength()];
		System.arraycopy(data, 4, array, 0, array.length);
		return array;
	}

	public final String getTopic() {
		return new String(getTopicArray());
	}

	public final int getMessageIdentifier() {
		int index = 4 + getTopicLength();
		return ((data[index] << 8) | data[index+1]);
	}

	public final byte[] getMessageArray() {
		int index = 4 + getTopicLength() + 2;
		final byte[] array = new byte[data.length-index];
		System.arraycopy(data, index, array, 0, array.length);
		return array;
	}

	public final String getMessage() {
		return new String(getMessageArray());
	}

//	@Override
//	public boolean equals(Object obj) {
//
//		//Condição que checa a menor mensagem MQTT (apenas dois bytes)
//		if(!super.equals(obj))
//			return false;
//
//		if(!(obj instanceof MQTTPublishMessage))
//			return false;
//		
//		MQTTPublishMessage mqtt = (MQTTPublishMessage) obj;
//
//		if(!this.getTopic().equals(mqtt.getTopic()))
//			return false;
//		
//		if(!this.getMessage().equals(mqtt.getMessage()))
//			return false;
//
//		return true;
//	}

}
