package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * <pre>
 * 6.2.1 GET Operation on the Base URL
 *
 * The server MUST offer an Atom 1.0 compliant feed of all child sections specified in
 * HRF specification [1], as identified in corresponding sections node in the root document.
 *
 * If the Accept header is non-existent, or set to <B>* '/' *</B> or application/atom+xml,
 * the system MUST return the Atom feed.
 *
 * Status Code: 200, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlGetStarAcceptTest extends BaseUrlGetTest {

	@NonNull
	public String getId() {
		return "6.2.1.3";
	}

	@Override
	public boolean isRequired() {
		return true; // MUST
	}

	@NonNull
	public String getName() {
		return "GET operation on baseURL MUST return Atom 1.0 feed of child sections if accept header is */*";
	}

	protected String getAcceptHeader() {
		return "*/*"; // Accept: */*
	}

}
