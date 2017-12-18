package br.com.davidalain.pcacpparser;

import java.io.IOException;
import java.util.Map.Entry;

import br.com.davidalain.pcacpparser.main.Parameters;
import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTFragment;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPubAck;
import br.com.davidalain.pcapparser.mqtt.MQTTPubComplete;
import br.com.davidalain.pcapparser.mqtt.MQTTPublishMessage;
import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.packet.impl.ApplicationPacket;
import io.pkts.protocol.Protocol;

public class PacketProcessingUtil {

	public long getArrivalTime(final Packet packet) {
		return packet.getArrivalTime();
	}

	public String getSourceIP(final Packet packet) throws IOException {

		if(!packet.hasProtocol(Protocol.IPv4))
			return null;

		IPPacket ip = (IPPacket) packet.getPacket(Protocol.IPv4);
		return ip.getSourceIP();
	}

	public String getDestinationIP(final Packet packet) throws IOException {

		if(!packet.hasProtocol(Protocol.IPv4))
			return null;

		IPPacket ip = (IPPacket) packet.getPacket(Protocol.IPv4);
		return ip.getDestinationIP();
	}

	public int getSourcePort(final Packet packet) throws IOException {

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

	public int getDestinationPort(final Packet packet) throws IOException {

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

	public PacketBuffer processMACPacket(final MACPacket ethernetPacket, final Context ctx, final DataPrinter printer) throws IOException {

		final Buffer ethernetPayload = ethernetPacket.getPayload();
		final byte[] ethernetPayloadArray = ethernetPayload.getArray();
		final int ethernetPayloadLen = ethernetPayloadArray.length;
		PacketBuffer packetBuffer = null;

		printer.log.println("Ethernet frame size: \t"+
				"total=" + ethernetPacket.getTotalLength() + "\t"+
				"header=" + (ethernetPacket.getTotalLength() - ethernetPayloadLen) + "\t"+
				"payload=" + ethernetPayloadLen);


		if(ethernetPacket.getNextPacket() != null) {
			packetBuffer = new PacketBuffer(ethernetPacket.getNextPacket(), ethernetPacket.getPayload());
		}

		return packetBuffer;
	}

	public PacketBuffer processIPPacket(final PacketBuffer ipPacketBuffer, final Context ctx, final DataPrinter printer) throws IOException{

		if((ipPacketBuffer.asPacket() == null) || 
				(!ipPacketBuffer.asPacket().hasProtocol(Protocol.TCP) && !ipPacketBuffer.asPacket().hasProtocol(Protocol.UDP)))
			return null;

		final IPPacket ipPacket = (IPPacket) ipPacketBuffer.asPacket();
		final int ipPacketLen = ipPacketBuffer.asPayloadBuffer().getArray().length;
		final Buffer ipPayload = ipPacket.getPayload();
		final byte[] ipPayloadArray = ipPayload.getArray();
		final int ipPayloadLen = ipPayloadArray.length;
		PacketBuffer packetBuffer = null;

		printer.log.println("IP datagram size: \t\t"+
				"total=" + ipPacketLen + "\t"+
				"header=" + (ipPacketLen - ipPayloadLen) + "\t"+
				"payload=" + ipPayloadLen);

		if(ipPacket.getNextPacket() != null) {
			packetBuffer = new PacketBuffer(ipPacket.getNextPacket(), ipPacket.getPayload());
		}

		return packetBuffer;
	}

	public PacketBuffer processTCPPacket(final PacketBuffer tcpPacketBuffer, final Context ctx, final DataPrinter printer) throws IOException {

		final TCPPacket tcpPacket = (TCPPacket) tcpPacketBuffer.asPacket();
		final int tcpPacketLen = tcpPacketBuffer.asPayloadBuffer().getArray().length;
		final Buffer tcpPayload = tcpPacket.getPayload();
		PacketBuffer packetBuffer = null;

		final byte[] applicationPacketArray = tcpPayload != null ? tcpPayload.getArray() : null;
		final int applicationPacketLen = applicationPacketArray != null ? applicationPacketArray.length : 0;

		printer.log.println("TCP segment size: \t\t"+
				"total=" + tcpPacketLen + "\t"+
				"header=" + (tcpPacketLen - applicationPacketLen) + "\t"+
				"payload=" + applicationPacketLen);

		printer.log.println("Flow: " + new Flow(tcpPacket).toString());
		printer.log.println();

		printer.log.println("Application packet length:");
		printer.log.println(applicationPacketLen);
		printer.log.println("Application packet hexdump:");
		printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));
		printer.log.println("TCP payload as String:");
		printer.log.println(tcpPayload);

		if(tcpPacket.getNextPacket() != null) {
			packetBuffer = new PacketBuffer(tcpPacket.getNextPacket(), tcpPacket.getPayload());
		}

		return packetBuffer;
	}

