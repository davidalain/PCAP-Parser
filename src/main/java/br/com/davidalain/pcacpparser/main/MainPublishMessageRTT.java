package br.com.davidalain.pcacpparser.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidParameterException;

import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcacpparser.PacketUtil;
import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

public class MainPublishMessageRTT {

	/**
	 * TODO:
	 * 
	 * @see https://github.com/emqtt/emqttd/wiki/$SYS-Topics
	 */

	public static void main(String[] args) throws IOException {

		System.out.println("Running...");

		/**
		 * Cria o caminho (não dá erro caso já exista)
		 */
		new File(Constants.OUTPUT_FLOW_PATH).mkdirs();

		final Pcap pcap = Pcap.openStream(Constants.FILEPATH);
		final Context ctx = new Context(Constants.BROKER1_IP, Constants.BROKER2_IP, Constants.CLIENT1_IP, Constants.CLIENT2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
		final PrintStream log = new PrintStream(new File(Constants.OUTPUT_PATH+Constants.PREFIX+"_log.txt"));
		final PrintStream resultTime = new PrintStream(new File(Constants.OUTPUT_PATH+Constants.PREFIX+"_resultTime.txt"));
		final PrintStream resultFlow = new PrintStream(new File(Constants.OUTPUT_PATH+Constants.PREFIX+"_resultFlow.txt"));
		final PrintStream printerAllFlow = new PrintStream(new File(Constants.OUTPUT_FLOW_PATH+Constants.PREFIX+"_allFlows.csv"));

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				ctx.incrementPackerNumber();

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					log.println("============================================================");
					log.println("packetNumber: " + ctx.getPackerNumber());

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);
					Buffer ethernetPayload = ethernetPacket.getPayload();
					byte[] ethernetPayloadArray = ethernetPayload.getArray();
					int ethernetPayloadLen = ethernetPayloadArray.length;

					if (ethernetPacket.hasProtocol(Protocol.IPv4)) {

						Flow flow = new Flow(ethernetPacket);

						log.println("time(us): " + PacketUtil.getArrivalTime(packet));
						log.println("flow: " + flow);
						ctx.addBytes(flow, PacketUtil.getArrivalTime(packet), ethernetPacket.getTotalLength());

						log.println("Ethernet frame size: \t"+
								"total=" + ethernetPacket.getTotalLength() + "\t"+
								"header=" + (ethernetPacket.getTotalLength() - ethernetPayloadLen) + "\t"+
								"payload=" + ethernetPayloadLen);

						IPPacket ipPacket = (IPPacket) ethernetPacket.getPacket(Protocol.IPv4);
						Buffer ipPayload = ipPacket.getPayload();
						byte[] ipPayloadArray = ipPayload.getArray();
						int ipPayloadLen = ipPayloadArray.length;

						log.println("IP datagram size: \t\t"+
								"total=" + ethernetPayloadLen + "\t"+
								"header=" + (ethernetPayloadLen - ipPayloadLen) + "\t"+
								"payload=" + ipPayloadLen);

						if (ipPacket.hasProtocol(Protocol.TCP)) {

							TCPPacket tcpPacket = (TCPPacket) ipPacket.getPacket(Protocol.TCP);
							Buffer tcpPayload = tcpPacket.getPayload();

							if (tcpPayload != null) {

								byte[] tcpPayloadArray = tcpPayload.getArray();
								int tcpPayloadLen = tcpPayloadArray.length;

								log.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - tcpPayloadLen) + "\t"+
										"payload=" + tcpPayloadLen);

								log.println(tcpPayloadArray.length);
								log.println(HexPrinter.toStringHexDump(tcpPayloadArray));
								log.println(tcpPayload);

								if(MQTTPacket.isMQTT(tcpPayload)) {
									log.println("==== MQTT: ===");
									log.println("pktNum="+ ctx.getPackerNumber() + ", É um pacote MQTT");

									MQTTPacket mqttPacket = factory.getMQTTPacket(tcpPayloadArray, tcpPacket.getArrivalTime());

									log.println("msgType:"+mqttPacket.getMessageType());
									log.println("DUPFlag:"+mqttPacket.getDupFlag());
									log.println("QoS:"+mqttPacket.getQoS());
									log.println("RetainFlag:"+mqttPacket.getRetainFlag());
									log.println("msgLen:"+mqttPacket.getMessageLength());

									int qos = mqttPacket.getQoS();
									/**
									 * Client1 enviando Publish Message para o Broker
									 */
									if(tcpPacket.getSourceIP().equals(Constants.CLIENT1_IP)) {
										ctx.getLastMqttReceived(qos).add(mqttPacket);

										log.println("CLIENTE ENVIANDO MQTT ####");
									}
									/**
									 * Client1 recebendo o Publish Message do Broker
									 */
									else if(tcpPacket.getDestinationIP().equals(Constants.CLIENT1_IP)) {

										log.println("CLIENTE RECEBENDO MQTT ****");
										log.println(ctx.getLastMqttReceived(0));
										log.println(ctx.getLastMqttReceived(1));
										log.println(ctx.getLastMqttReceived(2));

										/**
										 * Dois MQTTPacket são iguais quando o conteúdo do pacote é igual (atributo 'byte[] data').
										 * Não conta o tempo de chegada dos pacotes!!
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											log.println("CLIENTE RECEBENDO RESPOSTA DE ENVIO MQTT @@@@");
											
											ctx.getMqttPublishToMqttResponseMap(qos).put(ctx.getLastMqttReceived(qos).remove(index), mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que não foi enviada pelo Cliente1. 
											 */
										}

									}

								} 

							}

						} else if (packet.hasProtocol(Protocol.UDP)) {

							UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
							Buffer udpPayload = udpPacket.getPayload();
							if (udpPayload != null) {

								byte[] udpPayloadArray = udpPayload.getArray();
								int udpPayloadLen = udpPayloadArray.length;

								log.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - udpPayloadLen) + "\t"+
										"payload=" + udpPayloadLen);

							}
						} else {
							log.println("Another protocol:" + packet.getProtocol());
						}

						log.println("============================================================");					
					}
				}
				return true;
			}
		});

		if(ctx.getMqttPublishToMqttResponseMap(0).isEmpty() &&
				ctx.getMqttPublishToMqttResponseMap(1).isEmpty() &&
				ctx.getMqttPublishToMqttResponseMap(2).isEmpty())
		{
			System.err.println("Nenhum pacote foi endereçado de/para "+Constants.CLIENT1_IP+".");
			System.err.println("É possível que o endereço IP do cliente esteja errado ou não há messagens MQTT no arquivo " + Constants.FILEPATH);
		}

		DataPrinter.printQoSTimeAnalysisRTT(ctx, log, resultTime);
		DataPrinter.printSeparatedFlows(ctx,resultFlow);
		DataPrinter.printAllFlows(ctx,printerAllFlow);

		System.out.println("Done!");

	}//fim do main



}