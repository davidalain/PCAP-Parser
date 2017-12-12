package br.com.davidalain.pcapparser.mqtt;

public class MQTTSubscribeRequest extends MQTTPacket {

	public MQTTSubscribeRequest(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}

	public int getMessageIdentifier() {
		return ((data[2] << 8)| data[3]);
	}
	
	public int getTopicLength() {
		return ((data[4] << 8)| data[5]);
	}
	
	public final byte[] getTopicArray() {
		final byte[] array = new byte[getTopicLength()];
		int index = 6;
		System.arraycopy(data, index, array, 0, array.length);
		return array;
	}
	
	public final String getTopic() {
		return new String(getTopicArray());
	}
	
	public int getGrantedQoS() {
		int index = 6 + getTopicLength();
		return data[index];
	}
	
}
