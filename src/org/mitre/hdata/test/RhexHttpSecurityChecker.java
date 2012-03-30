package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * MITRE RHEX Patient Data Server HTTP request security handler implements
 * simple authentication using developer callback URL.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 3/30/12 12:51 PM
 */
public class RhexHttpSecurityChecker implements HttpRequestChecker {

	private static final Logger log = LoggerFactory.getLogger(RhexHttpSecurityChecker.class);

	private Header cookieHeader;

	private boolean securityCheck;

	/**
	 * Wrap <tt>HttpClient.execute()</tt> to pre/post-test HTTP requests for
	 * any server specific implementation handling such as authentication.
	 *
	 * @param context   Application context
	 * @param client   the HttpClient, must never be null
	 * @param req   the request to execute, must never be null
	 *
	 * @return  the response to the request.
	 * @throws IOException in case of a problem or the connection was aborted
	 * @throws ClientProtocolException in case of an http protocol error
	 */
	@NonNull
	public HttpResponse executeRequest(Context context, HttpClient client, HttpUriRequest req)
			throws IOException
	{
		if (cookieHeader != null) {
			log.trace("XXX: Add authentication cookie");
			req.addHeader(cookieHeader);
		}
		HttpResponse response = client.execute(req);
		StatusLine statusLine = response.getStatusLine();
		if (!securityCheck && statusLine != null && statusLine.getStatusCode() == 401) {
			log.debug("XXX: try callback for authentication cookie");
			securityCheck = true;
			URI uri;
			try {
				URI baseURL = context.getBaseURL();
				uri = new URI(String.format("%s://%s:%d/auth/developer/callback",
						baseURL.getScheme(), baseURL.getHost(), baseURL.getPort()));
			} catch (URISyntaxException e) {
				log.error("", e);
				return response;
			}
			log.debug("POST auth URL: {}", uri);
			HttpPost httppost = new HttpPost(uri);
			httppost.setHeader("Cache-Control", "no-cache");
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(2);
			formParams.add(new BasicNameValuePair("email", "testclient@mitre.org"));
			formParams.add(new BasicNameValuePair("id", "testclient"));
			final HttpResponse postResponse;
			try {
				httppost.setEntity(new UrlEncodedFormEntity(formParams));
				client = context.getHttpClient();
				postResponse = client.execute(httppost);
				/*
				 <form method='post' action='/auth/developer/callback' noValidate='noValidate'>
				 <label for='name'>Name:</label>
				 <input type='text' id='name' name='name'/>
				 <label for='email'>Email:</label>
				 <input type='text' id='email' name='email'/>
				 <button type='submit'>Sign In</button>      </form>
				 */
				if (log.isTraceEnabled()) {
					for (Header header : postResponse.getAllHeaders()) {
						System.out.println("\t" + header.getName() + ": " + header.getValue());
					}
					System.out.println(EntityUtils.toString(postResponse.getEntity()));
				}
			} catch (UnsupportedEncodingException e) {
				log.error("", e);
				return response;
			} catch (IOException e) {
				log.error("", e);
				return response;
			} finally {
				client.getConnectionManager().shutdown();
			}
			statusLine = postResponse.getStatusLine();
			if (statusLine != null && statusLine.getStatusCode() == 302) {
				// HTTP/1.1 302 Moved Temporarily
				Header cookie = postResponse.getFirstHeader("Set-Cookie");
				if (cookie != null) {
					client = context.getHttpClient();
					Header header = new BasicHeader("Cookie", cookie.getValue());
					req.addHeader(header);
					log.debug("Retry {} URL: {}", req.getMethod(), req.getURI());
					response = client.execute(req); // retry the request with cookie
					statusLine = response.getStatusLine();
					if (statusLine != null && statusLine.getStatusCode() != 401) {
						log.debug("XXX: save auth cookie");
						cookieHeader = header; // set if no exception thrown
					}
				}
			}
		}

		return response;
	}
}
