package de.tzi.coap08;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class IPv6AddressesHelper extends AsyncTask<TextView, String, String> {
	TextView ipv6Address;

	@Override
	protected String doInBackground(TextView... params) {
		ipv6Address = params[0];
		onProgressUpdate("Getting IPv6");
		Boolean ipv6Available = false;
		try {
			// Executes the command.
			// Process process = Runtime.getRuntime().exec("cat /proc/net/arp");
			// Process process =
			// Runtime.getRuntime().exec("ip -f inet6 addr show wlan0 scope global");
			Process process = Runtime.getRuntime().exec(
					"cat /proc/net/if_inet6");

			// Reads stdout
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			StringBuilder log = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {

				String[] buffer;
				buffer = line.split(" ");
				if (!(line.startsWith("0000") || line.startsWith("fe"))) {
					StringBuffer output = new StringBuffer(buffer[0]);

					int i = 4;
					while (output.length() < 39) {
						output.insert(i, ":");
						i = i + 5;
					}
					log.append("\n" + output);
					ipv6Available = true;
				}

			}
			Log.d("IPHELPER", "" + log);

			reader.close();

			// Waits for the command to finish.
			process.waitFor();

			// return output.toString();
			if (ipv6Available)
				return log.toString();
			else
				return "IPv6 not available...";

		} catch (IOException e) {
			// do nothing
		} catch (InterruptedException e) {
			// do nothing
		} catch (RuntimeException re) {
			// do nothing
		}
		return "IPv6 not available...";
	}

	protected void onProgressUpdate(String... progress) {
		ipv6Address.setText(progress[0]);
	}

	protected void onPostExecute(String result) {
		ipv6Address.setText(result);
	}

}
