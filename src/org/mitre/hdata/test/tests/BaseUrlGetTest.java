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

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * 6.2.1 GET Operation on the Base URL
 *
 * The server MUST offer an Atom 1.0 compliant feed of all child sections specified in
 * HRF specification, as identified in corresponding sections node in the root document.
 *
 * Status Code: 200, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlGetTest extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(BaseUrlGetTest.class);
	private Document doc;

	private boolean keepDocument;

	// property
	public static final String PROP_KEEP_DOCUMENT_BOOL = "keepDocument";

	@NonNull
	public String getId() {
		return "6.2.1.4";
	}

	@Override
	public boolean isRequired() {
		return true; // MUST
	}

	@NonNull
	public String getName() {
		return "GET operation on baseURL MUST return Atom 1.0 feed of child sections if accept header is application/atom+xml";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList();
	}

	protected String getAcceptHeader() {
		return MIME_APPLICATION_ATOM_XML; // Header Accept: application/atom+xml
	}

	public void execute() throws TestException {
		// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			HttpGet req = new HttpGet(context.getBaseURL());
			String acceptHeader = getAcceptHeader();
			if (acceptHeader != null) {
				// System.out.println("set Accept=" + acceptHeader); // debug
				req.setHeader("Accept", acceptHeader);
			} else {
				req.removeHeaders("Accept");
			}
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
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	protected void validateContent(int code, Context context) throws TestException, IOException, JDOMException {
		if (code != 200) {
			setStatus(StatusEnumType.FAILED, "Unexpected HTTP response: " + code);
			return;
		}

		final HttpEntity entity = response.getEntity();
		final String contentType = ClientHelper.getContentType(entity);
		if (MIME_TEXT_HTML.equals(contentType)) {
			throw new TestException("Expected application/atom+xml content-type but was: text/html");
		}
		if (!MIME_APPLICATION_ATOM_XML.equals(contentType)) {
			addWarning("Expected application/atom+xml content-type but was: " + contentType);
		}
		// contentType value could be text/xml or application/xml and still be valid.
		// Return content-type is not explicitly mandated.

		// minimum length expected is 43 bytes or a negative number if unknown
		long len = entity.getContentLength();
		assertTrue(len < 0 || len >= 43, "Expecting valid ATOM XML document for baseURL; returned length was " + len);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		entity.writeTo(bos);
		/*
		expecting:

		 <?xml version="1.0" encoding="UTF-8"?>
		 <feed xmlns="http://www.w3.org/2005/Atom">
		 ...
		 </feed>
		 */
		Document doc = getValidatingAtom(context, bos);
		// System.out.println("XXX: xmlErrors=" + xmlErrors + " warnings=" + getWarnings().size());
		// assertTrue(xmlErrors == 0, "Content has errors in ATOM feed");
		assertFalse(fatalXmlError, "Atom XML has a fatal XML error");
		final Element root = doc.getRootElement();
		assertEquals(NAMESPACE_W3_ATOM_2005, root.getNamespace().getURI());

		// only keep copy of the DOM if the test was successful if which case dependent tests may access it
		this.doc = doc;

		setStatus(StatusEnumType.SUCCESS);
		/*
		 root.setAttribute("schemaLocation", NAMESPACE_W3_ATOM_2005 + " "
			 + "schema/atom.xsd", xsiNamespace);
		 */
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

	public void cleanup() {
		super.cleanup();
		if (!keepDocument) {
			doc = null;
		}
	}

	public Document getDocument() {
		return doc;
	}
}
