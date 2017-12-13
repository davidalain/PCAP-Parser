package br.com.davidalain.pcacpparser.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import io.pkts.packet.TCPPacket;

public class DataPrinter {

	public static void printQoSTimeAnalysisRTT(Context ctx, final PrintStream log, final PrintStream resultTime) throws FileNotFoundException {

		log.println("########################################################################");
		for(int qos = 0 ; qos < Context.QOS_QUANTITY ; qos++) {
			log.println("******************************* QoS = "+qos+" ****************************");
			for(Entry<MQTTPacket, MQTTPacket> pair : ctx.getMqttPublishToMqttResponseMap(qos).entrySet()) {
				log.println("MQTT arrivalTime:" + pair.getKey().getArrivalTime());
				log.println(HexPrinter.toStringHexDump(pair.getKey().getData()));
				log.println("MQTT arrivalTime:" + pair.getKey().getArrivalTime());
				log.println(HexPrinter.toStringHexDump(pair.getValue().getData()));

				long diffTime = (pair.getValue().getArrivalTime() - pair.getKey().getArrivalTime());
				log.println("difftime(us) = " + diffTime);

				ctx.getTimes(qos).add(diffTime);
			}
			log.println("**************************************************************************");

			resultTime.println("============================= QoS = "+qos+" ===============================");
			double avg = 0;
			double median = 0;
			double max = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.max(ctx.getTimes(qos));
			double min = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.min(ctx.getTimes(qos));
			for(long l : ctx.getTimes(qos)) {
				avg += (double)l;
			}
			avg /= (double)ctx.getTimes(qos).size();
			resultTime.println(ctx.getTimes(qos));
			resultTime.println("max(us)="+max+", max(ms)="+(max/1000.0)+", max(s)="+(max/(1000.0*1000.0)));
			resultTime.println("min(us)="+min+", min(ms)="+(min/1000.0)+", min(s)="+(min/(1000.0*1000.0)));
			resultTime.println("avg(us)="+avg+", avg(ms)="+(avg/1000.0)+", avg(s)="+(avg/(1000.0*1000.0)));

			Collections.sort(ctx.getTimes(qos));
			median = ctx.getTimes(qos).size() == 0 ? Double.NaN : ctx.getTimes(qos).get(ctx.getTimes(qos).size()/2);
			resultTime.println("median(us)="+median+", median(ms)="+(median/1000.0)+", median(s)="+(median/(1000.0*1000.0)));
			resultTime.println("========================================================================");
		}
		log.println("########################################################################");

	}

	public static void printSeparatedFlows(final Context ctx, final PrintStream resultFlow) throws FileNotFoundException {

		/**
		 * Tempo inicial e final para ser mostrado em segundos (diferen�a do tempo real de chegada/saida dos pacotes em rela��o ao primeiro pacote capturado)
		 */
		final long firstSecond = 0;
		final long lastSecond = (ctx.getEndTimeUs() - ctx.getStartTimeUs()) / (1000L * 1000L);

		/**
		 * Impress�o de cada fluxo em um arquivo separado
		 */
		for(Entry<Flow, Map<Long, Long>> pairFlowThroughtput : ctx.getMapFlowThroughput().entrySet()) {
			resultFlow.println("========================================================================");

			final Flow flow = pairFlowThroughtput.getKey();
			final PrintStream printerCurrentFlow = new PrintStream(new File(Constants.OUTPUT_FLOW_PATH+Constants.PREFIX+"_resultFlow_"+flow.toStringForFileName()+".csv"));

			resultFlow.println("Flow: " + flow);
			resultFlow.println();
			resultFlow.println("second, bytes"); //CSV like

			printerCurrentFlow.println("second, bytes"); //CSV like

			Map<Long, Long> mapThroughput = pairFlowThroughtput.getValue();
			for(long second = firstSecond ; second <= lastSecond; second++) {

				Long bytes = mapThroughput.get(second);
				if(bytes == null) bytes = 0L;

				resultFlow.println(second + ", " + bytes); //imprime no arquivo com todos os fluxos
				printerCurrentFlow.println(second + ", " + bytes); //imprime no arquivo separado por fluxo
			}

			resultFlow.println("========================================================================");
		}

	}

