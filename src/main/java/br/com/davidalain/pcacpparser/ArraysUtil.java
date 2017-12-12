package br.com.davidalain.pcacpparser;

public class ArraysUtil {

	/**
	 * Checks if 'sub' is inside of 'array'
	 * 
	 * @param array
	 * @param sub
	 * @return
	 */
	public static boolean contains(byte[] array, byte[] sub) {
		
		for(int i = 0 ; i < array.length - sub.length; i++) {
			
			int count = 0;
			for(int j = 0 ; j < sub.length ; j++) {
				
				if(array[i + j] == sub[j]) {
					count++;
				} else {
					break;
				}
			}
			
			if(count == sub.length)
				return true;
		}
		
		return false;
	}
	
}
