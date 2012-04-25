package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.mitre.hdata.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Test for section document creation
 *
 * <pre>
 *  6.4 baseURL/sectionpath
 *
 *  6.4.2.2 POST Add new document
 *
 * When adding a new section document, the request Content Type MUST be “multipart/form-data”
 * if including metadata. In this case, the content part MUST contain the section document.
 * The content part MUST include a Content-Disposition header with a disposition of “form-data”
 * and a name of “content.” The metadata part MUST contain the metadata for this section document.
 * The metadata part MUST include a Content-Disposition header with a disposition of “form-data”
 * and a name of “metadata.” It is to be treated as informational, since the service MUST compute
 * the valid new metadata based on the requirements found in the HRF specification. The content
 * media type MUST conform to the media type of either the section or the media type identified
 * by metadata of the section document. For XML media types, the document MUST also conform to
 * the XML schema identified by the extensionId for the section or the document metadata.
 *
 * If the content cannot be validated against the media type and the XML schema identified
 * by the content type of this section, the server MUST return a status code of 400.
 *
 * If the request is successful, the new section document MUST show up in the document
 * feed for the section. The server returns a 201 with a Location header containing
 * the URI of the new document.
 *
 * Status Code: 201, 400
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentCreateCheck extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(DocumentCreateCheck.class);

	@NonNull
	@Override
	public String getId() {
		return "6.4.2.3";
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@NonNull
	public String getName() {
		return "New document MUST show up in the ATOM feed for the section";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(DocumentCreate.class); // 6.4.2.2
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseUrlRootXml must have passed
		// with 200 HTTP response and valid root.xml content.
		TestUnit baseTest = getDependency(DocumentCreate.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test: BaseUrlRootXml");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test: 6.3.1.1");
			return;
		}
		final DocumentCreate documentCreate = (DocumentCreate) baseTest;
		URI documentURL = documentCreate.getDocumentURL();
		if (documentURL == null) {
			log.error("Failed to retrieve prerequisite test results: DocumentCreate");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results: 6.4.2.2");
			return;
		}
		/*
		expecting:

		 <?xml version="1.0" encoding="UTF-8"?>
		 <root xmlns="http://projecthdata.org/hdata/schemas/2009/06/core" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			<extensions>
				<extension extensionId="1">http://projecthdata.org/extension/c32</extension>
				<extension extensionId="2">http://projecthdata.org/hdata/schemas/2009/06/allergy</extension>
				...
			</extensions>
			<sections>
    			<section path="c32" name="C32" extensionId="1"/>
    			<section path="allergies" name="Allergies" extensionId="2"/>
    			...
			</sections>
		 </root>
		 */
		String sectionPath = documentCreate.getSectionPath();
		if (sectionPath == null) {
			log.error("Failed to retrieve prerequisite section from DocumentCreate");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results: 6.4.2.2");
			return;
		}
		System.out.println("section path: " + sectionPath);
		sendRequest(sectionPath, documentURL);
	}

	protected void sendRequest(String sectionPath, URI documentURL) throws TestException {
		final Context context = Loader.getInstance().getContext();
		final HttpClient client = context.getHttpClient();
		try {
			URI baseUrl = context.getBaseURL(sectionPath);
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + baseUrl);
			}
			HttpGet req = new HttpGet(baseUrl);
			req.setHeader("Accept", MIME_APPLICATION_XML); // "application/atom+xml, text/xml, application/xml");
			System.out.println("executing request: " + req.getRequestLine());
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			final HttpEntity entity = response.getEntity();
			if (log.isDebugEnabled()) {
				System.out.println("----------------------------------------");
				//System.out.println("GET Response status=" + code);
				System.out.println("GET Response: " + response.getStatusLine());
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
				if (entity != null) {
					System.out.println("----------------------------------------");
					System.out.println(EntityUtils.toString(entity));
				}
			}
			if (code != 200) {
				setStatus(StatusEnumType.FAILED, "Expected 200 HTTP status code but was: " + code);
				return;
			}
			validateContent(context, entity, documentURL);
			setStatus(StatusEnumType.SUCCESS);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (URISyntaxException e) {
			throw new TestException(e);
		} catch (JDOMException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	private void validateContent(Context context, HttpEntity entity, URI documentURL)
			throws TestException, JDOMException, IOException
	{
		if (entity == null) {
			// no body
			log.info("no BODY in response for section feed");
			throw new TestException("encountered non-body response to section request");
		}
		final String contentType = ClientHelper.getContentType(entity);
		if (!MIME_APPLICATION_ATOM_XML.equals(contentType)) {
			addWarning("Expected " + MIME_APPLICATION_ATOM_XML + " content-type for section but was: " + contentType);
		}
		/*
			verify document is now added to the section ATOM feed:

			<?xml version="1.0"?>
			<feed xmlns="http://www.w3.org/2005/Atom">
			  <id>1333458159</id>
			  <title>/vital_signs</title>
			  <generator version="1.0">atom feed generator</generator>
			  <entry>
				<id>1</id>
				<title>Systolic Blood Pressure</title>
				<updated>1292389200</updated>
				<link href="http://rhex.mitre.org:3000/records/1/vital_signs/4f735368d7d76a43b200001f" type="application/xml"/>
				<link href="http://rhex.mitre.org:3000/records/1/vital_signs/4f735368d7d76a43b200001f" type="application/json"/>
			  </entry>
			  ...
			 */
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		entity.writeTo(bos);
		Document doc = getDefaultDocument(context, bos);
		final Namespace atomNs = Namespace.getNamespace(NAMESPACE_W3_ATOM_2005);
		String targetUrl = documentURL.toASCIIString();
		for(Object feedChild : doc.getRootElement().getChildren("entry", atomNs)) {
			if (!(feedChild instanceof Element)) continue;
			Element entry = (Element)feedChild;
			for(Object entryChild : entry.getChildren("link", atomNs)) {
				if (!(entryChild instanceof Element)) continue;
				Element link = (Element)entryChild;
				String href = link.getAttributeValue("href"); // required
				if (targetUrl.equals(href)) {
					return;
				}
			}
		}
		throw new TestException("Failed to verify document appears in ATOM feed");
	}

}