package blasd.wildling;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AsyncHttpCaller {

	protected final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

	protected final BlockingQueue<String> urlToRequest;

	protected final String urlPrefix;
	protected final int queueMaxSize;
	protected final Application application;

	public AsyncHttpCaller(String urlPrefix, int queueMaxSize, Application application) {
		this.urlPrefix = urlPrefix;
		this.queueMaxSize = queueMaxSize;
		this.application = application;

		urlToRequest = new LinkedBlockingQueue<String>(queueMaxSize);
	}

	public void offer(String url) {
		urlToRequest.offer(url);
	}

	public void startSchedule(int period, TimeUnit unit) {
		// Schedule regular sending of messages
		ses.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				if (isConnected()) {
					List<String> urlsToSend = new ArrayList<String>();

					urlToRequest.drainTo(urlsToSend);

					// TODO: Find a way to send all messages in a single HTTP
					// call
					for (String url : urlsToSend) {
						try {
							if (urlPrefix == null) {
								sendURL(Collections.singletonList(url));
							} else {
								sendURL(Collections.singletonList(urlPrefix + url));
							}
						} catch (IOException e) {
							if (e.getCause() instanceof ProtocolException && e.getCause().getCause() instanceof URISyntaxException) {
								// Don't retry
							} else {
								// retry
								urlToRequest.offer(url);
							}
						} catch (RuntimeException e) {
							// retry
							urlToRequest.offer(url);
						}
					}
				} else {
					// Not connected
				}

			}
		}, 0, period, unit);
	}

	private static List<String> sendURL(List<String> singletonList) throws ClientProtocolException, IOException {
		List<String> results = new ArrayList<String>();
		HttpClient hc = new DefaultHttpClient();

		for (String url : singletonList) {
			HttpPost post = new HttpPost(url);

			HttpResponse rp = hc.execute(post);

			// TODO: check the result is OK, else retry later. To workaround
			// temporary failure of the server

			if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				results.add(EntityUtils.toString(rp.getEntity()));
			}
		}
		return results;
	}

	public boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		} else {
			return false;
		}
	}
}