	/**
	 * @param applicationPacketBuffer
	 * @param ctx
	 * @param printer
	 * @return	A MQTTPacket or null
	 * 			If return is a MQTT packet is because either conditions:
	 * 				The applicationPacketBuffer contains a full MQTTPacket as TCP payload, or
	 * 				The applicationPacketBuffer contanis the last fragment of a MQTTPacket that is assembled and returned.
	 * 				
	 * 			If return is null is because either conditions:
	 * 				The applicationPacketBuffer does not contain a MQTT packet, or
	 * 				The applicationPacketBuffer contains only a fragment of a MQTT packet.
	 * 
	 * @throws IOException
	 */
	public final MQTTPacket processApplicationPacket(final PacketBuffer applicationPacketBuffer, final Context ctx, final DataPrinter printer) throws IOException {

		if(applicationPacketBuffer == null)
			return null;

		final ApplicationPacket applicationPacket = (ApplicationPacket) applicationPacketBuffer.asPacket();
		final byte[] applicationPacketArray = applicationPacketBuffer.asPayloadBuffer().getArray();
		final int applicationPacketLen = applicationPacketArray.length;
		final Flow flow = new Flow(applicationPacket);

		MQTTPacket mqttPacket = null;

		/**
		 * TCP payload n�o � MQTT ou � um fragmento de um MQTT.
		 */
		if(!MQTTPacket.isMQTT(applicationPacketArray)) {

			printer.log.println("==== TCP payload (application packet) n�o � um MQTT completo: ===");

			/**
			 * Pacotes com menos de 15 bytes s�o os poss�veis fragmentos
			 */
			if(applicationPacketLen < 15) {

				printer.log.println("==== MQTT fragment: ===");
				MQTTFragment mqttFragment = ctx.getMapMqttFragments().get(flow);

				/** Ainda n�o tinha recebido peda�os de uma mensagem MQTT neste fluxo **/
				if(mqttFragment == null) {

					/** Verifica se � um poss�vel peda�o de MQTT **/
					if(MQTTPacket.hasPacketType(applicationPacketArray)) {

						printer.log.println("==== primeiro fragmento ("+flow+") ===");

						mqttFragment = new MQTTFragment(applicationPacket.getArrivalTime()); //Primeiro fragmento

						System.arraycopy(applicationPacketArray, 0,
								mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), applicationPacketLen);

						mqttFragment.addPartialMessageLen(applicationPacketLen);

						printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));

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

					System.arraycopy(applicationPacketArray, 0,
							mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), applicationPacketLen);

					mqttFragment.addPartialMessageLen(applicationPacketLen);

					printer.log.println("fragmento recebido:");
					printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));

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
						mqttPacket = mqttFragment.buildMQTTPacket();
						ctx.getMapMqttFragments().remove(flow);

						printer.log.println("==== mensagem MQTT remontada ===");
						break;
					}

				}

			}
			/** (applicationPacketLen >= 15) **/
			else {

				printer.log.println("==== N�o � um fragmento MQTT (applicationPacketLen >= 15) ===");
			}

		}
		/**
		 * � um pacote MQTT
		 */
		else {

			mqttPacket = new FactoryMQTT().getMQTTPacket(applicationPacketArray, applicationPacket.getArrivalTime());
		}

		return mqttPacket;
	}

	public void processRTTPackets(final ApplicationPacket applicationPacket, final MQTTPacket mqttPacket, final Context ctx, final DataPrinter printer) throws IOException {

		/**
		 * Recebeu um MQTT ou remontou um MQTT
		 */
		if(MQTTPacket.isMQTT(mqttPacket.getData())) {

			printer.log.println("==== MQTT: ===");
			printer.log.println("pktNum="+ ctx.getPackerNumber() + ", � um pacote MQTT");

			printer.log.println("msgType:"+mqttPacket.getMessageTypeEnum());
			printer.log.println("DUPFlag:"+mqttPacket.getDupFlag());
			printer.log.println("QoS:"+mqttPacket.getQoS());
			printer.log.println("RetainFlag:"+mqttPacket.getRetainFlag());
			printer.log.println("msgLen:"+mqttPacket.getMessageLength());

			int qos = mqttPacket.getQoS();

			/**
			 * Client1 enviando PUBLISH Message para o Broker
			 */
			if(
					applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP) &&
					//tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP) &&
					mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value ) 
			{
				ctx.getLastPublishSent(qos).add(mqttPacket);

				printer.log.println("==== Cliente1 enviando PUBLISH ===");

				printer.log.println("==== PUBLISH QoS "+qos+" - (out)"+
						", pktNum: "+ctx.getPackerNumber()+
						", time(us): "+mqttPacket.getArrivalTime()+
						", msgId: "+ ((MQTTPublishMessage)mqttPacket).getMessageIdentifier()+
						", listLen: "+ctx.getLastPublishSent(qos).size()+
						", mapLen: "+ctx.getMqttTXvsRXMap(qos).size()+
						"===");
			}
			/**
			 * QoS 0 - Client1 recebendo o PUBLISH do Broker
			 */
			else if((applicationPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
					(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value))
			{

				printer.log.println("==== Cliente1 recebendo PUBLISH ===");

				/**
				 * Duas mensagens PUBLISH s�o iguais quando tem o mesmo tipo, tamanho, t�pico e mensagem.
				 * N�o conta o tempo de chegada dos pacotes.
				 * 
				 * @note indexOf() usa equals().
				 * 
				 * @see equals() em MQTTPacket
				 */
				int index = ctx.getLastPublishSent(qos).lastIndexOf(mqttPacket);
				if(index >= 0) {

					printer.log.println("== Cliente recebendo do broker a mensagem de Publish que foi enviada anteriormente ==");

					MQTTPublishMessage publishRx =  (MQTTPublishMessage) mqttPacket;
					printer.log.println("topic:"+publishRx.getTopic());
					printer.log.println("message:"+publishRx.getMessage());

					ctx.getMqttTXvsRXMap(qos).put(ctx.getLastPublishSent(qos).remove(index), publishRx);

					printer.log.println("==== PUBLISH QoS "+qos+" - (in)"+
							", pktNum: "+ctx.getPackerNumber()+
							", time(us): "+mqttPacket.getArrivalTime()+
							", msgId: "+ publishRx.getMessageIdentifier()+
							", listLen: "+ctx.getLastPublishSent(qos).size()+
							", mapLen: "+ctx.getMqttTXvsRXMap(qos).size()+
							"===");

					printer.log.println("== QoS "+qos+" - PUBLISH recebido � o mesmo enviado (topico e mensagem s�o iguais) ==");

				} else {
					/**
					 * Erro:
					 * 		Cliente1 recebendo Publish de uma mensagem que n�o foi enviada pelo Cliente1.
					 * 		Isto n�o faz parte do experimento.
					 * 		O cliente n�o deveria estar escrito em outro t�pico, e sim apenas naquele em que ele mesmo publica.
					 */
				}

			}
			/**
			 * QoS 1 - Parte 2 - Client1 enviando o PUBACK para o broker.
			 * 
			 * Guarda o pacote de PUBACK e substitui no lugar do pacote de PUBLISH recebida anteriormente do broker.
			 * 
			 * PUBACK � um pacote do fluxo do QoS 1, mas sempre tem QoS 0 no campo de cabe�alho
			 */
			else if((applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
					(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBACK.value))
			{

				printer.log.println(" == QoS 1 - PUBACK enviado ==");

				for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(1 /*qos*/).entrySet()) {

					/**
					 * Checa apenas os pares cujo valor ainda � um PUBLISH, ou seja, ainda n�o foi substituido pelo PUBACK
					 */
					if(pair.getValue() instanceof MQTTPublishMessage) {

						MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();

						MQTTPubAck pubAckRx = (MQTTPubAck) mqttPacket;

						//Substitui o MQTT Publish recebido pelo Publish ACK enviado (que cont�m o tempo final).
						//O PUBLISH que � a chave do mapa cont�m o tempo inicial.
						if(publishRx.getMessageIdentifier() == pubAckRx.getMessageIdentifier()) {
							ctx.getMqttTXvsRXMap(1 /*qos*/).replace(pair.getKey(), pubAckRx);

							printer.log.println("==== PUBACK QoS 1 - (out)"+
									", pktNum: "+ctx.getPackerNumber()+
									", time(us): "+mqttPacket.getArrivalTime()+
									", msgId: "+ ((MQTTPubAck)mqttPacket).getMessageIdentifier()+
									", listLen: "+ctx.getLastPublishSent(1).size()+
									", mapLen: "+ctx.getMqttTXvsRXMap(1).size()+
									"===");

							break;
						}
					}

				}

			}
			/**
			 * QoS 2 - Parte 2 - Client1 enviando o PUBCOMPLETE para o broker
			 * 
			 * Guarda o pacote de PUBCOMPLETE e substitui no lugar do pacote de PUBLISH recebido anteriormente do broker.
			 * 
			 * PUBCOMPLETE � um pacote do fluxo do QoS 2, mas sempre tem QoS 0 no campo de cabe�alho.
			 */
			else if((applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
					(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value))
			{

				printer.log.println(" == PUBCOMP enviado ==");

				for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(2 /*qos*/).entrySet()) {

					/**
					 * Checa apenas os pares cujo valor ainda � um PUBLISH, ou seja, ainda n�o foi substituido pelo PUBCOMP
					 */
					if(pair.getValue() instanceof MQTTPublishMessage) {
					
						MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();

						MQTTPubComplete pubCompleteRx = (MQTTPubComplete) mqttPacket;

						//Substitui o MQTT PUblish recebido pelo Publish Complete enviado (que cont�m o tempo final).
						//O PUBLISH que � a chave do mapa cont�m o tempo inicial.
						if(publishRx.getMessageIdentifier() == pubCompleteRx.getMessageIdentifier()) {
							ctx.getMqttTXvsRXMap(2 /*qos*/).replace(pair.getKey(), pubCompleteRx);

							printer.log.println("==== PUBCOMP QoS 2 - (out)"+
									", pktNum: "+ctx.getPackerNumber()+
									", time(us): "+mqttPacket.getArrivalTime()+
									", msgId: "+ ((MQTTPubComplete)mqttPacket).getMessageIdentifier()+
									", listLen: "+ctx.getLastPublishSent(2).size()+
									", mapLen: "+ctx.getMqttTXvsRXMap(2).size()+
									"===");

							break;
						}
						
					}
					

				}

			}

		}//� MQTT

	}//fim do m�todo

}
