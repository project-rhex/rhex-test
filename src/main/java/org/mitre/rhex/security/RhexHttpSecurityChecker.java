package org.mitre.rhex.security;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.mitre.test.ClientHelper;
import org.mitre.test.Context;
import org.mitre.test.HttpRequestChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * MITRE RHEX Patient Data Server HTTP request security handler
 * implements simple authentication using developer callback URL.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 3/30/12 12:51 PM
 */
public class RhexHttpSecurityChecker implements HttpRequestChecker {

	private static final Logger log = LoggerFactory.getLogger(RhexHttpSecurityChecker.class);

	private HttpContext localContext;

	/**
	 * Setups and initializes the HttpRequestChecker
	 *
	 * @param context   Application context
     * @throws IllegalArgumentException if setup/configuration fails
     */
	@Override
	public void setup(Context context) {
		log.debug("XXX: try callback to enable authentication");

        final URI uri = context.getPropertyAsURI("loginURL");
        if (uri == null) {
			throw new IllegalArgumentException("loginURL property not defined");
        }

        String loginEmail = context.getString("loginEmail");
        String loginPassword = context.getString("loginPassword");
        if (StringUtils.isBlank(loginEmail) || StringUtils.isBlank(loginPassword)) {
			throw new IllegalArgumentException("loginEmail and loginPassword properties are empty or missing");
        }

		log.debug("POST auth URL: {}", uri);
		HttpPost httppost = new HttpPost(uri);
		httppost.setHeader("Cache-Control", "no-cache");
		List<NameValuePair> formParams = new ArrayList<NameValuePair>(2);
		formParams.add(new BasicNameValuePair("email", loginEmail));
		formParams.add(new BasicNameValuePair("id", loginPassword));
		final HttpResponse response;
		HttpClient client = null;
		HttpContext httpContext = localContext;
		try {
			httppost.setEntity(new UrlEncodedFormEntity(formParams));
			client = context.getHttpClient();
			if (httpContext == null) {
				// Create a local instance of cookie store
				CookieStore cookieStore = new BasicCookieStore();
				// Create local HTTP context
				httpContext = new BasicHttpContext();
				// Bind custom cookie store to the local context
				httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
			}
			response = client.execute(httppost, httpContext);
			/*
			 <form method='post' action='/auth/developer/callback' noValidate='noValidate'>
			 <label for='name'>Name:</label>
			 <input type='text' id='name' name='name'/>
			 <label for='email'>Email:</label>
			 <input type='text' id='email' name='email'/>
			 <button type='submit'>Sign In</button>      </form>
			 */
			if (log.isDebugEnabled() || !checkAuth(response)) {
                ClientHelper.dumpResponse(httppost, response, true);
			}
		} catch (IOException e) {
			log.error("", e);
			return;
		} finally {
			if (client != null)
				client.getConnectionManager().shutdown();
		}

		final StatusLine statusLine = response.getStatusLine();
		if (statusLine != null && statusLine.getStatusCode() == 302) {
			// HTTP/1.1 302 Moved Temporarily
			Header cookie = response.getFirstHeader("Set-Cookie");
			if (cookie != null) {
				log.debug("XXX: set local context");
				this.localContext = httpContext;
			}
			else log.error("Expected Set-Cookie header in response");
			if (log.isDebugEnabled()) {
				Header location = response.getFirstHeader("Location");
				if (location != null) {
					checkAuthentication(context, location.getValue());
					//log.debug("XXX: set local context");
					//this.localContext = httpContext;
				}
				else log.error("Expected Location header in response");
			}
		} else {
			log.error("Expected 302 status code in response");
		}
	}

    @Override
    public void setUser(Context context, String userId, String userEmail, String userPassword) {
        // not implemented
    }

    @Override
    public String getCurrentUser(Context context) {
        return null; // not implemented
    }

    private static boolean checkAuth(HttpResponse response) {
		final StatusLine statusLine = response.getStatusLine();
		if (statusLine == null || statusLine.getStatusCode() != 302) return false;
		Header location = response.getFirstHeader("Location");
		/*
		 * if successful; then Location should be web server root (e.g. http://rhex.mitre.org:3000/)
		 * otherwise redirects to /users/sign_up
		 */
		return location != null && !StringUtils.endsWith(location.getValue(), "/users/sign_up");
	}

	private void checkAuthentication(Context context, String target) {
		HttpClient client = context.getHttpClient();
		try {
			log.debug("GET auth URL: {}", target);
			HttpGet req = new HttpGet(target);
			HttpResponse response = client.execute(req, localContext);
			if (log.isTraceEnabled() || response.getStatusLine().getStatusCode() != 200) {
                ClientHelper.dumpResponse(req, response, false);
			}
			//StatusLine statusLine = response.getStatusLine();
			//return statusLine != null && statusLine.getStatusCode() == 200;
		} catch (IOException e) {
			log.debug("", e);
			//return false;
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

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
		return client.execute(req, localContext);
	}

}