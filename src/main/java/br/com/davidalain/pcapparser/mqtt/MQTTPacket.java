package br.com.davidalain.pcapparser.mqtt;

import java.security.InvalidParameterException;
import java.util.Arrays;

import br.com.davidalain.pcacpparser.HexPrinter;
import io.pkts.buffer.Buffer;

public /*abstract*/ class MQTTPacket{

	/**
	 * @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Table_2.1_-
	 * 
	 * @author DavidAlain
	 */
	public enum PacketType{
		CONNECT(1),
		CONNACK(2),
		PUBLISH(3),
		PUBACK(4),
		PUBREC(5),
		PUBREL(6),
		PUBCOMP(7),
		SUBSCRIBE(8),
		SUBACK(9),
		UNSUBSCRIBE(10),
		UNSUBACK(11),
		PINGREQ(12),
		PINGRESP(13),
		DISCONNECT(14);

		//Valores 0 e 15 são reservados e proibidos de serem utilizados

		public final int value; 

		private PacketType(int value) {
			this.value = value;
		}
	}

	protected final byte[] data;
	protected final long arrivalTime;

	public MQTTPacket(byte[] data, long arrivalTime) {

		if(!isMQTT(data)) {
			System.err.println("Não é um pacote MQTT válido");
			System.err.println(HexPrinter.toStringHexDump(data));

			throw new InvalidParameterException("Não é um pacote MQTT válido!!");
		}

		this.data = data;
		this.arrivalTime = arrivalTime;
	}

	public final byte[] getData() {
		return data;
	}

	public long getArrivalTime() {
		return arrivalTime;
	}

	public int getMessageType() {
		return ((data[0] & 0xF0) >> 4);
	}
	
	public PacketType getMessageTypeEnum() {
		return readMQTTPacketTypeEnum(data);
	}

	public int getDupFlag() {
		return ((data[0] & 0x08) == 0) ? 1 : 0;
	}

	public int getQoS() {
		return ((data[0] & 0x06) >> 1);
	}

	public int getRetainFlag() {
		return (data[0] & 0x01);
	}

	public int getMessageLength() {
		return data[1];
	}

	@Override
	public boolean equals(Object obj) {

		if(!(obj instanceof MQTTPacket)) {
			return false;
		}

		MQTTPacket mqtt = (MQTTPacket) obj;

		if(this.data[0] != mqtt.data[0])
			return false;

		if(this.data[1] != mqtt.data[1])
			return false;

		return true;

		/**
		 * Não usar o código abaixo para comparação, pois:
		 *  um Publish Message enviado por um cliente terá o campo 'Message Identifier' modificado quando
		 *  o broker envia o mesmo Publish Message para o cliente que está escrito no tópico.
		 *  Apenas o campo 'message identifier' é alterado. Os outros campos permanecem sem alteração. 
		 *  
		 *   Isto faz falhar a condição utilizada para analisar o RTT
		 */
		//		return Arrays.equals(this.data, ((MQTTPacket) obj).data);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//		result = prime * result + (int) (arrivalTime ^ (arrivalTime >>> 32));
		result = prime * result + Arrays.hashCode(data);
		return result;
	}

	public static boolean isMQTT(Buffer buffer) {

		if(buffer == null)
			return false;

		return isMQTT(buffer.getArray());
	}

	public static int readMQTTPacketType(byte[] data) {

		if(data == null || data.length == 0)
			return -1;

		return (data[0] & 0xF0) >> 4;
	}

	public static PacketType readMQTTPacketTypeEnum(byte[] data) {

		if(!hasPacketType(data))
			return null;
		
		int type = readMQTTPacketType(data);
		
		return PacketType.values()[type-1];
	}

	public static boolean hasPacketType(byte[] data) {

		int type = readMQTTPacketType(data);
		if(type < PacketType.CONNECT.value || type > PacketType.DISCONNECT.value)
			return false;
		else
			return true;
	}

	public static boolean isMQTT(byte[] data) {

		if(data == null)
			return false;

		if(data.length < 2)
			return false;

		if(!hasPacketType(data))
			return false;

		if(data[1] != (data.length - 2))
			return false;

		return true;
	}

}
