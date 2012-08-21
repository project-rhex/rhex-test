package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Status Code: <B>200</B>, 400, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentPutPreTest extends BaseXmlTest {

	private URI baseURL;

	@NonNull
	public String getId() {
		return "6.5.2.0";
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@NonNull
	public String getName() {
		return "PUT operation to update document must have existing XML document defined";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList();
	}

	public void execute() throws TestException {
		final Context context = Loader.getInstance().getContext();
		final URI baseURL = context.getPropertyAsURI("updateDocumentUrl");
		if (baseURL == null) {
			// check pre-conditions and setup
			log.error("Failed to specify valid updateDocumentUrl property in configuration");
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid updateDocumentUrl property in configuration");
			return;
		}
		try {
			Document doc = getXmlDocument(context, baseURL);
			if (doc == null) {
				setStatus(StatusEnumType.FAILED, "Failed to get expected document");
				return;
			}
			this.baseURL = baseURL;
			if (keepDocument) setDocument(doc);
			setStatus(StatusEnumType.SUCCESS);
		} catch (IOException e) {
			System.out.println("URL=" + baseURL);
			throw new TestException(e);
		} catch (JDOMException e) {
			System.out.println("URL=" + baseURL);
			throw new TestException(e);
		}
	}

	public URI getBaseURL() {
		return baseURL;
	}
}
