package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

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
 * If the request is successful, the [updated] section document MUST show up in the
 * document feed for the section. The server returns a 200.
 *
 * Status Code: 200, [400], 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentPutBadContent extends DocumentUpdate {

	@NonNull
	public String getId() {
		return "6.5.2.5";
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@NonNull
	public String getName() {
		return "If the PUT parameter is incorrect or the content cannot be validated, server MUST return a 400";
	}

	protected String getContent(DocumentPutPreTest baseTest)
			throws IOException, JDOMException
	{
		// bogus XML document should generate 400 errors
		return "<bogus></bogus>";
	}

	/*
	protected HttpPut createRequest(URI baseURL, String xmlContent) {
		HttpPut request = new HttpPut(baseURL);
		StringEntity entity = new StringEntity("plain text", ContentType.TEXT_PLAIN);
		// StringEntity entity = new StringEntity(xmlContent, ContentType.APPLICATION_XML);
		request.setEntity(entity);
		return request;
	}
	*/

	protected boolean validateContent(Context context, HttpPut request, HttpResponse response, DocumentPutPreTest baseTest, URI baseURL)
			throws JDOMException, IOException, TestException
	{
		int code = response.getStatusLine().getStatusCode();
		if (code != 400 || log.isDebugEnabled()) {
			dumpResponse(request, response, code == 200);
			if (code == 200) verifyContents(context, baseTest, baseURL);
		}

		// check return code
		assertEquals(400, code);

		return true;
	}

	private void verifyContents(Context context, DocumentPutPreTest baseTest, URI baseURL) {
		try {
			// verify document is unchanged since update should fail in first place
			Document doc = getXmlDocument(context, baseURL);
			if (doc == null) {
				addLogWarning("Failed to retrieve document");
				return;
			}
			Element rootElement = doc.getRootElement();
			if (rootElement == null) {
				addLogWarning("Retrieve document has empty root element");
				return;
			}
			String rootElementName = rootElement.getName();
			if ("bogus".equals(rootElementName)) {
				addLogWarning("Server updated with invalid document contents");
			} else {
				// compare against original XML document
				Document baseDoc = baseTest.getDocument();
				if (baseDoc == null) {
					log.debug("Cannot verify update");
					return; // cannot compare
				}
				Element baseRootElement = doc.getRootElement();
				if (baseRootElement == null) {
					log.debug("Original document has empty root element. Cannot verify update");
					return;
				}
				if (rootElementName.equals(baseRootElement.getName()))
					log.debug("document root element unchanged"); // this is expected result
				else
					addLogWarning(String.format("Updated document root element %s does match expected element: %s",
							rootElementName, baseRootElement.getName()));
				Namespace ns = baseRootElement.getNamespace();
				if (ns != null && !ns.equals(rootElement.getNamespace())) {
					addLogWarning(String.format("Updated document namespace does not match: expected %s, actual=%s",
							ns, rootElement.getNamespace()));
				}
			}
		} catch (IOException e) {
			log.warn("Failed to verify document", e);
		} catch (JDOMException e) {
			log.warn("Failed to parse updated document", e);
		}
	}

}
