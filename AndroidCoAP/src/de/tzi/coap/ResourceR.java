package de.tzi.coap;

import android.util.Log;

public class ResourceR {
	short[] data;

	private int temp;
	private int hum;
	private int volt;	

	public ResourceR(short[] data) {
		this.data = data;

		int temp = 0;
		temp = (byte)(data[1] & 0xFF);
		temp = temp << 8;
		temp = temp + (byte)(data[2] & 0xFF);
		this.temp = temp;

		int hum = 0;
		hum = (byte)(data[4] & 0xFF);
		hum = hum << 8;
		hum = hum + (byte)(data[5] & 0xFF);
		this.hum = hum;

		int volt = 0;
		volt = (byte)(data[7] & 0xFF);
		volt = volt << 8;
		volt = volt + (byte)(data[8] & 0xFF);
		this.volt = volt;
	}

	public void show() {
		Log.i(CoAPClient.LOG_TAG, "INF: ResourceR: "+data[0]+"  "+this.temp);
	}

	public int getTemp() {
		return temp;
	}

	public int getHum() {
		return hum;
	}

	public int getVolt() {
		return volt;
	}	
}
