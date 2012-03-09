package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
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
 * <pre>
 * 6.3 baseURL/root.xml
 *
 * 6.3.1 GET
 *
 * This operation [MUST] return an XML representation of the current root document,
 * as defined by the HRF specification.
 *
 * Status Code: 200
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlRootXml extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(BaseUrlRootXml.class);
	private Document doc;
	private boolean keepDocument;

	// property
	public static final String PROP_KEEP_DOCUMENT_BOOL = "keepDocument";

	@NonNull
	public String getId() {
		return "6.3.1";
	}

	@Override
	public boolean isRequired() {
		return true; // implied MUST
	}

	@NonNull
	public String getName() {
		return "GET operation on baseURL/root.xml MUST return XML object with 200 status code";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList(); // none
	}

	public Document getDocument() {
		return doc;
	}

	/**
	 * Set property on this test.
	 *
	 * @param key
	 * @param value
	 * @exception ClassCastException if target type does not match expected type typically indicated
	 * as ending of the property name constant (e.g. PROP_KEEP_DOCUMENT_BOOL)
	 */
	public void setProperty(String key, Object value) {
		// System.out.printf("XXX: setProperty %s: %s%n", key, value);
		if (PROP_KEEP_DOCUMENT_BOOL.equals(key))
			keepDocument = (Boolean)value;
		else super.setProperty(key, value);
	}

	public void execute() throws TestException {
		// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			URI baseURL = context.getBaseURL("root.xml");
			// System.out.println("XXX: resolve="+ baseURL.resolve("/root.xml"));
			HttpGet req = new HttpGet(baseURL);
			req.setHeader("Accept", "text/xml");
			// req.setHeader("If-Modified-Since", "Tue, 28 Feb 2012 14:33:15 GMT");
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + req.getURI());
				for(Header header : req.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			response = client.execute(req);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			validateContent(code, context);
		} catch (JDOMException e) {
			throw new TestException(e);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (URISyntaxException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
		// System.out.println();
	}

	protected void validateContent(int code, Context context) throws TestException, IOException, JDOMException {
		if (code != 200) {
			setStatus(StatusEnumType.FAILED, "Unexpected HTTP response: " + code);
			return;
		}
		final HttpEntity entity = response.getEntity();
		final String contentType = ClientHelper.getContentType(entity, false);
		// content-type = text/xml OR application/xml
		if (!MIME_TEXT_XML.equals(contentType) && !MIME_APPLICATION_XML.equals(contentType)) {
			addWarning("Expected text/xml content-type but was: " + contentType);
		}
		if (entity == null) {
			setStatus(StatusEnumType.FAILED, "Expect XML in body of response");
			return;
		}
		long len = entity.getContentLength();
		// minimum length expected is 66 bytes or a negative number if unknown
		assertTrue(len < 0 || len >= 66, "Expecting valid XML document for baseURL/root.xml; returned length was " + len);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		entity.writeTo(bos);

		/*
		expecting:

		 <?xml version="1.0" encoding="UTF-8"?>
		 <root xmlns="http://projecthdata.org/hdata/schemas/2009/06/core" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			 ...
			 <extensions>
			 </extensions>
			 <sections>
			 </sections>
		 </root>
		 */

		Document doc = getValidatingParser(context, bos, "<root",
				NAMESPACE_HDATA_SCHEMAS_2009_06_CORE, "schemas/root.xsd");
		// System.out.println("XXX: xmlErrors=" + xmlErrors + " warnings=" + getWarnings().size());
		// TODO: if xmlErrors != 0 then have warnings added to this test so are XML errors warnings or failed assertion ??
		// assertTrue(xmlErrors == 0, "Content has errors in XML feed");
		assertFalse(fatalXmlError, "XML has a fatal XML error");
		final Element root = doc.getRootElement();
		assertEquals(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE, root.getNamespace().getURI());

		// only keep copy of the DOM if the test was successful if which case dependent tests may access it
		this.doc = doc;

		setStatus(StatusEnumType.SUCCESS);
	}

	public void cleanup() {
		super.cleanup();
		if (!keepDocument) {
			doc = null;
		}
	}

}
