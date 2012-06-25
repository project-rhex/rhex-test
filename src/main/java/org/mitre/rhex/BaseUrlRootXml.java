package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import java.util.*;

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

	/**
	 * Mapping of extensions to section paths
	 */
	private final Map<String,String> extensionPathMap = new HashMap<String, String>();

	@NonNull
	public String getId() {
		return "6.3.1.1";
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

	public void execute() throws TestException {
		// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			URI baseURL = context.getBaseURL("root.xml");
			// System.out.println("XXX: resolve="+ baseURL.resolve("/root.xml"));
			HttpGet req = new HttpGet(baseURL);
			req.setHeader("Accept", "application/xml");
			// req.setHeader("If-Modified-Since", "Tue, 28 Feb 2012 14:33:15 GMT");
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + req.getURI());
				for(Header header : req.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			HttpResponse response = context.executeRequest(client, req);
			validateContent(context, response);
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

	protected void validateContent(Context context, HttpResponse response)
			throws TestException, IOException, JDOMException {

		int code = response.getStatusLine().getStatusCode();
		if (log.isDebugEnabled()) {
			System.out.println("Response status=" + code);
			for (Header header : response.getAllHeaders()) {
				System.out.println("\t" + header.getName() + ": " + header.getValue());
			}
		}
		if (code != 200) {
			setStatus(StatusEnumType.FAILED, "Unexpected HTTP response: " + code);
			return;
		}
		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			setStatus(StatusEnumType.FAILED, "Expect XML in body of response");
			return;
		}
		final String contentType = ClientHelper.getContentType(entity, false);
		// content-type = text/xml OR application/xml
		if (!MIME_TEXT_XML.equals(contentType) && !MIME_APPLICATION_XML.equals(contentType)) {
			addWarning("Expected supported xml content-type but was: " + contentType);
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
			   <extension extensionId="1">http://projecthdata.org/extension/c32</extension>
			   ...
			 </extensions>
			 <sections>
			 	<section path="c32" name="C32" extensionId="1"/>
			 	...
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

		checkRootDocument(root);

		// only keep copy of the DOM and response if the test was successful if which case dependent tests may access it
		setResponse(response);
		if (keepDocument) {
			setDocument(doc);
		}
		setStatus(StatusEnumType.SUCCESS);
	}

	private void checkRootDocument(Element root) {
		final Namespace ns = Namespace.getNamespace(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE);
		Element extensionsElt = root.getChild("extensions", ns);
		HashSet<String> extensions = new HashSet<String>();

		Map<String,String> idExtensionMap = new HashMap<String, String>();
		if (extensionsElt != null) {
			// verify HL7v3 HRF 1.0 - 2.2 bullet item #8: /hrf:extensions/hrf:extension/@extensionId (xs:string, 1)
			// This attribute contains a local identifier for the extension. It MUST be unique within the root document.
			/*
			<extensions>
				<extension extensionId="1">http://projecthdata.org/extension/c32</extension>
				...
		 	*/
			for(Object child : extensionsElt.getChildren("extension", ns)) {
				if (!(child instanceof Element)) continue;
				Element ext = (Element)child;
				String id = StringUtils.trimToNull(ext.getAttributeValue("extensionId")); // required
				if (id != null) {
					if (!extensions.add(id)) {
						addWarning("duplicate extensionId for " + id + " violates HL7 unique constraint");
					}
					String extension = StringUtils.trimToNull(ext.getText());
					if (extension != null) idExtensionMap.put(id, extension); // populate local mapping of ids to extensions
				}
			}
			log.trace("extensionIds={}", extensions);
		}
		if (extensions.isEmpty()) return; // no mappings possible
		// System.out.println("XXX: idExtensionMap=" + idExtensionMap); // debug

		Element sections = root.getChild("sections", ns);
		if (sections != null) {
			// verify HL7v3 HRF 1.0 - 2.2 bullet item #12: /htf:sections/hrf:section/@extensionId (xs:string, 1)
			// This identifier MUST be equal to the identifier of any of the registered extension elements,
			// as identified by the id attribute of the <extension> element.
			/*
			<sections>
    			<section path="c32" name="C32" extensionId="1"/>
    			...
			 */
			for(Object child : sections.getChildren("section", ns)) {
				if (!(child instanceof Element)) continue;
				Element section = (Element)child;
				String id = StringUtils.trimToNull(section.getAttributeValue("extensionId")); // required
                if (id == null || !extensions.contains(id)) {
					addWarning("section extensionId " + id + " violates HL7 constraint and must equal id in /hrf:extensions");
					break;
				} else {
					String extension = idExtensionMap.get(id);
                    if (extension == null) {
                        addWarning("section extensionId " + id + " violates HL7 constraint and must map to id in /hrf:extensions");
                    } else {
						String path = StringUtils.trimToNull(section.getAttributeValue("path"));
						if (path != null) extensionPathMap.put(extension, path);
					}
				}
			}
			log.trace("extensionPathMap={}", extensionPathMap);
		}
	}

	/**
	 * Get mapping of extensions to section paths
	 * @return Map
	 */
	@NonNull
	public Map<String, String> getExtensionPathMap() {
		return extensionPathMap;
	}
}
