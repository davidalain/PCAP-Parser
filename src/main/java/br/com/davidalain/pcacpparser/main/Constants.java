package br.com.davidalain.pcacpparser.main;

public class Constants {

	public static final String BROKER1_IP = "172.16.0.3";
	public static final String BROKER2_IP = "172.16.0.2";
	public static final String CLIENT1_IP = "192.168.43.1";
	public static final String CLIENT2_IP = "192.168.43.2"; //FIXME

	public static final String FILEPATH = "tracex.pcap";
	
	public static final String PREFIX = FILEPATH.substring(0, FILEPATH.length() - 5);
	public static final String OUTPUT_PATH = "output/";
	public static final String OUTPUT_FLOW_PATH = "output/flow/";
	
}
