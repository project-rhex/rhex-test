package org.mitre.test;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;

/**
 * @author Jason Mathews, MITRE Corp.
 *
 * Date: 2/22/12 5:56 PM
 */
public abstract class BaseXmlTest extends BaseTest implements ErrorHandler {

	public static final Logger log = LoggerFactory.getLogger(BaseXmlTest.class);

	private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private SimpleDateFormat dateFormatter;

	private Document document;

	protected boolean keepDocument;

	// property
	public static final String PROP_KEEP_DOCUMENT_BOOL = "keepDocument";

	// private int xmlWarnings;
	protected int xmlErrors;
	protected boolean fatalXmlError;

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document doc) {
		this.document = doc;
	}

	public void warning(SAXParseException exception) throws SAXException {
		// xmlWarnings++;
		if (log.isDebugEnabled()) {
			log.debug("WARN: " + exception.getMessage());
		}
		// System.out.println("WARN: "  + ); // debug
	}

	public void error(SAXParseException exception) throws SAXException {
		xmlErrors++;
		final String s = exception.getMessage();
		addWarning(s);
		// System.out.println("ERROR: "  + s);
		log.debug("ERROR: " + s);
	}

	public void fatalError(SAXParseException exception) throws SAXException {
		xmlErrors++;
		fatalXmlError = true;
		final String s = exception.getMessage();
		addWarning(s);
		//System.out.println("ERROR: "  + s);
		log.error("ERROR: " + s);
	}

	protected SimpleDateFormat getDateFormatter() {
		if (dateFormatter == null) {
			dateFormatter = new SimpleDateFormat(ISO_DATE_FMT);
		}
		return dateFormatter;
	}

	protected Document getDefaultDocument(Context context, ByteArrayOutputStream bos)
			throws JDOMException, IOException
	{
		return context.getBuilder(this).build(new ByteArrayInputStream(bos.toByteArray()));
	}

	/**
	 * Get DOM from byte-array using validating parser if able to rewrite XML with target
	 * namespace and schema location otherwise builds DOM from a non-validating parser.
	 *
	 * @param context Context
	 * @param bos <code>ByteArrayOutputStream</code> to read from
	 * @param rootElement Expected root element in XML document with start tag '<' but no end tag (e.g. "<feed")
	 * @param namespaceUri Target namespace URI
	 * @param namespaceLocation Local location for XML Schema file
	 * @return <code>Document</code> resultant Document object
	 *
	 * @exception JDOMException when errors occur in parsing
	 * @exception IOException when an I/O error prevents a document
	 *         from being fully parsed.
	 */
	protected Document getValidatingParser(Context context, ByteArrayOutputStream bos, String rootElement,
										   String namespaceUri, String namespaceLocation)
			throws IOException, JDOMException
	{
		String content = bos.toString("UTF-8");
		if (log.isDebugEnabled()) {
			System.out.println("Content:\n" + content); // debug
		}
		int ind = content.indexOf(rootElement); // find starting position of the root element (e.g. "<feed")
		if (ind >= 0) {
			int endp = content.indexOf('>', ind + 5);
			if (endp > ind) {
				//boolean changed = false;
				// rewrite XML and add schema location to XML document to force schema validation against target ATOM XSD
				// insert xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.w3.org/2005/Atom file:/C:/xml/atom.xsd"
				//if (endp > ind) {
				// String xml = content.substring(ind, endp);
				// System.out.println();
				StringBuilder buf = new StringBuilder();
				if (ind > 0) buf.append(content.substring(0, ind));
				buf.append(rootElement);
				if (!content.substring(ind, endp).contains("xmlns:xsi=")) {
					// changed = true;
					buf.append(" xmlns:xsi=\"").append(NAMESPACE_W3_XMLSchema_INSTANCE).append("\"\n");
				}
				if (content.contains(namespaceUri)) {
					final File file = new File(namespaceLocation);
					if (file.exists()) {
						//changed = true;
						buf.append(" xsi:schemaLocation=\"").append(namespaceUri).append(' ').
								append(file.toURI().toASCIIString()).append("\"\n");
						buf.append(content.substring(ind + 5));
						content = buf.toString();
						// System.out.println(content);
						log.trace("Use validating XML parser");
						return context.getValidatingBuilder(this).build(new ByteArrayInputStream(content.getBytes("UTF-8")));
					}
				} // otherwise target namespace URI not found in document

				/*
				if (changed) {
					buf.append(content.substring(ind + 5));
					content = buf.toString();
					System.out.println(content);
					log.debug("Use validating XML parser");
					return context.getValidatingBuilder(this).build(new ByteArrayInputStream(content.getBytes("UTF-8")));
				//} else {
					// return context.getValidatingBuilder(this).build(new ByteArrayInputStream(bos.toByteArray()));
				}
				*/
			}
		}
		return getDefaultDocument(context, bos);
	}

	protected Document getValidatingAtom(Context context, ByteArrayOutputStream bos)
			throws IOException, JDOMException
	{
		return getValidatingParser(context, bos, "<feed", NAMESPACE_W3_ATOM_2005, "schemas/atom.xsd");
	}

	protected Document getSectionAtomDocument(Context context, String sectionPath) {
		try {
			URI baseUrl = context.getBaseURL(sectionPath);
			return getSectionAtomDocument(context, baseUrl);
		} catch (URISyntaxException e) {
			log.warn("", e);
			return null;
		}
	}

	protected Document getSectionAtomDocument(Context context, URI sectionPathUri) {
		final HttpClient client = context.getHttpClient();
		try {
			if (log.isDebugEnabled()) {
				System.out.println("\nSection URL: " + sectionPathUri);
			}
			HttpGet req = new HttpGet(sectionPathUri);
			req.setHeader("Accept", MIME_APPLICATION_XML); // "application/atom+xml, text/xml, application/xml");
			System.out.println("executing request: " + req.getRequestLine());
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			final HttpEntity entity = response.getEntity();
			if (log.isDebugEnabled()) {
				System.out.println("----------------------------------------");
				System.out.println("GET Response: " + response.getStatusLine());
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			if (code != 200) {
				log.warn("Expected 200 HTTP status code for section atom feed but was: " + code);
				return null;
			}
			if (entity == null) {
				log.warn("no BODY in response for section feed"); // no body
				return null;
			}
			final String contentType = ClientHelper.getContentType(entity);
			if (!MIME_APPLICATION_ATOM_XML.equals(contentType)) {
				addWarning("Expected " + MIME_APPLICATION_ATOM_XML + " content-type for section but was: " + contentType);
			}
			/*
			example section ATOM feed:

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
			return getDefaultDocument(context, bos);
		} catch (IOException e) {
			log.warn("", e);
		} catch (JDOMException e) {
			log.warn("", e);
		} finally {
			client.getConnectionManager().shutdown();
		}
		return null;
	}

	/**
	 * Set property on this test.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
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
			// if doc is non-null then status is SUCCESS
			// status = FAILED => doc = null
			document = null;
		}
	}

}