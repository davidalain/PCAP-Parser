package br.com.davidalain.pcacpparser.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import br.com.davidalain.pcacpparser.ArraysUtil;
import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcacpparser.PacketUtil;
import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTFragment;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPublishMessage;
import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

public class MainClusterSync {

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
		new File(Parameters.OUTPUT_FLOW_PATH).mkdirs();

		final Pcap pcap = Pcap.openStream(Parameters.FILEPATH);
		final Context ctx = new Context(Parameters.BROKER1_IP, Parameters.BROKER2_IP, Parameters.CLIENT1_IP, Parameters.CLIENT2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
		final PrintStream log = new PrintStream(new File(Parameters.OUTPUT_PATH+Parameters.PREFIX+"_log.txt"));
		final PrintStream resultTime = new PrintStream(new File(Parameters.OUTPUT_PATH+Parameters.PREFIX+"_resultTime.txt"));
		final PrintStream resultFlow = new PrintStream(new File(Parameters.OUTPUT_PATH+Parameters.PREFIX+"_resultFlow.txt"));
		final PrintStream printerAllFlow = new PrintStream(new File(Parameters.OUTPUT_FLOW_PATH+Parameters.PREFIX+"_allFlows.csv"));

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

						log.println("Ethernet frame sizes: \t"+
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

									log.println("msgType:"+mqttPacket.getMessageTypeEnum());
									log.println("DUPFlag:"+mqttPacket.getDupFlag());
									log.println("QoS:"+mqttPacket.getQoS());
									log.println("RetainFlag:"+mqttPacket.getRetainFlag());
									log.println("msgLen:"+mqttPacket.getMessageLength());

									/**
									 * Client1 enviando Publish Message para o broker1
									 */
									if(
											tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP) && 
											tcpPacket.getDestinationIP().equals(Parameters.BROKER1_IP) ) {
										ctx.setLastMqttReceived(mqttPacket);	
									}else {
										log.println("{IP de origem = ("+tcpPacket.getSourceIP()+") != ("+Parameters.CLIENT1_IP+")} ou {IP de destino = ("+tcpPacket.getDestinationIP()+") != ("+Parameters.BROKER1_IP+")}");
									}

								} else {
									/** TCP payload não é MQTT ou é um fragmento de um MQTT. **/

									log.println("==== TCP payload não é um MQTT completo: ===");

									/**
									 * Pacotes com menos de 15 bytes são os possíveis fragmentos
									 */
									if(tcpPayloadLen < 15) {

										log.println("==== MQTT fragment: ===");
										MQTTFragment mqttFragment = ctx.getMapMqttFragments().get(flow);

										/** Ainda não tinha recebido pedaços de uma mensagem MQTT neste fluxo **/
										if(mqttFragment == null) {

											/** Verifica se é um possível pedaço de MQTT **/
											if(MQTTPacket.hasPacketType(tcpPayloadArray)) {

												log.println("==== primeiro fragmento ("+flow+") ===");

												mqttFragment = new MQTTFragment(tcpPacket.getArrivalTime()); //Primeiro fragmento

												System.arraycopy(tcpPayloadArray, 0,
														mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), tcpPayloadLen);

												mqttFragment.addPartialMessageLen(tcpPayloadLen);

												log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

												ctx.getMapMqttFragments().put(flow, mqttFragment);

											}
											/** Não é um pedaço de MQTT. Processa como outro pacote qualquer de camada de aplicação **/
											else {
												//Como não é MQTT e é pequeno demais pra uma mensagem sync então nada faz!
												log.println("==== não tem formato de MQTT ou é pequeno demais ===");
											}
										}
										/** Está recebendo pedaços de uma mensagem MQTT neste fluxo, então armazena os bytes recebidos **/
										else {

											log.println("==== outro fragmento ("+flow+")===");

											System.arraycopy(tcpPayloadArray, 0,
													mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), tcpPayloadLen);

											mqttFragment.addPartialMessageLen(tcpPayloadLen);

											log.println("fragmento recebido:");
											log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

											log.println("fragmento parcial:");
											log.println(HexPrinter.toStringHexDump(mqttFragment.getPartialMessageBuffer(), 0, mqttFragment.getPartialMessageLen()));

											switch (mqttFragment.check()) {
											case -1: //mensagem inválida
												ctx.getMapMqttFragments().remove(flow);
												log.println("==== fragmentos formaram mensagem MQTT inválida ===");
												break;
											case 0: //mensagem incompleta (ainda tem bytes pra receber)
												//faz nada
												break;
											case 1: //mensagem completa (processar o pacote recebido)
												ctx.setLastMqttReceived(mqttFragment.buildMQTTPacket());
												ctx.getMapMqttFragments().remove(flow);
												log.println("==== mensagem MQTT remontada ===");
												break;
											}

											log.println(ctx.getMapMqttFragments().get(flow));

										}

									}
									
									MQTTPacket lastMqtt = ctx.getLastMqttReceived();

									log.println("lastMqtt="+lastMqtt);

									if(lastMqtt != null) {

										/**
										 * Já recebeu um pacote MQTT. Procura pelo pacote de sync.
										 */

										log.println("==== Não é MQTT: ===");
										log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

										//Os pacotes de sync tem os primeiros 4 bytes como sendo o tamanho da mensagem de sync, mas ainda não sei sobre os outros campos
										if(tcpPayloadLen >= 4) {
											int len = ByteBuffer.wrap(tcpPayloadArray, 0, 4).getInt();
											log.println("len="+len);
										}

										log.println(tcpPayloadLen);

										if(lastMqtt.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) {

											MQTTPublishMessage mqttPublish = (MQTTPublishMessage) lastMqtt;

											byte[] topic = mqttPublish.getTopicArray();
											byte[] message = mqttPublish.getMessageArray();

											/**
											 * Broker1 enviando mensagem de sync para Broker2
											 */
											if(true || (tcpPacket.getSourceIP().equals(Parameters.BROKER1_IP) && tcpPacket.getDestinationIP().equals(Parameters.BROKER2_IP))) {

												//FIXME:
												//Note: this is a not safe check, because topic name and message can be equals (same content) or shorter than necessary to guarantee correct working
												//The correct way to check is analyze if topic and message are into TCP payload at the their specific correct positions
												if(
														ArraysUtil.contains(tcpPayloadArray, topic) &&
														ArraysUtil.contains(tcpPayloadArray, message)) 
												{

													//This is the TCP Segment that broker uses to synchronize last MQTT Publish Message to another broker in the cluster

													int qos = lastMqtt.getQoS();
													log.println("qos="+qos);

													ctx.getMqttToTcpBrokerSyncMap(qos).put(lastMqtt, tcpPacket);
													ctx.setLastMqttReceived(null);

												} else {

													//												log.println("ArraysUtil.contains(tcpPayloadArray, topic)="+ArraysUtil.contains(tcpPayloadArray, topic));
													//												log.println(HexPrinter.toStringHexDump(topic));
													//												
													//												log.println("ArraysUtil.contains(tcpPayloadArray, message)="+ArraysUtil.contains(tcpPayloadArray, message));
													//												log.println(HexPrinter.toStringHexDump(message));

												}

											}

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
							log.println(packet.getProtocol());
						}

						log.println("============================================================");					
					}
				}
				return true;
			}
		});

		if(ctx.getMqttToTcpBrokerSyncMap(0).isEmpty() &&
				ctx.getMqttToTcpBrokerSyncMap(1).isEmpty() &&
				ctx.getMqttToTcpBrokerSyncMap(2).isEmpty())
		{
			System.err.println("Nenhum pacote MQTT foi endereçado de/para "+Parameters.CLIENT1_IP+".");
			System.err.println("É possível que o endereço IP do cliente esteja errado ou não há messagens MQTT no arquivo " + Parameters.FILEPATH);
			System.err.println("Ou está acontecendo fragmentação dos segmentos das mensagens MQTT.");

			System.err.println("Veja o arquivo '" + Parameters.OUTPUT_PATH+Parameters.PREFIX+"_log.txt'");
		}

		DataPrinter.printQoSTimeAnalysisClusterSync(ctx, log, resultTime);
		DataPrinter.printSeparatedFlows(ctx,resultFlow);
		DataPrinter.printAllFlows(ctx,printerAllFlow);

		System.out.println("Done!");

	}//fim do main



}