	public static void printAllFlows(final Context ctx, final PrintStream printerAllFlow) throws FileNotFoundException {

		/**
		 * Tempo inicial e final para ser mostrado em segundos (diferen�a do tempo real de chegada/saida dos pacotes em rela��o ao primeiro pacote capturado)
		 */
		final long firstSecond = 0;
		final long lastSecond = (ctx.getEndTimeUs() - ctx.getStartTimeUs()) / (1000L * 1000L);

		/**
		 * Impress�o de todos os fluxos em um �nico arquivo
		 */

		final int columnCount = ctx.getMapFlowThroughput().entrySet().size();
		final int lineCount = (int) lastSecond + 1; //+1 pq come�a a contar do zero
		final String[] headerFlowsStr = new String[columnCount];
		final long[][] matrixAllFlow = new long[lineCount][columnCount];
		int column = 0;

		/**
		 * L� o mapa de todos os fluxos
		 */
		for(Entry<Flow, Map<Long/*second*/, Long/*bytes*/>> pairFlowThroughtput : ctx.getMapFlowThroughput().entrySet()) {

			/**
			 * Para cada fluxo, contabiliza a quantidade de bytes transferida por segundo 
			 */
			final Flow flow = pairFlowThroughtput.getKey();
			final Map<Long, Long> mapThroughput = pairFlowThroughtput.getValue();

			headerFlowsStr[column] = flow.toString(); //Preenche o cabe�alho

			for(long second = firstSecond, line = 0 ; second <= lastSecond; second++, line++) {

				Long bytes = mapThroughput.get(second);
				if(bytes == null) bytes = 0L;

				matrixAllFlow[(int)line][column] = bytes.longValue();
			}

			column++;
		}

		/**
		 * Imprime os dados no formato CSV, com o seguinte padr�o de cabe�alho:
		 * 	"second, fluxo1, fluxo2, ..." 
		 */
		printerAllFlow.print("second");
		for(String header : headerFlowsStr) {
			printerAllFlow.print(", " + header);
		}
		printerAllFlow.println();

		int secondValue = 0;
		for(long[] lineValues : matrixAllFlow) {
			printerAllFlow.print(secondValue++);
			for(long value : lineValues) {
				printerAllFlow.print(", " + value);	
			}
			printerAllFlow.println();
		}
	}

	public static void printQoSTimeAnalysisClusterSync(Context ctx, final PrintStream log, final PrintStream resultTime) throws FileNotFoundException {

		log.println("########################################################################");
		for(int qos = 0 ; qos < Context.QOS_QUANTITY ; qos++) {
			log.println("******************************* QoS = "+qos+" ****************************");
			for(Entry<MQTTPacket, TCPPacket> pair : ctx.getMqttToTcpBrokerSyncMap(qos).entrySet()) {
				log.println("MQTT");
				log.println(HexPrinter.toStringHexDump(pair.getKey().getData()));
				log.println("TCP");
				log.println(HexPrinter.toStringHexDump(pair.getValue().getPayload().getArray()));
				long diffTime = (pair.getValue().getArrivalTime() - pair.getKey().getArrivalTime());
				log.println("difftime(us) = " + diffTime);

				ctx.getTimes(qos).add(diffTime);
			}
			log.println("**************************************************************************");

			resultTime.println("============================= QoS = "+qos+" ===============================");
			double avg = 0;
			double median = 0;
			double max = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.max(ctx.getTimes(qos));
			double min = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.min(ctx.getTimes(qos));
			for(long l : ctx.getTimes(qos)) {
				avg += (double)l;
			}
			avg /= (double)ctx.getTimes(qos).size();
			resultTime.println(ctx.getTimes(qos));
			resultTime.println("max(us)="+max+", max(ms)="+(max/1000.0)+", max(s)="+(max/(1000.0*1000.0)));
			resultTime.println("min(us)="+min+", min(ms)="+(min/1000.0)+", min(s)="+(min/(1000.0*1000.0)));
			resultTime.println("avg(us)="+avg+", avg(ms)="+(avg/1000.0)+", avg(s)="+(avg/(1000.0*1000.0)));

			Collections.sort(ctx.getTimes(qos));
			median = ctx.getTimes(qos).size() == 0 ? Double.NaN : ctx.getTimes(qos).get(ctx.getTimes(qos).size()/2);
			resultTime.println("median(us)="+median+", median(ms)="+(median/1000.0)+", median(s)="+(median/(1000.0*1000.0)));
			resultTime.println("========================================================================");
		}
		log.println("########################################################################");

	}


}