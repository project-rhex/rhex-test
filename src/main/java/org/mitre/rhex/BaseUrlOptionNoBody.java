package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.mitre.test.BaseTest;
import org.mitre.test.TestException;
import org.mitre.test.TestUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * 6.2.5 OPTIONS
 *
 * The OPTIONS operation on the baseURL is per [8], section 9.2, intended to return communications options to the clients.
 * Within the context of this specification, OPTIONS is used to indicate which security mechanisms are available for a given
 * baseURL and a list of hData content profiles supported by this implementation. All implementations MUST support
 * OPTIONS on the baseURL of each HDR and return a status code of 200. <P>
 *
 * The response SHOULD NOT include an HTTP body. <P>
 *
 * Status Code: 200
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlOptionNoBody extends BaseTest {

	public BaseUrlOptionNoBody() {
		// forces source test to keep its Document objects after it executes
		setProperty(BaseUrlOptions.class, PROP_KEEP_RESPONSE_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.2.5.6";
	}

	@Override
	public boolean isRequired() {
		return false; // SHOULD NOT
	}

	@NonNull
	public String getName() {
		return "OPTIONS response SHOULD NOT include an HTTP body ";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(BaseUrlOptions.class); // 6.2.5.1
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseUrlOptions must have passed
		// with 200 HTTP response
		TestUnit baseTest = getDependency(BaseUrlOptions.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}

		HttpResponse response = ((BaseUrlOptions)baseTest).getResponse();
		if (response == null) {
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}

		final HttpEntity entity = response.getEntity();
		if (entity != null) {
			// response SHOULD NOT include an HTTP body so entity should be null
			setStatus(StatusEnumType.FAILED, "Response includes a HTTP body");
		}
	}

}
