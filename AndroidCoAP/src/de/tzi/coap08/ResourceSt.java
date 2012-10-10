package de.tzi.coap08;

import android.util.Log;

public class ResourceSt {
	short[] data;

	private int temp;

	public ResourceSt(short[] data) {
		this.data = data;

		Log.i(CoAPClient.LOG_TAG, "INF: ResourceSt: data.len: " + data.length);
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			sb.append((char)data[i]);
		}
		Log.i(CoAPClient.LOG_TAG, "INF: ResourceSt: string: " + sb.toString());
		
		temp = Integer.valueOf(sb.toString());
		
		/*int temp = 0;
		temp = (byte)(data[1] & 0xFF);
		temp = temp << 8;
		temp = temp + (byte)(data[0] & 0xFF); 
		this.temp = temp;*/
		//temp = 0;
	}

	/*public void show() {
		Log.i(CoAPClient.LOG_TAG, "INF: ResourceSt: "+data[0]+" "+data[1]);
	}*/

	public int getTemp() {
		return temp;
	}
}
