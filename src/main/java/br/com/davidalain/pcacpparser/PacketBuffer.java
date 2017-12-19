package br.com.davidalain.pcacpparser;

import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.protocol.Protocol;

public class PacketBuffer {

	private final Packet packet;
	private final Buffer payloadBuffer;

	public PacketBuffer(Packet packet, Buffer payloadBuffer) {
		super();
		this.packet = packet;
		this.payloadBuffer = payloadBuffer;
	}

	public Packet getPacket() {
		return packet;
	}

	public Buffer getPayloadBuffer() {
		return payloadBuffer;
	}

	public Protocol getProtocol() {
		Protocol p = null;
		if(this.packet != null)
			p = this.packet.getProtocol();
		return p;
	}

}
