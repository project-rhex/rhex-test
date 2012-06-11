package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.mitre.hdata.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Test for section PUT operation
 *
 * <pre>
 * 6.4 baseURL/sectionpath
 *
 * 6.4.3 PUT
 *
 * This operation is not defined by this specification.
 *
 * Status Code: 405, unless an implementer defines this operation.
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class SectionPut extends BaseTest {

	private static final Logger log = LoggerFactory.getLogger(SectionPut.class);

	@NonNull
	public String getId() {
		return "6.4.3";
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@NonNull
	public String getName() {
		return "PUT on section MUST return a 405";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(BaseSectionFromRootXml.class); // 6.4.1.1
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseSectionFromRootXml
		// must have passed with 200 HTTP and has a Map of all section ATOM DOMs and list of sections.
		TestUnit baseTest = getDependency(BaseSectionFromRootXml.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}
		List<String> sections = ((BaseSectionFromRootXml)baseTest).getSectionList();
		if (sections.isEmpty()) {
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}
		final Context context = Loader.getInstance().getContext();
		String section = context.getString("documentSection");
		if (StringUtils.isBlank(section)) {
			// check pre-conditions and setup
			log.error("Failed to specify valid documentSection property in configuration");
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid documentSection property in configuration");
			return;
		}
		if (!sections.contains(section)) {
			// test pre-conditions
			setStatus(StatusEnumType.SKIPPED, "Failed to find section in test results");
			return;
		}
		HttpClient client = null;
		try {
			URI baseUrl = context.getBaseURL(section);
			if (log.isDebugEnabled()) {
				System.out.println("\nPUT URL: " + baseUrl);
			}
			client = context.getHttpClient();
			HttpPut request = new HttpPut(baseUrl);
			HttpResponse response = context.executeRequest(client, request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 405) {
				dumpResponse(request,  response, log.isDebugEnabled());
				setStatus(StatusEnumType.FAILED, "Expected 405 HTTP status code but was: " + code);
				return;
			}

			if (log.isDebugEnabled()) {
				dumpResponse(request,  response);
			}
			setStatus(StatusEnumType.SUCCESS);

		} catch (URISyntaxException e) {
			log.error("", e);
			setStatus(StatusEnumType.SKIPPED, "Failed to construct valid URI for section");
		} catch (IOException e) {
			throw new TestException(e);
		} finally {
			if (client != null)
				client.getConnectionManager().shutdown();
		}

	}

}
