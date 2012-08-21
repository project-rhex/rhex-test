package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Test for document path URL not found
 *
 * <pre>
 * 6.5 baseURL/sectionpath/documentname
 *
 * 6.5.1 GET
 *
 * This operation returns a representation of the document that is identified by documentname within the
 * section identified by sectionpath. The documentname is typically assigned by the underlying system
 * and is not guaranteed to be identical across two different systems.
 *
 * Implementations MAY use identifiers contained within the infoset of the document as documentnames.
 *
 * If no document of name documentname exists, the implementation *MUST* return a HTTP status code 404.
 *
 * Status Codes: 200, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 3/5/12 10:45 AM
 */
public class DocumentNotFound extends BaseTest {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@NonNull
	public String getId() {
		return "6.5.1.3";
	}

	@Override
	public boolean isRequired() {
		return true; // implied MUST
	}

	@NonNull
	public String getName() {
		return "GET baseURL/sectionpath/documentname operation. If no document of name documentname exists, the implementation MUST return a HTTP status code 404";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(BaseSectionFromRootXml.class); // 6.4.1.1
	}

	public void execute() throws TestException {
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

		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			String section = getTargetSection(sectionList);
			URI baseURL = context.getBaseURL(section);
			baseURL = new URI(baseURL.toASCIIString() + "/should_not_exist");
			HttpRequestBase req = createRequest(baseURL);
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + req.getURI());
			}
			HttpResponse response = context.executeRequest(client, req);
			/*
			//int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				dumpResponse(req, response, true);
				/*
				System.out.println("Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
				final HttpEntity entity = response.getEntity();
				if (entity != null) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					entity.writeTo(bos);
                    if (bos.size() > 0) {
					    System.out.println("XXX: body\n" + bos.toString("UTF-8"));
                    }
				}
			}
			*/
			validateResponse(req, response);
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

	protected void validateResponse(HttpRequestBase req, HttpResponse response) throws TestException {
		int code = response.getStatusLine().getStatusCode();
		if (code != 404 || log.isDebugEnabled()) {
			dumpResponse(req, response, true);
		}
		assertEquals(404, code);
	}

	protected HttpRequestBase createRequest(URI baseURL) {
		HttpGet req = new HttpGet(baseURL);
		req.setHeader("Accept", "application/json, application/xml");
		return req;
	}

	private static String getTargetSection(List<String> sectionList) {
        if (sectionList.isEmpty()) {
            // should never have an empty list
            return "medications";
        }
        // possible sections: c32, allergies, care_goals, conditions, encounters, immunizations, medical_equipment,
        // medications, procedures, results, social_history, vital_signs, etc.
        String[] sections = { "medications", "allergies", "results", "conditions", "encounters", "immunizations" };
        for(String section : sections) {
            if (sectionList.contains(section)) return section;
        }
        String section = "medications";
        for (String aSection : sectionList) {
            section = aSection;
            // try to get non-c32 section which may have special handling and not support json media type
            if (!"c32".equals(section)) break; // done
        }
        return section;
    }

}
