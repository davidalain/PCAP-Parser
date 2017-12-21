package br.com.davidalain.pcacpparser.main;

import java.io.IOException;
import java.security.InvalidParameterException;

import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.DataPrinter;
import br.com.davidalain.pcacpparser.PacketBuffer;
import br.com.davidalain.pcacpparser.PacketProcessingUtil;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.protocol.Protocol;

public class MainClusterSync {

	public static void run(final String pcapFilePath, final String brokerIP) throws IOException {

		if(pcapFilePath == null)
			throw new InvalidParameterException("pcap file must be specified");
		if(brokerIP == null)
			throw new InvalidParameterException("broker IP must be specified");
		
		System.out.println("Running...");

		final Pcap pcap = Pcap.openStream(pcapFilePath);
		final Context ctx = new Context(brokerIP, null);
		final DataPrinter printer = new DataPrinter(pcapFilePath);
		final PacketProcessingUtil packetUtil = new PacketProcessingUtil();

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				ctx.incrementPackerNumber();

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					printer.log.println("============================================================");
					printer.log.println("packetNumber: " + ctx.getPackerNumber());
					printer.log.println("time(us): " + packet.getArrivalTime());

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);

					PacketBuffer networkPacketBuffer = packetUtil.processMACPacket(ethernetPacket, ctx, printer);

					if(networkPacketBuffer != null && networkPacketBuffer.getProtocol() == Protocol.IPv4) {

						PacketBuffer transportPacketBuffer = packetUtil.processIPPacket(networkPacketBuffer, ctx, printer);

						if(transportPacketBuffer != null && transportPacketBuffer.getProtocol() == Protocol.TCP) {

							PacketBuffer applicationPacketBuffer = packetUtil.processTCPPacket(transportPacketBuffer, ctx, printer);

							MQTTPacket ReceivedMqttPacket = packetUtil.processApplicationPacket(applicationPacketBuffer, ctx, printer);

							packetUtil.processMQTTandSyncPackets(applicationPacketBuffer, ReceivedMqttPacket, ctx, printer);

							ctx.addBytesToFlow(transportPacketBuffer);

						}//tcp

					}//ipv4

				}//ethernet

				return true;
			}

		});

		if(ctx.getMqttToTcpBrokerSyncMap(0).isEmpty() &&		//qos 0
				ctx.getMqttToTcpBrokerSyncMap(1).isEmpty() &&	//qos 1
				ctx.getMqttToTcpBrokerSyncMap(2).isEmpty())		//qos 2
		{
			System.err.println("No one MQTT Publish packet was sincronized from broker with IP "+brokerIP+" another broker");
			System.err.println("It is possible that IP broker is wrong or "+pcapFilePath+" does not contains MQTT Publish and syncronization packet of that Publish");
			
			System.err.println("See '" + PathUtil.logFilePathTXT(pcapFilePath)+"' file for detailed info.");
		}

		printer.printQoSTimeAnalysisClusterSync(ctx);
//		printer.printSeparatedFlows(ctx);
		printer.printAllFlows(ctx);

		System.out.println("Done!");
		System.out.println("See '" + PathUtil.logFilePathTXT(pcapFilePath)+"' file for detailed info.");

	}//fim do main


}
