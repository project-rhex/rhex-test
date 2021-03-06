package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Test for section path URL not found
 *
 * <pre>
 * 6.4 baseURL/sectionpath
 *
 * 6.4.1 GET
 *
 * This operation MUST return an Atom 1.0 compliant feed of all section documents and child sections contained in this
 * section. Each entry MUST contain a link to a resource that uniquely identifies the section document or child section.
 *
 * If the section document type defines a creation time, is RECOMMENDED to set the Created node to that datetime.
 * For section documents, the Atom Content element MUST contain the XML representation of its metadata (see Section 2.4.1).
 *
 * Status Code: 200, [404]
 *
 * Implied If baseURL does not exist or invalid sectionpath is specified then return 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 3/5/12 10:45 AM
 */
public class SectionNotFound extends BaseXmlTest {

	@NonNull
	public String getId() {
		return "6.4.1.2";
	}

	@Override
	public boolean isRequired() {
		return true; // implied MUST
	}

	@NonNull
	public String getName() {
		return "If baseURL does not exist or invalid sectionpath is specified then return 404 on GET operation";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		// return Collections.<Class<? extends TestUnit>> singletonList(BaseSectionFromRootXml.class); // 6.4.1.1
		return Collections.emptyList(); // none
	}

	public void execute() throws TestException {
		/*
		// with 200 HTTP doc
		/*
		TestUnit baseTest = getDependency(BaseSectionFromRootXml.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}

		List<String> sectionList = ((BaseSectionFromRootXml)baseTest).getSectionList();
		if (sectionList.isEmpty()) {
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}
		*/
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			URI baseURL = context.getBaseURL("notfound");
			// TODO: assumes section notfound does not exist
			HttpGet req = new HttpGet(baseURL);
			if (log.isDebugEnabled()) {
				System.out.println("GET URL: " + baseURL);
			}
			req.setHeader("Accept", "application/atom+xml");
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (code != 404 || log.isDebugEnabled()) {
				if (!log.isDebugEnabled()) {
					System.out.println("GET URL: " + baseURL);
				}
				dumpResponse(req, response);
			}
			assertEquals(404, code);
			// setResponse(response);
			setStatus(StatusEnumType.SUCCESS);
		} catch (URISyntaxException e) {
			throw new TestException(e);
		} catch (ClientProtocolException e) {
			throw new TestException(e);
		} catch (IOException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}
