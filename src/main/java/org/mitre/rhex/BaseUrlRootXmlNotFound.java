package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * <pre>
 * 6.3 baseURL/root.xml
 *
 * 6.3.1 GET
 *
 * This operation [MUST] return an XML representation of the current root document,
 * as defined by the HRF specification.
 *
 * Implies GET operation on a baseURL that does not exist must return a 404
 * not found status code.
 *
 * Status Code: 200, [404]
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlRootXmlNotFound extends BaseTest {

	@NonNull
	public String getId() {
		return "6.3.1.2";
	}

	@Override
	public boolean isRequired() {
		return false; // implied requirement
	}

	@NonNull
	public String getName() {
		return "If baseURL does not exist then GET on baseURL/root.xml MUST return 404 status code";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList(); // none
	}

	public void execute() throws TestException {
		final Context context = Loader.getInstance().getContext();
		String baseURL = context.getString("invalidBaseURL");
		// test pre-conditions
		if (baseURL == null) {
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid invalidBaseURL property in configuration");
			return;
		}
		if (baseURL.endsWith("/"))
			baseURL += "root.xml";
		else
			baseURL += "/root.xml";
		final URI uri;
		try {
			uri = new URI(baseURL);
		} catch (URISyntaxException e) {
			setStatus(StatusEnumType.SKIPPED, "Failed to construct valid URI to run test");
			log.warn("", e);
			return;
		}

		HttpClient client = context.getHttpClient();
		try {
			HttpGet req = new HttpGet(uri);
			req.setHeader("Accept", MIME_APPLICATION_XML);
			if (log.isDebugEnabled()) {
				System.out.println("\nGET URL: " + req.getURI());
			}
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (code != 404) {
				dumpResponse(req, response, log.isDebugEnabled());
				setStatus(StatusEnumType.FAILED, "Expected 404 HTTP status code but was: " + code);
			} else {
				setStatus(StatusEnumType.SUCCESS);
			}
		} catch (IOException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}
