package de.tzi.coap08;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import de.tzi.coap08.R;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

/**
 * Handles data between JavaWorld and JavaScript World
 * 
 * 
 */
public class FlotGraphHandler {
	private WebView mAppView;
	private Context ctx;
	JSONArray data;
	String startTime;

	public FlotGraphHandler(Context AppContext, WebView appView, JSONArray stats, String time) {
		mAppView = appView;
		data = stats;
		ctx = AppContext;
		startTime = time;
	}

	/**
	 * Set the title of the graph
	 * 
	 * @return
	 */
	public String getGraphTitle() {
		return ctx.getResources().getString(R.string.graph_title);
	}
	
	/**
	 * Set the label for x-axis
	 * 
	 * @return
	 */
	public String getXAxisLabel() {
		return ctx.getResources().getString(R.string.graph_x_axis) + " " + startTime;
	}

	/**
	 * Load Graph data
	 */
	public void loadGraph() {
		JSONObject options = new JSONObject();
		try {

			// adding options
			options.put("lines", getLineOptionsJSON());
			options.put("points", getPointsOptionsJSON());
			options.put("legend", getLegendsOptionsJSON());
			options.put("grid", getGridOptionsJSON());

		} catch (JSONException e) {
			Log.d(this.getClass().getSimpleName(),
					"Got an exception while trying to parse JSON");
			e.printStackTrace();
		}

		// Log.d(this.getClass().getSimpleName(), data.toString() + ","
		// + options.toString());
		mAppView.loadUrl("javascript:GotGraph(" + data.toString() + ","
				+ options.toString() + ")"); // this callback works!
	}

	private JSONObject getLineOptionsJSON() throws JSONException {
		JSONObject lineOption = new JSONObject();
		lineOption.put("show", true);
		return lineOption;
	}

	private JSONObject getPointsOptionsJSON() throws JSONException {
		JSONObject pointsOption = new JSONObject();
		pointsOption.put("show", true);
		pointsOption.put("radius", 1);
		return pointsOption;
	}

	private JSONObject getLegendsOptionsJSON() throws JSONException {
		JSONObject legendOption = new JSONObject();
		legendOption.put("show", true);
		legendOption.put("position", "ne");
		legendOption.put("noColumns", "3");
		legendOption.put("container", "#legend");
		return legendOption;
	}

	private JSONObject getGridOptionsJSON() throws JSONException {
		JSONObject gridOption = new JSONObject();
		JSONObject bgColorsOption = new JSONObject();
		JSONArray bgColor = new JSONArray();
		bgColor.put("#fff");
		bgColor.put("#eee");
		bgColorsOption.put("colors", bgColor);
		gridOption.put("backgroundColor", bgColorsOption);
		return gridOption;
	}
}