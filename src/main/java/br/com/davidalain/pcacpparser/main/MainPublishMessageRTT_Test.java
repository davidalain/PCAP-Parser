package br.com.davidalain.pcacpparser.main;

import java.io.IOException;

import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.DataPrinter;
import br.com.davidalain.pcacpparser.PacketBuffer;
import br.com.davidalain.pcacpparser.PacketProcessingUtil;
import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.protocol.Protocol;

public class MainPublishMessageRTT_Test {

	/**
	 * TODO:
	 * 
	 * @see https://github.com/emqtt/emqttd/wiki/$SYS-Topics
	 */

	public static void main(String[] args) throws IOException {

		System.out.println("Running...");

		final Pcap pcap = Pcap.openStream(Parameters.PCAP_FILEPATH);
		final Context ctx = new Context(Parameters.BROKER1_IP, Parameters.BROKER2_IP, Parameters.CLIENT1_IP, Parameters.CLIENT2_IP);
		final DataPrinter printer = new DataPrinter();
		final PacketProcessingUtil packetUtil = new PacketProcessingUtil();

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				ctx.incrementPackerNumber();

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					printer.log.println("============================================================");
					printer.log.println("packetNumber: " + ctx.getPackerNumber());
					
					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);

					PacketBuffer networkPacketBuffer = packetUtil.processMACPacket(ethernetPacket, ctx, printer);

					if(networkPacketBuffer != null && networkPacketBuffer.getProtocol() == Protocol.IPv4) {
						
						PacketBuffer transportPacketBuffer = packetUtil.processIPPacket(networkPacketBuffer, ctx, printer);
						
						if(transportPacketBuffer != null && transportPacketBuffer.getProtocol() == Protocol.TCP) {
							
							PacketBuffer applicationPacketBuffer = packetUtil.processTCPPacket(transportPacketBuffer, ctx, printer);

							packetUtil.processApplicationPacket(applicationPacketBuffer, ctx, printer);
							
						}//tcp
						
					}//ipv4
					
				}//ethernet
				
				return true;
			}
		});

		if(ctx.getMqttTXvsRXMap(0).isEmpty() &&
				ctx.getMqttTXvsRXMap(1).isEmpty() &&
				ctx.getMqttTXvsRXMap(2).isEmpty())
		{
			System.err.println("Nenhum pacote MQTT foi endereçado de/para "+Parameters.CLIENT1_IP+".");
			System.err.println("É possível que o endereço IP do cliente esteja errado ou não há messagens MQTT no arquivo " + Parameters.PCAP_FILEPATH);
			System.err.println("Ou está acontecendo fragmentação dos segmentos das mensagens MQTT.");

			System.err.println("Veja o arquivo '" + Parameters.LOG_FILEPATH);
		}

		printer.printQoSTimeAnalysisRTT(ctx);
		printer.printSeparatedFlows(ctx);
		printer.printAllFlows(ctx);

		System.out.println("Done!");

	}//fim do main



}