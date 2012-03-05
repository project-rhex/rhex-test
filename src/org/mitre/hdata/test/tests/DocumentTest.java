package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.mitre.hdata.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test for documents
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
 * If no document of name documentname exists, the implementation MUST return a HTTP status code 404.
 *
 * Status Codes: 200, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentTest extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(DocumentTest.class);

	// regexp for mime-type (rfc2046); e.g. application/rss+xml, audio/L2, application/x-pkcs7-signature, etc.
	private static final Pattern mimePattern = Pattern.compile("[a-z]+/\\S+");

	public DocumentTest() {
		// forces source test to keep its Document objects after it executes
		setProperty(BaseSectionFromRootXml.class, BaseSectionFromRootXml.PROP_KEEP_SECTION_DOM_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.5.1.1";
	}

	@Override
	public boolean isRequired() {
		return true; // implied MUST
	}

	@NonNull
	public String getName() {
		return "GET operation returns a representation of the document that is identified by documentname within the section";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(BaseSectionFromRootXml.class); // 6.4.1.1
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseUrlOptions must have passed
		// with 200 HTTP doc
		TestUnit baseTest = getDependency(BaseSectionFromRootXml.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}

		Map<String, Document> docMap = ((BaseSectionFromRootXml)baseTest).getDocumentMap();
		if (docMap.isEmpty()) {
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}

		final Context context = Loader.getInstance().getContext();
		try {
			for (Document doc : docMap.values()) {
				checkFeed(context, doc);
			}
		} catch (URISyntaxException e) {
			throw new TestException(e);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (JDOMException e) {
			throw new TestException(e);
		}

		setStatus(StatusEnumType.SUCCESS);
	}

	private void checkFeed(Context context, Document doc) throws URISyntaxException, IOException, JDOMException, TestException {
		final Element root = doc.getRootElement();
		final Namespace ns = Namespace.getNamespace(NAMESPACE_W3_ATOM_2005);
		// System.out.println("check document feed " + root.getChildText("title", ns));

		/*
		expecting XML like this:

		 <?xml version="1.0" encoding="UTF-8"?>
		 <feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom">
		   <id>tag:hdata.herokuapp.com,2005:/records/1460/c32</id>
		   <link rel="alternate" type="text/html" href="https://hdata.herokuapp.com"/>
		   <link rel="self" type="application/atom+xml" href="https://hdata.herokuapp.com/records/1460/c32"/>
		   <title>C32</title>
		   <entry>
			 <id>tag:hdata.herokuapp.com,2005:Record/4ef38c7b00f4bab04c000032</id>
			 <updated>2011-12-22T20:24:32+00:00</updated>
			 <link rel="alternate" type="text/html" href="https://hdata.herokuapp.com/records/1460/c32/1460"/>
			 <title>1460</title>
		   </entry>
		 </feed>
		 */

		for(Object obj : root.getChildren("entry", ns)) {
			if (!(obj instanceof Element)) continue;
			Element e = (Element)obj;
			// allow multiple links per entry
			for(Object linkChild : e.getChildren("link", ns)) {
				if (!(linkChild instanceof Element)) continue;
				Element link = (Element)linkChild;
				final String href = link.getAttributeValue("href");
				if (StringUtils.isNotBlank(href)) {
					checkDocument(context, href, link.getAttributeValue("type"));
				}
			} // for each link
		} // for each entry
	}

	private void checkDocument(Context context, String href, String type) throws URISyntaxException, IOException {
		URI baseURL = new URI(href);
		System.out.println("\nXXX: document " + href + " type=" + type);

		String contentType = "application/json"; // TODO: hard-coded work-around for bug https://www.pivotaltracker.com/story/show/25948565
/*
		String contentType = getValidType(type);
		if (type != null && !type.equals(contentType))
			System.out.println("\tcontent type=" + contentType);
		if (contentType == null)
			contentType = "application/atom+xml, text/xml, application/xml, application/json, text/html, * / *";
		else contentType += ", application/json, * / *";
*/
//		else contentType = "application/json, " + contentType + ", */*";


		HttpClient client = context.getHttpClient();
		try {
			System.out.println("GET URL=" + baseURL);
			HttpGet req = new HttpGet(baseURL);
			req.setHeader("Accept", contentType);
			System.out.println("Accept=" + contentType);
			response = client.execute(req);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("Response status=" + code);
				/*
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
				*/
			}
			// TODO: what can we test about these document URLs ???
			// HTTP Status-Code 406: Not Acceptable.
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	private static String getValidType(String type) {
		if (type == null || type.length() == 0) return null;
		int ind = type.indexOf(';');
		if (ind == 0) return null;
		if (ind > 0) type = type.substring(0, ind);
		type = type.trim();
		return mimePattern.matcher(type).matches() ? type : null;
	}

	/*
	private boolean validateContent(int code, String path, Context context) throws TestException, IOException, JDOMException {
		if (code != 200) {
			setStatus(StatusEnumType.FAILED, "Unexpected HTTP response: " + code);
			return false;
		}
		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			// no body
			log.info("no BODY in response for section: " + path);
			return true;
		}
		final String contentType = ClientHelper.getContentType(entity);
		// content-type = text/xml OR application/xml
		if (!MIME_APPLICATION_ATOM_XML.equals(contentType)) {
			addWarning("Expected " + MIME_APPLICATION_ATOM_XML + " content-type for section but was: " + contentType);
		}
		long len = entity.getContentLength();
		// minimum length expected is 66 bytes or a negative number if unknown
		// assertTrue(len < 0 || len >= 66, "Expecting valid XML document for baseURL/root.xml; returned length was " + len);
		if (len > 49) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			entity.writeTo(bos);
			Document doc = getValidatingAtom(context, bos);
			// assertTrue(xmlErrors == 0, "Content has errors in ATOM feed"); // leave as warning for now
			final Element root = doc.getRootElement();
			assertEquals(NAMESPACE_W3_ATOM_2005, root.getNamespace().getURI());
			documentMap.put(path, doc);
		} else log.warn("section content length=" + len + ", expecting len > 0");
		return true;
	}
*/

}
