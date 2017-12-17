package br.com.davidalain.pcacpparser.main;

import java.io.IOException;
import java.util.Map.Entry;

import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.DataPrinter;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcacpparser.PacketProcessingUtil;
import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPubAck;
import br.com.davidalain.pcapparser.mqtt.MQTTPubComplete;
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

public class MainPublishMessageRTT {

	/**
	 * TODO:
	 * 
	 * @see https://github.com/emqtt/emqttd/wiki/$SYS-Topics
	 */

	public static void main(String[] args) throws IOException {

		System.out.println("Running...");

		final Pcap pcap = Pcap.openStream(Parameters.PCAP_FILEPATH);
		final Context ctx = new Context(Parameters.BROKER1_IP, Parameters.BROKER2_IP, Parameters.CLIENT1_IP, Parameters.CLIENT2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
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

						printer.log.println("Ethernet frame size: \t"+
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
									printer.log.println("pktNum="+ ctx.getPackerNumber() + ", É um pacote MQTT");

									MQTTPacket mqttPacket = factory.getMQTTPacket(tcpPayloadArray, tcpPacket.getArrivalTime());

									printer.log.println("msgType:"+mqttPacket.getMessageType());
									printer.log.println("DUPFlag:"+mqttPacket.getDupFlag());
									printer.log.println("QoS:"+mqttPacket.getQoS());
									printer.log.println("RetainFlag:"+mqttPacket.getRetainFlag());
									printer.log.println("msgLen:"+mqttPacket.getMessageLength());

									int qos = mqttPacket.getQoS();
									/**
									 * Client1 enviando Publish Message para o Broker
									 */
									if(
											tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP) &&
											//tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP) &&
											mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value ) 
									{
										ctx.getLastMqttReceived(mqttPacket.getQoS()).add(mqttPacket);

										printer.log.println("CLIENTE ENVIANDO MQTT ####");
									}

									/**
									 * QoS 0 - Client1 recebendo o PUBLISH do Broker
									 */
									else if((tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
											(qos == 0) &&
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value))
									{

										printer.log.println("CLIENTE RECEBENDO MQTT ****");

										/**
										 * Duas mensagens PUBLISH são iguais quando tem o mesmo tipo, tamanho, tópico e mensagem.
										 * Não conta o tempo de chegada dos pacotes.
										 * 
										 * @note indexOf() usa equals().
										 * 
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											printer.log.println("== Cliente recebendo do broker a mensagem de Publish que foi enviada anteriormente ==");
											MQTTPublishMessage publish =  (MQTTPublishMessage) mqttPacket;
											printer.log.println("topico:"+publish.getTopic());
											printer.log.println("message:"+publish.getMessage());

											ctx.getMqttTXvsRXMap(qos).put(ctx.getLastMqttReceived(qos).remove(index), mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que não foi enviada pelo Cliente1. 
											 */
										}

									}
									/**
									 * QoS 1 - Parte 1 - Client1 recebendo o PUBLISH do Broker
									 * 
									 * Guardar o PUBLISH enviado pelo broker para o cliente que contém o mesmo 'Message ID' da mensagem
									 * de PUBACK que o cliente vai enviar para o broker.
									 * O tempo será contabilizado através dessa mensagem PUBACK.
									 */
									else if((tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
											(qos == 1) &&
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value))
									{

										printer.log.println("== Cliente recebeu PUBLISH ==");

										/**
										 * @note indexOf() usa equals().
										 * 
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											printer.log.println("== PUBLISH recebido é o mesmo enviado (topico e mensagem são iguais) ==");

											//Guarda a mensagem de publish recebido que contém o 'Message ID' do PUBACK que vai ser enviado para depois trocar no mapa essa PUBLISH pelo PUBACK.
											ctx.getMqttTXvsRXMap(qos).put(ctx.getLastMqttReceived(qos).remove(index)	, mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que não foi enviada pelo Cliente1. 
											 */
										}

									}
									/**
									 * QoS 1 - Parte 2 - Client1 enviando o PUBACK para o broker
									 */
									else if((tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
											(mqttPacket.getQoS() == 0) && //o PUBACK (enviado como confirmação de um PUBLISH com QoS 1) tem QoS 0
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBACK.value))
									{

										printer.log.println(" == PUBACK enviado ==");

										for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(1).entrySet()) {

											MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();
											MQTTPubAck pubAckRx = (MQTTPubAck) mqttPacket;

											//Substitui o MQTT PUblish recebido pelo Publish ACK enviado (que contém o tempo final).
											//O PUBLISH que é a chave do mapa contém o tempo inicial.
											if(publishRx.getMessageIdentifier() == pubAckRx.getMessageIdentifier()) {
												ctx.getMqttTXvsRXMap(1).put(pair.getKey(), pubAckRx);
												break;
											}

										}

									}
									/**
									 * QoS 2 - Client1 recebendo o PUBLISH do Broker
									 * 
									 * Guardar o PUBLISH enviado pelo broker para o cliente que contém o mesmo 'Message ID' da mensagem
									 * de PUBCOMP que o cliente vai enviar para o broker.
									 * O tempo será contabilizado através dessa mensagem PUBCOMP.
									 */
									else if((tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
											(qos == 2) &&
											(mqttPacket.getMessageTypeEnum().equals(MQTTPacket.PacketType.PUBLISH))) 
									{

										/**
										 * @note indexOf() usa equals().
										 * 
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											printer.log.println("CLIENTE RECEBENDO RESPOSTA DE ENVIO MQTT @@@@");

											ctx.getMqttTXvsRXMap(qos).put(ctx.getLastMqttReceived(qos).remove(index), mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que não foi enviada pelo Cliente1. 
											 */
										}

									}
									/**
									 * QoS 2 - Parte 2 - Client1 enviando o PUBCOMPLETE para o broker
									 */
									else if((tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
											(mqttPacket.getQoS() == 0) && //o PUBCOMP (enviado como confirmação de um PUBLISH com QoS 2) tem QoS 0
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value))
									{

										printer.log.println(" == PUBCOMP enviado ==");

										for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(2).entrySet()) {

											MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();
											MQTTPubComplete pubCompleteRx = (MQTTPubComplete) mqttPacket;

											//Substitui o MQTT PUblish recebido pelo Publish Complete enviado (que contém o tempo final).
											//O PUBLISH que é a chave do mapa contém o tempo inicial.
											if(publishRx.getMessageIdentifier() == pubCompleteRx.getMessageIdentifier()) {
												ctx.getMqttTXvsRXMap(2).put(pair.getKey(), pubCompleteRx);
												break;
											}

										}

									}

								}
								/**
								 * Não é um pacote MQTT
								 */
								else {
									
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
							printer.log.println("Another protocol:" + packet.getProtocol());
						}

						printer.log.println("============================================================");					
					}
				}
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