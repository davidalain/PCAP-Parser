package com.javahelps.pcapparser;

import java.io.IOException;

import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

public class PacketUtil {

	public static long getArrivalTime(Packet packet) {
		return packet.getArrivalTime();
	}

	public static String getSourceIP(Packet packet) throws IOException {

		if(!packet.hasProtocol(Protocol.IPv4))
			return null;

		IPPacket ip = (IPPacket) packet.getPacket(Protocol.IPv4);
		return ip.getSourceIP();
	}

	public static String getDestinationIP(Packet packet) throws IOException {

		if(!packet.hasProtocol(Protocol.IPv4))
			return null;

		IPPacket ip = (IPPacket) packet.getPacket(Protocol.IPv4);
		return ip.getDestinationIP();
	}

	public static int getSourcePort(Packet packet) throws IOException {

		if(packet.hasProtocol(Protocol.TCP)) {
			TCPPacket ip = (TCPPacket) packet.getPacket(Protocol.TCP);
			return ip.getSourcePort();
		}
		
		if(packet.hasProtocol(Protocol.UDP)) {
			UDPPacket ip = (UDPPacket) packet.getPacket(Protocol.UDP);
			return ip.getSourcePort();
		}
		
		return -1;
	}
	
	public static int getDestinationPort(Packet packet) throws IOException {

		if(packet.hasProtocol(Protocol.TCP)) {
			TCPPacket ip = (TCPPacket) packet.getPacket(Protocol.TCP);
			return ip.getDestinationPort();
		}
		
		if(packet.hasProtocol(Protocol.UDP)) {
			UDPPacket ip = (UDPPacket) packet.getPacket(Protocol.UDP);
			return ip.getDestinationPort();
		}
		
		return -1;
	}
	
}
