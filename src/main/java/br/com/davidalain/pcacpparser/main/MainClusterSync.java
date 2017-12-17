package br.com.davidalain.pcacpparser.main;

import java.io.IOException;
import java.nio.ByteBuffer;

import br.com.davidalain.pcacpparser.ArraysUtil;
import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.DataPrinter;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcacpparser.PacketProcessingUtil;
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

		final Pcap pcap = Pcap.openStream(Parameters.PCAP_FILEPATH);
		final Context ctx = new Context(Parameters.BROKER1_IP, Parameters.BROKER2_IP, Parameters.CLIENT1_IP, Parameters.CLIENT2_IP);
		final DataPrinter printer = new DataPrinter();

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				ctx.incrementPackerNumber();

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					printer.log.println("============================================================");
					printer.log.println("packetNumber: " + ctx.getPackerNumber());

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);
					Buffer ethernetPayload = ethernetPacket.getPayload();
					byte[] ethernetPayloadArray = ethernetPayload.getArray();
					int ethernetPayloadLen = ethernetPayloadArray.length;

					if (ethernetPacket.hasProtocol(Protocol.IPv4)) {

						Flow flow = new Flow(ethernetPacket);

						printer.log.println("time(us): " + new PacketProcessingUtil().getArrivalTime(packet));
						printer.log.println("flow: " + flow);
						ctx.addBytes(flow, new PacketProcessingUtil().getArrivalTime(packet), ethernetPacket.getTotalLength());

						printer.log.println("Ethernet frame sizes: \t"+
								"total=" + ethernetPacket.getTotalLength() + "\t"+
								"header=" + (ethernetPacket.getTotalLength() - ethernetPayloadLen) + "\t"+
								"payload=" + ethernetPayloadLen);

						IPPacket ipPacket = (IPPacket) ethernetPacket.getPacket(Protocol.IPv4);
						Buffer ipPayload = ipPacket.getPayload();
						byte[] ipPayloadArray = ipPayload.getArray();
						int ipPayloadLen = ipPayloadArray.length;

						printer.log.println("IP datagram size: \t\t"+
								"total=" + ethernetPayloadLen + "\t"+
								"header=" + (ethernetPayloadLen - ipPayloadLen) + "\t"+
								"payload=" + ipPayloadLen);

						if (ipPacket.hasProtocol(Protocol.TCP)) {

							TCPPacket tcpPacket = (TCPPacket) ipPacket.getPacket(Protocol.TCP);
							Buffer tcpPayload = tcpPacket.getPayload();

							if (tcpPayload != null) {

								byte[] tcpPayloadArray = tcpPayload.getArray();
								int tcpPayloadLen = tcpPayloadArray.length;

								printer.log.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - tcpPayloadLen) + "\t"+
										"payload=" + tcpPayloadLen);

								printer.log.println(tcpPayloadArray.length);
								printer.log.println(HexPrinter.toStringHexDump(tcpPayloadArray));
								printer.log.println(tcpPayload);

								if(MQTTPacket.isMQTT(tcpPayload)) {

									printer.log.println("==== MQTT: ===");
									printer.log.println("pktNum="+ ctx.getPackerNumber() + ", � um pacote MQTT");

									MQTTPacket mqttPacket = new FactoryMQTT().getMQTTPacket(tcpPayloadArray, tcpPacket.getArrivalTime());

									printer.log.println("msgType:"+mqttPacket.getMessageTypeEnum());
									printer.log.println("DUPFlag:"+mqttPacket.getDupFlag());
									printer.log.println("QoS:"+mqttPacket.getQoS());
									printer.log.println("RetainFlag:"+mqttPacket.getRetainFlag());
									printer.log.println("msgLen:"+mqttPacket.getMessageLength());

									/**
									 * Client1 enviando Publish Message para o broker1
									 */
									if(
											tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP) && 
											tcpPacket.getDestinationIP().equals(Parameters.BROKER1_IP) ) {
										ctx.setLastMqttReceived(mqttPacket);	
									}else {
										printer.log.println("{IP de origem = ("+tcpPacket.getSourceIP()+") != ("+Parameters.CLIENT1_IP+")} ou {IP de destino = ("+tcpPacket.getDestinationIP()+") != ("+Parameters.BROKER1_IP+")}");
									}

								}
								/**
								 * TCP payload n�o � MQTT ou � um fragmento de um MQTT.
								 */
								else {
									
									printer.log.println("==== TCP payload n�o � um MQTT completo: ===");

									/**
									 * Pacotes com menos de 15 bytes s�o os poss�veis fragmentos
									 */
									if(tcpPayloadLen < 15) {

										printer.log.println("==== MQTT fragment: ===");
										MQTTFragment mqttFragment = ctx.getMapMqttFragments().get(flow);

										/** Ainda n�o tinha recebido peda�os de uma mensagem MQTT neste fluxo **/
										if(mqttFragment == null) {

											/** Verifica se � um poss�vel peda�o de MQTT **/
											if(MQTTPacket.hasPacketType(tcpPayloadArray)) {

												printer.log.println("==== primeiro fragmento ("+flow+") ===");

												mqttFragment = new MQTTFragment(tcpPacket.getArrivalTime()); //Primeiro fragmento

												System.arraycopy(tcpPayloadArray, 0,
														mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), tcpPayloadLen);

												mqttFragment.addPartialMessageLen(tcpPayloadLen);

												printer.log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

												ctx.getMapMqttFragments().put(flow, mqttFragment);

											}
											/** N�o � um peda�o de MQTT. Processa como outro pacote qualquer de camada de aplica��o **/
											else {
												//Como n�o � MQTT e � pequeno demais pra uma mensagem sync ent�o nada faz!
												printer.log.println("==== n�o tem formato de MQTT ou � pequeno demais ===");
											}
										}
										/** Est� recebendo peda�os de uma mensagem MQTT neste fluxo, ent�o armazena os bytes recebidos **/
										else {

											printer.log.println("==== outro fragmento ("+flow+")===");

											System.arraycopy(tcpPayloadArray, 0,
													mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), tcpPayloadLen);

											mqttFragment.addPartialMessageLen(tcpPayloadLen);

											printer.log.println("fragmento recebido:");
											printer.log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

											printer.log.println("fragmento parcial:");
											printer.log.println(HexPrinter.toStringHexDump(mqttFragment.getPartialMessageBuffer(), 0, mqttFragment.getPartialMessageLen()));

											switch (mqttFragment.check()) {
											case -1: //mensagem inv�lida
												ctx.getMapMqttFragments().remove(flow);
												printer.log.println("==== fragmentos formaram mensagem MQTT inv�lida ===");
												break;
											case 0: //mensagem incompleta (ainda tem bytes pra receber)
												printer.log.println("==== mensagem incompleta, ainda falta receber fragmentos ===");
												break;
											case 1: //mensagem completa (processar o pacote recebido)
												ctx.setLastMqttReceived(mqttFragment.buildMQTTPacket());
												ctx.getMapMqttFragments().remove(flow);
												printer.log.println("==== mensagem MQTT remontada ===");
												break;
											}

											printer.log.println("Fragmento: " + ctx.getMapMqttFragments().get(flow));

										}

									}
									
									MQTTPacket lastMqtt = ctx.getLastMqttReceived();

									printer.log.println("lastMqtt="+lastMqtt);

									if(lastMqtt != null) {

										/**
										 * J� recebeu um pacote MQTT. Procura pelo pacote de sync.
										 */

										printer.log.println("==== N�o � MQTT: ===");
										printer.log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

										//Os pacotes de sync tem os primeiros 4 bytes como sendo o tamanho da mensagem de sync, mas ainda n�o sei sobre os outros campos
										if(tcpPayloadLen >= 4) {
											int len = ByteBuffer.wrap(tcpPayloadArray, 0, 4).getInt();
											printer.log.println("len="+len);
										}

										printer.log.println(tcpPayloadLen);

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
													printer.log.println("qos="+qos);

													ctx.getMqttToTcpBrokerSyncMap(qos).put(lastMqtt, tcpPacket);
													ctx.setLastMqttReceived(null);

												} else {

													//												printer.log.println("ArraysUtil.contains(tcpPayloadArray, topic)="+ArraysUtil.contains(tcpPayloadArray, topic));
													//												printer.log.println(HexPrinter.toStringHexDump(topic));
													//												
													//												printer.log.println("ArraysUtil.contains(tcpPayloadArray, message)="+ArraysUtil.contains(tcpPayloadArray, message));
													//												printer.log.println(HexPrinter.toStringHexDump(message));

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

								printer.log.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - udpPayloadLen) + "\t"+
										"payload=" + udpPayloadLen);

							}
						} else {
							printer.log.println(packet.getProtocol());
						}

						printer.log.println("============================================================");					
					}
				}
				return true;
			}
		});

		if(ctx.getMqttToTcpBrokerSyncMap(0).isEmpty() &&
				ctx.getMqttToTcpBrokerSyncMap(1).isEmpty() &&
				ctx.getMqttToTcpBrokerSyncMap(2).isEmpty())
		{
			System.err.println("Nenhum pacote MQTT foi endere�ado de/para "+Parameters.CLIENT1_IP+".");
			System.err.println("� poss�vel que o endere�o IP do cliente esteja errado ou n�o h� messagens MQTT no arquivo " + Parameters.PCAP_FILEPATH);
			System.err.println("Ou est� acontecendo fragmenta��o dos segmentos das mensagens MQTT.");

			System.err.println("Veja o arquivo '" + Parameters.LOG_FILEPATH);
		}

		printer.printQoSTimeAnalysisClusterSync(ctx);
		printer.printSeparatedFlows(ctx);
		printer.printAllFlows(ctx);

		System.out.println("Done!");

	}//fim do main



}
