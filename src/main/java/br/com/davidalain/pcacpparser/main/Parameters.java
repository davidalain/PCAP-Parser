package br.com.davidalain.pcacpparser.main;

import br.com.davidalain.pcacpparser.Flow;

public class Parameters {

	public static final String BROKER1_IP = "172.17.0.3";
	public static final String BROKER2_IP = "172.17.0.2";
	public static final String CLIENT1_IP = "192.168.25.63"; //"192.168.43.1"; //"192.168.25.63"; //"172.17.0.1";
	public static final String CLIENT2_IP = "172.17.0.1";

	public static final String PCAP_FILENAME = "trace_sync_dumpBroker2_broker2_172.17.0.3_to_client_192.168.25.63.pcap";
	
	public static final String PCAP_DIR_PATH = "pcap_files/";
	public static final String PCAP_FILE_PATH = PCAP_DIR_PATH+PCAP_FILENAME;
	public static final String FILENAME_ONLY = PCAP_FILENAME.substring(0, PCAP_FILENAME.indexOf(".pcap"));
	
	public static final String OUTPUT_DIR_PATH = "output/";
	public static final String OUTPUT_FLOW_DIR_PATH = "output/flow/";
	
	public static final String LOG_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME_ONLY+"_log.txt";
	public static final String RESULT_TIME_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME_ONLY+"_resultTime.txt";
	public static final String RESULT_FLOW_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME_ONLY+"_resultFlow.txt";
	public static final String ALL_FLOW_CSV_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME_ONLY+"_allFlows.csv";
	
	public static final String resultFlowCsvFilePath(Flow flow) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+Parameters.FILENAME_ONLY+"_resultFlow_"+flow.toStringForFileName()+".csv";
	}
	
	
	public static final int MQTT_FRAGMENT_BYTES_THRESHOLD = 30;
	
}
