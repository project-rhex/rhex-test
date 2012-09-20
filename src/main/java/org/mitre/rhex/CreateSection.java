package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test for section creation
 *
 * <pre>
 *  6.2 Operations on the Base URL
 *
 *  6.2.2 POST – Parameters:extensionID, path, name
 *
 *  This operation is used to create a new Section at the root of the document.
 *  The request body is of type “application/xwww-form-urlencoded” and MUST contain
 *  the extensionId, path, and name parameters. The extensionId parameter MAY be
 *  a string that is equal to value of one of the registered <extension> nodes of
 *  the root document of the HDR identified by baseURL. The path MUST be a string
 *  that can be used as a URL path segment. If any parameters are incorrect or
 *  not existent, the server MUST return a status code of 400.
 *
 *  The system MUST confirm that there is no other section registered as a
 *  child node that uses the same path name. If there is a collision, the
 *  server MUST return a status code of 409.
 *
 *  If the extensionId is not registered as a valid extension, the server
 *  <B>MUST</B> verify that it can support this extension. If it cannot support
 *  the extension it MUST return a status code of 406. It MAY provide
 *  additional entity information.
 *
 *  If it can support that extension, it <B>MUST</B> register it with the root.xml
 *  of this record. When creating the section resource, the server <B>MUST</B>
 *  update the root document: in the node of the parent section a new child node
 *  must be inserted. If successful, the server <B>MUST</B> return a 201 status
 *  code and SHOULD include the location of the new section.
 *
 *  The [optional] name parameter MUST be used as the user-friendly name for the
 *  new section.
 *
 * Status Code: <B>201</B>, 400, 406, 409
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * @see CreateSectionCodeCheck
 * Date: 2/20/12 10:45 AM
 */
public class CreateSection extends BaseXmlTest {

	private String sectionPath;

