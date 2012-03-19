package org.mitre.hdata.test.tests;

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
import org.mitre.hdata.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Test for section ATOM feeds
 *
 * <pre>
 * 6.4 baseURL/sectionpath
 *
 * 6.4.1 GET
 *
 * This operation MUST return an Atom 1.0 compliant feed of all section documents and child sections contained in this
 * section. Each entry MUST contain a link to a resource that uniquely identifies the section document or child section.
 *
 * If the section document type defines a creation time, is RECOMMENDED to set the Created node to that datetime.
 * For section documents, the Atom Content element MUST contain the XML representation of its metadata (see Section 2.4.1).
 *
 * Status Code: 200
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseSectionFromRootXml extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(BaseSectionFromRootXml.class);

	private final List<String> sectionList = new ArrayList<String>();
	private final Map<String,Document> documentMap = new HashMap<String, Document>();

	private boolean keepSectionDocs;

	// property
	public static final String PROP_KEEP_SECTION_DOM_BOOL = "keepSectionDocs";

	public BaseSectionFromRootXml() {
		// forces BaseUrlRootXml test to keep its Document object after it executes
		setProperty(BaseUrlRootXml.class, BaseUrlRootXml.PROP_KEEP_DOCUMENT_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.4.1.1";
	}

	@NonNull
	public String getName() {
		return "GET operation MUST return an Atom 1.0 compliant feed of all section documents and child sections contained in this section";
	}

	@Override
	public boolean isRequired() {
		return true; // implied MUST
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.<Class<? extends TestUnit>> singletonList(BaseUrlRootXml.class); // 6.3.1
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite test BaseUrlOptions must have passed
		// with 200 HTTP response and a valid root.xml doc
		TestUnit baseTest = getDependency(BaseUrlRootXml.class);
		if (baseTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}

		Document doc = ((BaseUrlRootXml)baseTest).getDocument();
		if (doc == null) {
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}

		/*
			expecting:

		 <?xml version="1.0" encoding="UTF-8"?>
		 <root xmlns="http://projecthdata.org/hdata/schemas/2009/06/core" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			 ...
			<sections>
				<section path="c32" name="C32" extensionId="1"/>
				<section path="allergies" name="Allergies" extensionId="2"/>
				<section path="care_goals" name="Care Goals" extensionId="3"/>
				<section path="conditions" name="Conditions" extensionId="4"/>
				<section path="encounters" name="Encounters" extensionId="5"/>
				<section path="immunizations" name="Immunizations" extensionId="6"/>
				<section path="medical_equipment" name="Medical Equipment" extensionId="7"/>
				<section path="medications" name="Medications" extensionId="8"/>
				<section path="procedures" name="Procedures" extensionId="9"/>
				<section path="results" name="Lab Results" extensionId="10"/>
				<section path="social_history" name="Social History" extensionId="11"/>
				<section path="vital_signs" name="Vital Signs" extensionId="10"/>
			  </sections>
		 */

		final Namespace ns = Namespace.getNamespace(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE);
		Element sections = doc.getRootElement().getChild("sections", ns);
		if (sections == null) {
			log.warn("rootXML has no sections defined");
			setStatus(StatusEnumType.SKIPPED, "rootXML has no sections defined");
			return;
		}

		final Context context = Loader.getInstance().getContext();
		// TODO possibly move sections to Context for dependent tests for documents ??
		//final List<String> sectionList = context.getSectionList();
		try {
			for(Object child : sections.getChildren("section", ns)) {
				if (!(child instanceof Element)) continue;
				Element e = (Element)child;
				String path = e.getAttributeValue("path"); // required
				if (StringUtils.isNotBlank(path)) {
					System.out.println("XXX: path=" + path); // debug
					if (!checkSection(context, path)) {
						// test failed in last section
						return;
					}
					sectionList.add(path); // added validated sections to the list
				}
			}
		} catch (URISyntaxException e) {
			throw new TestException(e);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (JDOMException e) {
			throw new TestException(e);
		}

		if (sectionList.isEmpty()) {
			log.warn("rootXML has no sections defined");
			setStatus(StatusEnumType.SKIPPED, "rootXML has no sections defined");
		} else {
			setStatus(StatusEnumType.SUCCESS);
		}
	}

	private boolean checkSection(Context context, String path) throws URISyntaxException, IOException, JDOMException, TestException {
		URI baseURL = context.getBaseURL(path);
		HttpClient client = context.getHttpClient();
		try {
			System.out.println("\nGET URL=" + baseURL);
			HttpGet req = new HttpGet(baseURL);
			req.setHeader("Accept", "application/atom+xml, text/xml, application/xml");
			HttpResponse response = client.execute(req);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			return validateContent(code, path, context, response);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	private boolean validateContent(int code, String path, Context context, HttpResponse response)
			throws TestException, IOException, JDOMException {

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
			/*
			expecting:

			 <?xml version="1.0" encoding="UTF-8"?>
			 <feed xmlns="http://www.w3.org/2005/Atom">
			 ...
			 </feed>
			 */
			Document doc = getValidatingAtom(context, bos);
			// assertTrue(xmlErrors == 0, "Content has errors in ATOM feed"); // leave as warning for now
			final Element root = doc.getRootElement();
			assertEquals(NAMESPACE_W3_ATOM_2005, root.getNamespace().getURI());
			if (keepSectionDocs) {
				documentMap.put(path, doc);
			}
		} else log.warn("section content length=" + len + ", expecting len > 0");

		return true;
	}

	/**
	 * Set property on this test.
	 *
	 * @param key
	 * @param value
	 * @exception ClassCastException if target type does not match expected type typically indicated
	 * as ending of the property name constant (e.g. PROP_KEEP_SECTION_DOM_BOOL)
	 */
	public void setProperty(String key, Object value) {
		if (PROP_KEEP_SECTION_DOM_BOOL.equals(key))
			keepSectionDocs = (Boolean)value;
		else super.setProperty(key, value);
	}

	public void cleanup() {
		super.cleanup();
		if (!keepSectionDocs || getStatus() ==  TestUnit.StatusEnumType.FAILED) {
			documentMap.clear();
		}
	}

	@NonNull
	public Map<String, Document> getDocumentMap() {
		return documentMap;
	}

	public List<String> getSectionList() {
		return sectionList;
	}
}
