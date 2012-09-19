package org.mitre.rhex;

import com.google.gson.*;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.mitre.test.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Test for documents as discovered via section atom feeds
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
 * Status Codes: <B>200</B>, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentTest extends BaseXmlTest {

	private final boolean debugEnabled = log.isDebugEnabled();
    private final boolean traceEnabled = log.isTraceEnabled();

	// regexp for mime-type (rfc2046); e.g. application/rss+xml, audio/L2, application/x-pkcs7-signature, etc.
	private static final Pattern mimePattern = Pattern.compile("[a-z]+/\\S+");

	private JsonParser parser;

	private int count;

	public DocumentTest() {
		// forces source test to keep its Document objects after it executes
		setProperty(BaseSectionFromRootXml.class, BaseSectionFromRootXml.PROP_KEEP_SECTION_DOM_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.5.1.2";
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
		// pre-conditions: for this test to be executed the prerequisite test BaseSectionFromRootXml must have passed
		// with 200 HTTP and has Map of all DOMs
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

	private void checkDocument(Context context, String href, String type) throws URISyntaxException, IOException, TestException {
		URI baseURL = new URI(href);
		if (!baseURL.isAbsolute()) {
			// REVIEW: is this legal wrt HL7 spec
			baseURL = context.getBaseURL().resolve(baseURL);
			log.trace("relative URL {} -> {}", href, baseURL);
		}
		String contentType = getValidType(type);
		if (debugEnabled && type != null && !type.equals(contentType)) {
			System.out.println("\tcontent type=" + contentType);
		}
		if ("localhost".equalsIgnoreCase(baseURL.getHost())) {
			addLogWarning("URL cannot be localhost");
			return;
		}
		if (contentType == null) {
			if (log.isDebugEnabled()) {
				if (type == null) log.debug("null content type");
				else log.debug("null content type from type=" + type);
			}
			contentType = "application/xml,text/xml,application/json,text/html;q=0.9,*/*;q=0.8";
		} else {
			// NOTE: implementation with Ruby on Rails does not handle the */* or quality weights on Accept header correctly
			// so for example: Accept => application/xml,*/*;q=0.8 returns HTML document even when an XML representation exists.
			// Use multiple formats in accept header for 25% of the documents.
			// if (contentType.contains("/xml")) return; // skip XML for now
			if (++count % 4 == 0) {
				// if add multiple mime types then get HTML output
				if (MIME_APPLICATION_JSON.equals(contentType))
					contentType += ",application/xml;q=0.9";
				else
					contentType += ",application/json;q=0.9";
				// else contentType += ",*/*;q=0.8"; // responds 406 status code
			}
		}

		HttpClient client = context.getHttpClient();
		try {
            if (debugEnabled) System.out.println("GET URL=" + baseURL);
			//System.out.println("GET URL=" + baseURL);
			HttpGet req = new HttpGet(baseURL);
			req.setHeader("Accept", contentType);
            if (debugEnabled) System.out.println("Accept=" + contentType);
			validateContent(baseURL, req, context.executeRequest(client, req), context, contentType);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	private void validateContent(URI baseURL, HttpGet req, HttpResponse response,
								 Context context, String requestMediaType)
			throws IOException, TestException
	{
		// TODO: what can we test about these document URLs - does any error in document fail overall conformance for the spec requirement
		int code = response.getStatusLine().getStatusCode();
		if (debugEnabled) {
			System.out.println("Response status=" + code);
		}
		if (code == 406) {
			if (addLogWarning("Failed to retrieve content for requested media type")) {
				dumpResponse(req, response, true);
			}
		} else if (code != 200) {
			dumpResponse(req, response, true);
			throw new TestException("Unexpected HTTP response: " + code);
		}
		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			// no body
			addWarning("encountered non-body response to document request");
			log.info("no BODY in response for document: " + baseURL.getPath());
			return;
		}
		long len = entity.getContentLength();
		if (len <= 0) {
			log.warn("section content length=" + len + ", expecting len > 0");
			return;
		}
		final String contentType = ClientHelper.getContentType(entity);
		if (contentType == null) {
			if (addLogWarning("Failed to determine content type") && debugEnabled) {
				dumpResponse(req, response, true);
			}
			return;
		}
		int ind = requestMediaType.indexOf(',');
		if (ind > 0) requestMediaType = requestMediaType.substring(0, ind);
		ind = requestMediaType.indexOf(';'); // strip off any parameter (e.g. ;q=0.9)
		if (ind > 0) requestMediaType = requestMediaType.substring(0, ind);
		if (!requestMediaType.equalsIgnoreCase(contentType)) {
			addLogWarning(String.format("Returned content type: %s does not match requested type: %s",
					requestMediaType ,contentType));
		}
		if (ClientHelper.isXmlContentType(contentType)) {
			// content-type = application/atom+xml OR text/xml OR application/xml
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			entity.writeTo(bos);
			try {
				getDefaultDocument(context, bos);
			} catch (JDOMException e) {
				addWarning(e.getMessage());
				log.warn("", e);
			}
		} else if (contentType.equals(MIME_APPLICATION_JSON)) {
			if (len > 1) {
				try {
					String json = EntityUtils.toString(entity);
					if (StringUtils.isBlank(json)) {
						addLogWarning("Empty json document");
					} else {
						// e.g. {"codes":{"SNOMED-CT":["84114007"],"ICD-9-CM":["428.9"],"ICD-10-CM":["I50.9"]},...
						try {
							if (parser == null) parser = new JsonParser();
							parser.parse(json);
						} catch(JsonSyntaxException e) {
							log.warn("", e);
						}
					}
				} catch(Exception e) {
					log.debug("", e);
				}
			} else {
				// 1-byte json documents are bogus
				addLogWarning("Empty json document");
			}
		} // else System.out.println("XXX: other type="+ contentType);
		// if not XML/JSON then do nothing for now
	}

	private static String getValidType(String type) {
		if (type == null || type.length() == 0) return null;
		int ind = type.indexOf(';');
		if (ind == 0) return null;
		if (ind > 0) {
            // strip off parameter values (e.g. charset=...) from mime type if present
            type = type.substring(0, ind);
        }
		type = type.trim();
        // regexp for mime-type (rfc2046) /^[a-z]+/\\S+/ matching mime type (e.g. application/rss+xml)
		return mimePattern.matcher(type).matches() ? type : null;
	}

}
