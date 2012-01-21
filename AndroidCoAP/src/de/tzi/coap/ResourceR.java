package de.tzi.coap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import android.util.Log;

import de.tzi.coap.jni.coap;
import de.tzi.coap.jni.coapConstants;
import de.tzi.coap.jni.coap_context_t;
import de.tzi.coap.jni.coap_pdu_t;
import de.tzi.coap.jni.str;

public class ResourceR {
	ByteBuffer bb;
	int temp;
	int hum;
	int volt;	
	short dd;
	short[] data;
//	String data;
	
	public ResourceR(short[] data) {

		this.data = data;	
		this.temp = (((char)data[1])<<8 | ((char)data[2]));

	}

	
	public void show() {
		Log.i(CoAPClient.LOG_TAG, "INF: ResourceR: "+data[0]+"  "+this.temp);
		
//		System.out.println(RCI+"show length "+data.length +"  "+ volt + Character.getNumericValue(volt));
		
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