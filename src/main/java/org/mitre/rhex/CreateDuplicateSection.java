package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
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
 *  server <B>MUST</B> return a status code of 409.
 *
 * Status Code: 201, 400, 406, <B>409</B>
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class CreateDuplicateSection extends BaseXmlTest {

	private String sectionPath;
	private String extensionId;

	public CreateDuplicateSection() {
		setProperty(BaseUrlRootXml.class, PROP_KEEP_DOCUMENT_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.2.2.3";
	}

	@Override
	public boolean isRequired() {
		// If there is a collision, the server *MUST* return a status code of 409
		return true;
	}

	@NonNull
	public String getName() {
		return "If there is a collision in section creation, the server MUST return a status code of 409";
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
		String name = null;
		if (extensionsElt != null) {
			for(Object child : extensionsElt.getChildren("extension", ns)) {
				if (!(child instanceof Element)) continue;
				Element ext = (Element)child;
				String id = StringUtils.trimToNull(ext.getAttributeValue("extensionId")); // required
				if (id == null) continue;
				String section = ext.getText();
				if (StringUtils.isNotBlank(section)) {
					// find first section with id and path values
					extensionId = id;
					sectionPath = section;
					name = ext.getText();
					break;
				}
			}
		}
		if (sectionPath == null) {
			log.error("Failed to retrieve section from prerequisite test results: BaseUrlRootXml");
			setStatus(StatusEnumType.SKIPPED, "Failed to find section in prerequisite test results: 6.3.1.1");
			return;
		}

		final Context context = Loader.getInstance().getContext();
		final HttpClient client = context.getHttpClient();
		try {
			URI baseUrl = context.getBaseURL();
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + baseUrl);
				System.out.println("section path=" + sectionPath);
			}
			HttpPost post = new HttpPost(baseUrl);
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(3);
			formParams.add(new BasicNameValuePair("path", sectionPath));
			if (StringUtils.isNotBlank(name)) {
				formParams.add(new BasicNameValuePair("name", name)); // optional
			}
			formParams.add(new BasicNameValuePair("extensionID", extensionId));
			post.setEntity(new UrlEncodedFormEntity(formParams));
			HttpResponse response = context.executeRequest(client, post);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("POST Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			if (code != 409) {
				dumpResponse(post, response, log.isDebugEnabled());
				setStatus(StatusEnumType.FAILED, "Expected 409 HTTP status code but was: " + code);
			} else {
				setStatus(StatusEnumType.SUCCESS);
			}
		} catch (IOException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}