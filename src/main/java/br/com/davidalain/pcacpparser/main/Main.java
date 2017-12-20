package br.com.davidalain.pcacpparser.main;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
	
	public static final String JAR_RUNNABLE_NAME = "mqttAnalyzer.jar";

	private static final String SYNC = "-sync";
	private static final String RTT = "-rtt";
	
	private static final String BROKER = "-b";
	private static final String CLIENT = "-c";

	public static void main(String[] args) throws IOException {

		List<String> list = Arrays.asList(args);

		//At least one
		if(!list.contains(SYNC) && !list.contains(RTT)) {
			
			System.err.println("You must specify " + SYNC + " or (exclusive) " + RTT + " parameters to run.");
			usage(args);
			return;
		}

		//Only one
		if(list.contains(SYNC) && list.contains(RTT)) {
			
			System.err.println("You must specify " + SYNC + " or (exclusive) " + RTT + " parameters to run.");
			usage(args);
			return;
		}

		if(list.contains(SYNC)) {

			String pcapFilePath = null;
			String brokerIP = null;
			
			if(args.length >= 3 && args[2].endsWith(".pcap")) {
				pcapFilePath = args[2];
				
			}else {
				usage(args);
				return;
			}
			
			if(args.length >= 5 && PathUtil.isIpAddress(args[4])) {
				brokerIP = args[4];
			}else {
				usage(args);
				return;
			}
			
			MainClusterSync.run(pcapFilePath, brokerIP);
			return;
		}
		
		if(list.contains(SYNC)) {

			if(args.length < 4){
				usage(args);
				return;
			}
			
			if(!args[0].equals(SYNC)){
				usage(args);
				return;				
			}
			
			if(!args[1].endsWith(".pcap")){
				usage(args);
				return;								
			}
				
			if(!args[2].equals(BROKER)){
				usage(args);
				return;
			}
			
			if(!PathUtil.isIpAddress(args[3])) {
				usage(args);
				return;
			}
			
			final String pcapFilePath = args[1];
			final String clientIP = args[3];
			
			MainClusterSync.run(pcapFilePath, clientIP);
			return;
		}

		if(list.contains(RTT)) {

			if(args.length < 4){
				usage(args);
				return;
			}
			
			if(!args[0].equals(RTT)){
				usage(args);
				return;				
			}
			
			if(!args[1].endsWith(".pcap")){
				usage(args);
				return;								
			}
				
			if(!args[2].equals(CLIENT)){
				usage(args);
				return;
			}
			
			if(!PathUtil.isIpAddress(args[3])) {
				usage(args);
				return;
			}
			
			final String pcapFilePath = args[1];
			final String clientIP = args[3];
			
			MainPublishMessageRTT.run(pcapFilePath, clientIP);
			return;
		}

	}

	private static void usage(String[] args) {
		
		System.err.println("Examples:");
		System.err.println("java -jar " + JAR_RUNNABLE_NAME + " " + SYNC + " <file.pcap> " + BROKER + " <broker IP>");
		System.err.println("or");
		System.err.println("java -jar " + JAR_RUNNABLE_NAME + " " + RTT + " <file.pcap> " + CLIENT + " <client IP>");

	}

}
