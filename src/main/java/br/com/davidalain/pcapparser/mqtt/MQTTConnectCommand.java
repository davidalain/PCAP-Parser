package br.com.davidalain.pcapparser.mqtt;

import br.com.davidalain.pcacpparser.Util;

public class MQTTConnectCommand extends MQTTPacket {

	public MQTTConnectCommand(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
	public int getProtocolNameLength() {
		return Util.toInt(data, 2, 2);
	}
	
	public final byte[] getProtocolNameArray() {
		byte[] array = new byte[getProtocolNameLength()];
		System.arraycopy(data, 4, array, 0, array.length);
		return array;
	}
	
	public final String getProtocolName() {
		return new String(getProtocolNameArray());
	}
	
	public int getVersion() {
		int index = 2 + 2 + getProtocolNameLength();
		return data[index];
	}
	
	public byte getConnectFlags() {
		int index = 2 + 2 + getProtocolNameLength() + 1;
		return data[index];
	}
	
	public int getKeepAlive() {
		int index = 2 + 2 + getProtocolNameLength() + 1 + 2;
		return Util.toInt(data, index, 2);
	}
	
	public int getClientIdLength() {
		int index = 2 + 2 + getProtocolNameLength() + 1 + 2 + 2;
		return Util.toInt(data, index, 2);
	}
	
	public byte[] getClientIdArray() {
		int index = 2 + 2 + getProtocolNameLength() + 1 + 2 + 2 + 2;
		byte[] array = new byte[getClientIdLength()];
		System.arraycopy(data, index, array, 0, array.length);
		return array;
	}
	
	public String getClientId() {
		return new String(getClientIdArray());
	}

}
