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
	
	/**
	 * Read nBytes from byteArray and convert them to a int value
	 * 
	 * @param byteArray		Array that contains bytes to be converted to int 
	 * @param offset		Initial index
	 * @param nBytes		How many bytes represents number that will be converted 
	 * @return
	 */
	public static int toInt(byte[] byteArray, int offset, int nBytes) {
		int ret = 0;
		for (int i=offset; i<offset+nBytes && i<byteArray.length; i++) {
			ret <<= 8;
			ret |= (int)byteArray[i] & 0xFF;
		}
		return ret;
	}
	
}
