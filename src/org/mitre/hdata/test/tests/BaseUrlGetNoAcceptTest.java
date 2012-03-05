package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.mitre.hdata.test.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * 6.2.1 GET Operation on the Base URL
 *
 * The server MUST offer an Atom 1.0 compliant feed of all child sections specified in HRF specification [1],
 * as identified in the corresponding sections node in the root document.
 *
 * If the Accept header is <B>non-existent</B>, or set to * '/' * or application/atom+xml, the system MUST
 * return the Atom feed.
 *
 * Status Code: 200, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlGetNoAcceptTest extends BaseUrlGetTest {

	@NonNull
	public String getId() {
		return "6.2.1.2";
	}

	@Override
	public boolean isRequired() {
		return true; // MUST
	}

	@NonNull
	public String getName() {
		return "GET operation on baseURL MUST return Atom 1.0 feed of child sections if accept header is non-existent";
	}

	protected String getAcceptHeader() {
		// null will omit sending the HTTP Accept header
		return null;
	}

}
