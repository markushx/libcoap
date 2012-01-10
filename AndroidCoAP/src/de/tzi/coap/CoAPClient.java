package de.tzi.coap;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.Vector;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import de.tzi.coap.jni.*;

/*
 * @author Markus Becker <mab@comnets.uni-bremen.de>
 * @author Thomas Poetsch <thp@comnets.uni-bremen.de>
 */

public class CoAPClient extends Activity {

	private static final String LOG_TAG = "CoAP";

	// CoAP specifics
	static Random generateRand = new Random();
	coap_context_t ctx;

	DatagramSocket clientSocket;
	LowerReceive lr = null;
	Retransmitter rt = null;

	static {
		try{
			Log.i(LOG_TAG, "static load library");
			System.loadLibrary("coap");
			Log.i(LOG_TAG, "static load library success");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOG_TAG, "static load library fail");
		}
	}

	//TODO: call setup_coap in onCreate()?
	public void setup_coap() {
		// create coap_context
		Log.i(LOG_TAG, "INF: create context");
		ctx = coap.coap_new_context(77777);
		if (ctx == null) {
			Log.e(LOG_TAG, "ERR: Could not create context");
			return;
		} else {
			Log.i(LOG_TAG, "INF: created context");
		}

		// register ourselves for message handling
		Log.i(LOG_TAG, "INF: register message handler");
		coap.register_message_handler(ctx, this);
		Log.i(LOG_TAG, "INF: registered message handler");

		try {
			clientSocket = new DatagramSocket();
			Log.i(LOG_TAG, "INF: open new socket on port " + clientSocket.getLocalPort());
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		lr = new LowerReceive(ctx, clientSocket);
		lr.start();

		rt = new Retransmitter(ctx);
		rt.start();
	}

	//TODO: do in onDestroy()?
	protected void finalize() throws Throwable {
		try {
			// free context
			Log.i(LOG_TAG, "INF: finalize");
			coap.coap_free_context(ctx);
		} finally {
			super.finalize();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_client);

		Button run = (Button)findViewById(R.id.button1);
		run.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {

				//				new runClient().execute(null);
				run();
			}
		});


		//		Log.i(LOG_TAG, "create context");    
		//		coap_context_t ctx = coap.coap_new_context(77777);
		//		if (ctx == null) {
		//			Log.e(LOG_TAG, "Could not create context");
		//			Toast.makeText(getBaseContext(), 
		//					"ERR: create context fail", 
		//					Toast.LENGTH_LONG).show();
		//			return;
		//		}
		//		Log.i(LOG_TAG, "create context success");
		//		Toast.makeText(getBaseContext(), 
		//				"INF: create context success", 
		//				Toast.LENGTH_LONG).show();
		//		coap.register_message_handler(ctx, this);
		//        coap_pdu_t pdu = coap_new_request(method, optlist, payload);

	}

	//	private class runClient extends AsyncTask<String, String, String> {
	//		protected String doInBackground(String... s) {
	//			
	//			
	//			return null;
	//		}
	//
	//		protected void onProgressUpdate(String... progress) {
	//			//				errTextView.setText(""+progress[0]);
	//		}
	//
	//		protected void onPostExecute(String result) {
	//			//				toggleButton.setChecked(false);
	//		}
	//	}

	public void run() {
		String destination = "::1";
		int port = coapConstants.COAP_DEFAULT_PORT;
		int method = coapConstants.COAP_REQUEST_GET;
		int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
		String uri = "/l";
		String token = "3a";
		String payload = "Hello CoAP";

		Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();
		CoapJavaOption contentTypeOption = new CoapJavaOption(coapConstants.COAP_OPTION_CONTENT_TYPE, ""+(char)content_type, 1);
		optionList.add(contentTypeOption);
		CoapJavaOption uriOption = new CoapJavaOption(coap.COAP_OPTION_URI_PATH, uri, uri.length());
		optionList.add(uriOption);
		CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN, token, token.length());
		optionList.add(tokenOption);
		if ( method == coapConstants.COAP_REQUEST_GET || method == coapConstants.COAP_REQUEST_DELETE) {
			payload = null;
		}

		Log.i(LOG_TAG, "create context");    
		coap_context_t ctx = coap.coap_new_context(77777);
		if (ctx == null) {
			Log.e(LOG_TAG, "Could not create context");
			Toast.makeText(getBaseContext(), 
					"ERR: create context fail", 
					Toast.LENGTH_LONG).show();
			return;
		}
		Log.i(LOG_TAG, "create context success");
		Toast.makeText(getBaseContext(), 
				"INF: create context success", 
				Toast.LENGTH_LONG).show();
		coap.register_message_handler(ctx, this);

		coap_pdu_t pdu = coap_new_request(method, optionList, payload);

		if (pdu == null) {
			System.err.println("Could not create pdu");
			return;
		}

		// set destination
		//sockaddr_in6 dst;
		SWIGTYPE_p_sockaddr_in6 dst;
		dst = coap.sockaddr_in6_create(coapConstants.AF_INET6,
				port,
				destination);
	}

	private void sendRequest(String destination) {
		Vector<CoapJavaOption> optionList = new Vector<CoapJavaOption>();

		int content_type = coapConstants.COAP_MEDIATYPE_APPLICATION_OCTET_STREAM;
		CoapJavaOption contentTypeOption = new CoapJavaOption(
				coapConstants.COAP_OPTION_CONTENT_TYPE, ""
						+ (char) content_type, 1);

		String uri = "r";
		//String uri = "lipsum";
		
		optionList.add(contentTypeOption);
		CoapJavaOption uriOption = new CoapJavaOption(
				coap.COAP_OPTION_URI_PATH, uri, uri.length());
		optionList.add(uriOption);

		String token = Integer.toHexString(generateRand.nextInt(0xFF)); 

		CoapJavaOption tokenOption = new CoapJavaOption(coap.COAP_OPTION_TOKEN,
				token, token.length());
		optionList.add(tokenOption);

		int method = coapConstants.COAP_REQUEST_GET;
		String payload = null;
		coap_pdu_t pdu = coap_new_request(method, optionList, payload);

		if (pdu == null) {
			Log.e(LOG_TAG, "ERR: Could not create pdu");
			return;
		}

		// set destination
		SWIGTYPE_p_sockaddr_in6 dst;
		dst = coap.sockaddr_in6_create(coapConstants.AF_INET6, coapConstants.COAP_DEFAULT_PORT,
				destination);

		// send pdu	
		Log.i(LOG_TAG, "INF: send_confirmed");
		coap.coap_send_confirmed(ctx, dst, pdu);
		// will trigger messageHandler() callback

		// free destination
		coap.sockaddr_in6_free(dst);
	}

	//JNI callback to replace C socket with Java DatagramSocket
	public void coap_send_impl(coap_context_t ctx, SWIGTYPE_p_sockaddr_in6 dst, 
			coap_pdu_t pdu,
			int free_pdu) {
		Log.i(LOG_TAG, "INF: callback coap_send_impl @ "+System.currentTimeMillis());

		try {
			InetAddress IPAddress = InetAddress.getByName(coap.get_addr(dst));
			lowerSend(coap.get_bytes(pdu), IPAddress);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.w(LOG_TAG, "WARN: No network connectivity...\n");
			//e.printStackTrace();
		}

		return;
	}

	public void lowerSend(byte[] sendData, InetAddress IPAddress) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, coapConstants.COAP_DEFAULT_PORT);
		Log.i(LOG_TAG, "INF: send message to " + IPAddress.getHostAddress());
		clientSocket.send(sendPacket);		
		return;
	}

	public void messageHandler(coap_context_t ctx, coap_listnode node,
			String data) {
		coap_pdu_t pdu = null;
		//		System.out.println(LI+"INF: Java Client messageHandler()");

		//		System.out.println(LI+"INF: ****** pdu (" + node.getPdu().getLength()
		//				+ " bytes)" + " v:" + node.getPdu().getHdr().getVersion()
		//				+ " t:" + node.getPdu().getHdr().getType() + " oc:"
		//				+ node.getPdu().getHdr().getOptcnt() + " c:"
		//				+ node.getPdu().getHdr().getCode() + " id:"
		//				+ node.getPdu().getHdr().getId());

		if (node.getPdu().getHdr().getVersion() != coapConstants.COAP_DEFAULT_VERSION) {
			Log.w(LOG_TAG, "WARN: dropped packet with unknown version "+
					node.getPdu().getHdr().getVersion()+"\n");
			return;
		}

		/* send 500 response */
		if (node.getPdu().getHdr().getCode() < coapConstants.COAP_RESPONSE_100
				&& node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
			pdu = new_response(ctx, node, coapConstants.COAP_RESPONSE_500);
			finish(ctx, node, pdu);
			return;
		}		
		
		//block not supported yet		
		
		/* just print payload */
		if (node.getPdu().getHdr().getCode() == coapConstants.COAP_RESPONSE_200) {
			//String pdudata = node.getPdu().getData();
			//String pdudata = "";
			short[] pdudata = new short[node.getPdu().getLength()];
			
			int len = coap.coap_get_data_java(node.getPdu(), pdudata);
			Log.i(LOG_TAG, "INF: ****** data:'" + pdudata + "'" +len);
			
			for (int i = 0; i < len; i++) {
				System.out.print(""+pdudata[i]+" ");
			}
			System.out.println("");
			// TODO: insert into buffer
			
			//addFrameToBuffer(node);
//			System.out.println(LI+"INF: "+coap.get_addr(node.getRemote()));
		}
				
		/* acknowledge if requested */
		if (node.getPdu().getHdr().getType() == coapConstants.COAP_MESSAGE_CON) {
			pdu = new_ack(ctx, node);
			Log.i(LOG_TAG, "INF: Acknowledge CON message");
		}
		
		finish(ctx, node, pdu);
		//System.out.println("INF: ~Java messageHandler()");
	}

	public void finish(coap_context_t ctx, coap_listnode node, coap_pdu_t pdu) {
		System.out.println("INF: finish()");
		if ((pdu != null)
				&& (coap.coap_send(ctx, node.getRemote(), pdu) == coapConstants.COAP_INVALID_TID)) {
			Log.e(LOG_TAG, "ERR: message_handler: error sending reponse");
			coap.coap_delete_pdu(pdu);
		}
		//		System.out.println(LI+"INF: doStop = true");
		//		doStop = true;
	}

	// CoAP:
	coap_pdu_t new_ack(coap_context_t ctx, coap_listnode node) {
		coap_pdu_t pdu = coap.coap_new_pdu();

		if (pdu != null) {
			pdu.getHdr().setType(coapConstants.COAP_MESSAGE_ACK);
			pdu.getHdr().setCode(0);
			pdu.getHdr().setId(node.getPdu().getHdr().getId());
		}

		return pdu;
	}

	coap_pdu_t new_response(coap_context_t ctx, coap_listnode node, int code) {
		coap_pdu_t pdu = new_ack(ctx, node);

		if (pdu != null)
			pdu.getHdr().setCode(code);

		return pdu;
	}

	coap_pdu_t coap_new_request(int methodid, Vector<CoapJavaOption> optlist, String payload) {

		coap_pdu_t pdu = coap.coap_new_pdu();
		if (pdu == null) {
			Log.e(LOG_TAG, "INF: could not create pdu");
			return pdu;
		}

		Log.i(LOG_TAG, "INF: set header values");
		pdu.getHdr().setVersion(coapConstants.COAP_DEFAULT_VERSION);
		pdu.getHdr().setType(coapConstants.COAP_MESSAGE_CON);
		pdu.getHdr().setCode(methodid);
		pdu.getHdr().setId(generateRand.nextInt(0xFFFF));

		for (int i=0; i<optlist.size(); i++) {
			coap.coap_add_option(pdu, optlist.get(i).getType(), optlist.get(i).getLength(), optlist.get(i).getValue());
		}

		if (payload != null) {
			coap.coap_add_data(pdu, payload.length(), payload);
		}

		Log.i(LOG_TAG, "INF: created pdu");
		return pdu;
	}

}