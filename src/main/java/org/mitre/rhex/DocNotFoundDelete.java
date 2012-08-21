package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Test for section document deletion
 *
 * <pre>
 * 6.5 baseURL/sectionpath/documentname
 *
 * 6.5.4 DELETE
 *
 * This operation MAY be implemented. If a DELETE is sent to the document URL,
 * the document is completely deleted. If DELETE is implemented, special
 * precautions should be taken to assure against accidental or malicious
 * deletion.
 *
 * Future requests to the section URL MAY return a status code of 410,
 * unless the record is restored.
 *
 * Status Code: 204, 410, [404], [405]
 *
 * 6.1.2 General Conventions
 *
 * Any HTTP GET, PUT, POST, DELETE, or OPTIONS operation on a given resource
 * that are not implemented MUST return an HTTP response with a status code
 * of 405 that includes an Allow header that specifies the allowed methods.
 *
 * If baseURL, sectionpath, or target document name does NOT exist then SHOULD
 * return <B>404</B> (not found) or <B>405</B> (not implemented) status [implied].
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocNotFoundDelete extends DocumentNotFound {

	@NonNull
	@Override
	public String getId() {
		return "6.5.4.4";
	}

	@Override
	public boolean isRequired() {
		return false; // implied SHOULD
	}

	@NonNull
	public String getName() {
		return "DELETE sent to non-existing document URL should return 404 or 405 status";
	}

	protected void validateResponse(HttpRequestBase req, HttpResponse response) throws TestException {
		int code = response.getStatusLine().getStatusCode();
		boolean success = code == 404 || code == 405;
		if (!success || log.isDebugEnabled()) {
			dumpResponse(req, response, true);
		}
		/*
		server responds with 404 code but HTML content shows an error so may not be handled correctly:

		<h1>Routing Error</h1>
		<p><pre>No route matches [DELETE] &quot;/records/1/medications/should_not_exist&quot;</pre></p>
		<p>
		  Try running <code>rake routes</code> for more information on available routes.
		</p>
		 */
		if (!success) {
			fail("Expected 404/405 status code but was " + code);
		}
	}

	protected HttpRequestBase createRequest(URI baseURL) {
		HttpDelete req = new HttpDelete(baseURL);
		//req.setHeader("Accept", MIME_APPLICATION_JSON);
		req.setHeader("Accept", "application/json, application/xml");
		return req;
	}

}