	public CreateSection() {
		// forces source test to keep its Document objects after it executes
		setProperty(BaseUrlRootXml.class, PROP_KEEP_DOCUMENT_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.2.2.1";
	}

	@Override
	public boolean isRequired() {
		return true; // implied MUST
	}

	@NonNull
	public String getName() {
		return "POST operation on baseURL creates a new Section at the root of the document";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(BaseUrlRootXml.class); // 6.3.1.1
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseUrlRootXml must have passed
		// with 200 HTTP response and valid root.xml content.
		TestUnit baseTest = getDependency(BaseUrlRootXml.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test: BaseUrlRootXml");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test: 6.3.1.1");
			return;
		}
		Document doc = ((BaseUrlRootXml)baseTest).getDocument();
		if (doc == null) {
			log.error("Failed to retrieve prerequisite test results: BaseUrlRootXml");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results: 6.3.1.1");
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
				<extension extensionId="11">http://projecthdata.org/hdata/schemas/2009/06/social_history</extension>
			</extensions>
			...
		 </root>
		 */
		final Namespace ns = Namespace.getNamespace(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE);
		Element extensionsElt = doc.getRootElement().getChild("extensions", ns);
		String extensionId = null;
		if (extensionsElt != null) {
			for(Object child : extensionsElt.getChildren("extension", ns)) {
				if (!(child instanceof Element)) continue;
				Element ext = (Element)child;
				String id = StringUtils.trimToNull(ext.getAttributeValue("extensionId")); // required
				if (id != null) {
					extensionId = id;
					// prefer non-c32 section if available which may have special handling
					// PDS only allows one c32 per patient
					String section = ext.getText();
					// log.debug("XXX: id={} section={}", id, section);
					if (section == null || !section.contains("/c32")) break;
				}
			}
		}
		if (extensionId == null) {
			log.error("Failed to retrieve extensionId from prerequisite test results: BaseUrlRootXml");
			setStatus(StatusEnumType.SKIPPED, "Failed to find extensionId in prerequisite test results: 6.3.1.1");
			return;
		}

		final Context context = Loader.getInstance().getContext();
		final HttpClient client = context.getHttpClient();
		try {
			sectionPath = Long.toHexString(System.currentTimeMillis());
			URI baseUrl = context.getBaseURL();
			if (log.isDebugEnabled()) {
				System.out.println("URL: " + baseUrl);
				System.out.println("path=" + sectionPath);
				System.out.println("extensionId=" + extensionId);
			}
			HttpPost post = new HttpPost(baseUrl);
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(3);
			formParams.add(new BasicNameValuePair("path", sectionPath));
			formParams.add(new BasicNameValuePair("name", sectionPath)); // optional
			formParams.add(new BasicNameValuePair("extensionId", extensionId));
			post.setEntity(new UrlEncodedFormEntity(formParams));
			HttpResponse response = context.executeRequest(client, post);
			int code = response.getStatusLine().getStatusCode();
			if (code != 201 || log.isDebugEnabled()) {
				dumpResponse(post, response);
			}
			if (code >= 400) {
				if (!log.isDebugEnabled()) {
					System.out.println("URL: " + baseUrl);
					System.out.println("path=" + sectionPath);
					System.out.println("extensionId=" + extensionId);
				}
				// request failed entirely
				setStatus(StatusEnumType.FAILED, "Expected 201 HTTP status code but was: " + code);
				return;
			}
			// NOTE: response status code 201 checked in another test. See CreateSectionCodeCheck.
			// here we're going to verify if the new section was actually created
			checkRootXML(context);
			// TODO: check if baseURL/section returns 200 status code with skeleton atom feed
			StatusEnumType status = getStatus();
			if (status == StatusEnumType.SUCCESS) {
				setResponse(response); // save response
			} else if (status == null) {
				setStatus(StatusEnumType.FAILED, "Failed to verify if new section path was created in root.xml");
			}
			/*
			if (code != 201) {
				// wrong status code but operation may have created the section
				if (status != null) { // status == StatusEnumType.SUCCESS || status == StatusEnumType.FAILED) {
					// TODO: verify for non-201 status code does this test pass with warning or fail ??
					addWarning("Expected 201 HTTP status code but was: " + code);
				} else
					setStatus(StatusEnumType.FAILED, "Expected 201 HTTP status code but was: " + code);
			} else if (status == null) { // status != StatusEnumType.SUCCESS) {
				// right return status code but unable to verify root.xml is updated
				setStatus(StatusEnumType.FAILED, "Failed to verify if new section path was created in root.xml");
			}
			// Document doc = BaseUrlRootXml.loadDocument(baseUrl);
			*/
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

	private void checkRootXML(Context context) throws URISyntaxException, IOException, JDOMException {
		URI baseURL = context.getBaseURL("root.xml");
		HttpGet req = new HttpGet(baseURL);
		req.setHeader("Accept", "text/xml");
		if (log.isDebugEnabled()) {
			System.out.println("\nGET URL: " + req.getURI());
		}
		final HttpClient client = context.getHttpClient();
		try {
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			if (code != 200) {
				addWarning("Unexpected HTTP response: " + code);
				return;
			}
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				addWarning("Expected XML in body of GET response");
				return;
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			entity.writeTo(bos);
			if (log.isDebugEnabled()) {
				System.out.println("Content:\n" + bos.toString("UTF-8"));
			}
			final Document doc = getDefaultDocument(context, bos);
			final Element root = doc.getRootElement();
			// assertEquals(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE, root.getNamespace().getURI());
			final Namespace ns = Namespace.getNamespace(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE);
			Element sections = root.getChild("sections", ns);
			if (sections != null) {
				int sectionCount = 0;
				for(Object child : sections.getChildren("section", ns)) {
					if (!(child instanceof Element)) continue;
					sectionCount++;
					Element section = (Element)child;
					if (sectionPath.equals(section.getAttributeValue("path"))) {
						setStatus(StatusEnumType.SUCCESS);
						System.out.println("XXX: found section new " +section + " in ATOM feed"); // debug
						return; // section path found and test passed
					}
				}
				if (sectionCount > 0) {
					setStatus(StatusEnumType.FAILED, "Failed to update root.xml with new section path");
				}
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public String getSectionPath() {
		return sectionPath;
	}
}