package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Test for document PUT operation
 *
 * <pre>
 * 6.5 baseURL/sectionpath/documentname
 *
 * 6.5.2 PUT
 *
 * This operation is used to update a document by replacing it. The PUT operation
 * MUST NOT be used to create a new document; new documents MUST be created by
 * POSTing to the section. If the client attempts to create a new document this
 * way [via PUT], the server MUST return a 404.
 *
 * The content MUST conform to the media type identified by the document metadata
 * or the section content type. For media type application/xml, the document MUST
 * also conform to the XML schema that corresponds to the content type identified
 * by the document metadata or the section.
 *
 * If the parameter is incorrect or the content cannot be validated against
 * the correct media type or the XML schema identified by the content type
 * of this section, the server MUST return a status code of 400.
 *
 * If the request is successful, the new section document MUST show up in the
 * document feed for the section. The server returns a 200.
 *
 * Status Code: 200, 400, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentPut extends BaseTest {

	private static final Logger log = LoggerFactory.getLogger(DocumentPut.class);

	@NonNull
	public String getId() {
		return "6.5.2.3";
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@NonNull
	public String getName() {
		return "PUT operation to create a new document, server MUST return a 404";
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
		String documentSection = context.getString("document.section");
        if (StringUtils.isBlank(documentSection)) {
			// check pre-conditions and setup
            // e.g. documentSection=vital_signs
			log.error("Failed to specify valid document/section property in configuration");
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid document/section property in configuration");
			return;
		}
		if (!sections.contains(documentSection)) {
			// test pre-conditions
			setStatus(StatusEnumType.SKIPPED, "Failed to find section in test results");
			return;
		}
		HttpClient client = null;
		try {
			// create a documentname URL for an existing section that does not already exist
			String documentPath = Long.toHexString(System.currentTimeMillis());
    		URI baseUrl = context.getBaseURL(documentSection + "/" + documentPath);
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + baseUrl);
				System.out.println("documentPath=" + documentPath);
			}
			client = context.getHttpClient();
			HttpPut request = new HttpPut(baseUrl);

			// note: section 6.5.2 does not list the PUT form parameter for the request body
			// which is assumed to be of type “application/xwww-	form-urlencoded as defined
			// in 6.2.2 POST as Parameters:extensionID, path, name.
			// List<NameValuePair> formParams = new ArrayList<NameValuePair>(2);
			// TODO: what does a valid document look like ?? only have access to json presentation now ???
			// formParams.add(new BasicNameValuePair("body", ""));
			// formParams.add(new BasicNameValuePair("document", ""));
			// request.setEntity(new UrlEncodedFormEntity(formParams));

			File updateDocument = context.getPropertyAsFile("document.file");
			if (updateDocument != null) {
				log.debug("use file input: {}", updateDocument);
				request.setEntity(new FileEntity(updateDocument, MIME_APPLICATION_XML));
			} else {
				StringEntity entity = new StringEntity("plain text", HTTP.PLAIN_TEXT_TYPE, "UTF-8");
                // this should generate a 400 error
				request.setEntity(entity);
			}
			HttpResponse response = context.executeRequest(client, request);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("PUT Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			if (code != 404 && code != 400) {
				setStatus(StatusEnumType.FAILED, "Expected 400 or 404 HTTP status code but was: " + code);
				return;
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
