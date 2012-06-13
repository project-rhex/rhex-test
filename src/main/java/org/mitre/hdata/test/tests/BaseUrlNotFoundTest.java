package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * 6.2.1 GET Operation on the Base URL
 *
 * If there is no HDR at the base URL, the server SHOULD return a 404 - Not found status code.
 *
 * Status Code: 200, <B>404</B>
 * </pre>
 * @author Jason Mathews, MITRE Corp.

 * Date: 2/23/12 10:45 AM
 */
public class BaseUrlNotFoundTest extends BaseTest {

	private static final Logger log = LoggerFactory.getLogger(BaseUrlNotFoundTest.class);

	@NonNull
	public String getId() {
		return "6.2.1.1";
	}

	@NonNull
	public String getName() {
		return "GET operation on non-existent HDR baseURL SHOULD return 404";
	}

	@Override
	public boolean isRequired() {
		return false; // SHOULD
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList();
	}

	public void execute() throws TestException {
		final Context context = Loader.getInstance().getContext();
		//final String baseURL = context.getString("invalidBaseURL");
		URI baseURL = context.getPropertyAsURI("invalidBaseURL");
		// test pre-conditions
		if (baseURL == null) {
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid invalidBaseURL property in configuration");
			return;
		}
		if (log.isDebugEnabled()) {
			System.out.println("\nGET URL: " + baseURL);
		}
		HttpClient client = context.getHttpClient();
		try {
			HttpGet req = new HttpGet(baseURL);
			req.setHeader("Accept", MIME_APPLICATION_ATOM_XML);
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (code == 404) {
				setStatus(StatusEnumType.SUCCESS);
			} else {
				// fails to meet recommendation/should element in the specification
				//addWarning("Expected 404 HTTP status code but was: " + code);
				dumpResponse(req, response);
				setStatus(StatusEnumType.FAILED, "Expected 404 HTTP status code but was: " + code);
			}
			// assertEquals(404, code);
		} catch (IOException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}
