package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Test for cross-check of BaseURL Root.xml section elements with same info
 * published as ATOM feed. Implied/recommended behavior not defined in the spec.
 *
 * <pre>
 * 6.2.1 GET Operation on the Base URL
 *
 * The server <B>MUST</B> offer an Atom 1.0 compliant feed of all child sections specified in
 * HRF specification, as identified in corresponding sections node in the root document.
 *
 * 6.3.1 GET Operation on baseURL/root.xml
 *
 * This operation [MUST] return an XML representation of the current root document,
 * as defined by the HRF specification.
 *
 * This test checks that the Atom compliant feed [6.2.1] is equivalent to that of the
 * XML representation of the Root XML [6.3.1] minimally that all the section paths
 * in the Root XML are contained in the Atom feed as links.
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class RootXmlAtomCheck extends BaseTest {

	public RootXmlAtomCheck() {
		// forces BaseUrlRootXml + BaseUrlGetTest tests to keep its Document object after they execute
		setProperty(BaseUrlRootXml.class, BaseUrlRootXml.PROP_KEEP_DOCUMENT_BOOL, Boolean.TRUE);
		setProperty(BaseUrlGetTest.class, BaseUrlGetTest.PROP_KEEP_DOCUMENT_BOOL, Boolean.TRUE);
	}

	@NonNull
	public String getId() {
		return "6.2.1.6";
	}

	@NonNull
	public String getName() {
		return "BaseUrl ATOM feed should match section details in the equivalent root.xml feed";
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		List<Class<? extends TestUnit>> depends = new ArrayList<Class<? extends TestUnit>>(2);
		depends.add(BaseUrlGetTest.class); // 6.2.1.4
		depends.add(BaseUrlRootXml.class); // 6.3.1.1
		return depends;
	}

	public void execute() throws TestException {
		// pre-conditions: for this test to be executed the prerequisite tests
		// BaseUrlGetTest and  BaseUrlRootXml must have both passed with 200 HTTP response and valid content.
		TestUnit baseUrlRootXmlTest = getDependency(BaseUrlRootXml.class);
		TestUnit baseUrlGetTest = getDependency(BaseUrlGetTest.class);
		if (baseUrlRootXmlTest == null || baseUrlGetTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
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
		Document rootDoc = ((BaseUrlRootXml)baseUrlRootXmlTest).getDocument();
		if (rootDoc == null) {
			log.error("Failed to retrieve prerequisite test " + baseUrlRootXmlTest.getId());
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}

		Document atomDoc = ((BaseUrlGetTest)baseUrlGetTest).getDocument();
		if (atomDoc == null) {
			log.error("Failed to retrieve prerequisite test " + baseUrlGetTest.getId());
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results");
			return;
		}

		final Namespace ns = Namespace.getNamespace(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE);
		Element sectionsElt = rootDoc.getRootElement().getChild("sections", ns);
		if (sectionsElt == null) {
			log.warn("rootXML has no sections defined");
			setStatus(StatusEnumType.SKIPPED, "rootXML has no sections defined");
			return;
		}

		final Context context = Loader.getInstance().getContext();
		final URI baseURI = context.getBaseURL(); // e.g. https://hdata.server.com/records/1547
		String baseUrl = baseURI.toASCIIString();
		// if (!baseUrl.endsWith("/")) baseUrl += '/'; // e.g. https://hdata.server.com/records/1547/
		// Note: if there are non-ASCII characters in the baseURL then the URL comparison below might not match
		log.debug("using baseUrl={}", baseUrl);

		// Map<String,String> sections = new HashMap<String, String>();
		Set<String> sectionPaths = new HashSet<String>();
		for(Object child : sectionsElt.getChildren("section", ns)) {
			if (!(child instanceof Element)) continue;
			Element section = (Element)child;
			String path = section.getAttributeValue("path"); // required
			if (StringUtils.isBlank(path)) continue;
			// also extensionId [required]
			if (path.startsWith("/")) {
				// this should be relative to baseURL
				if ("/".equals(path)) {
					addWarning("Invalid section path: " + path);
					continue;
				}
				addWarning("section path " + path + " should not start with '/'");
				path = path.substring(1);
			}
			if (!sectionPaths.add(path)) {
			 	addWarning("Duplicate section path: "  + path);
			}
			/*
			String name = section.getAttributeValue("name"); // optional
			if (StringUtils.isNotBlank(name)) {
				sections.put(name, path);
			}
			*/
		}

		final boolean debug = log.isDebugEnabled();
		if (debug) log.debug("root.xml section paths=" + sectionPaths);

		/*
		expecting:

		 <?xml version="1.0" encoding="UTF-8"?>
		 <feed xmlns="http://www.w3.org/2005/Atom">
		 	...
		  	<entry>
				<id>c32</id>
				<link rel="alternate" type="application/json" href="https://hdata.herokuapp.com/records/1460/c32"/>
		  	</entry>
			...
		 </feed>

		latest returns:
		 <entry>
			<id>/allergies</id>
			<link rel="alternate" type="text/html" href="http://localhost:3000/records/1/allergies"/>
			<link rel="alternate" type="application/atom+xml" href="http://localhost:3000/records/1/allergies"/>
			<title>Allergies</title>
		  </entry>

		 */
		final Namespace atomNs = Namespace.getNamespace(NAMESPACE_W3_ATOM_2005);
		Set<String> atomSections = new HashSet<String>();
		// check Atom entries against sections in root.xml
		for(Object feedChild : atomDoc.getRootElement().getChildren("entry", atomNs)) {
			if (!(feedChild instanceof Element)) continue;
			Element entry = (Element)feedChild;
			String id = entry.getChildText("id", atomNs);
			if (id == null) continue;
			if (id.startsWith("/")) id = id.substring(1);
			if (StringUtils.isBlank("id")) continue;
			if (sectionPaths.contains(id)) {
				/*
				HL7 2.3.2.2:

				<atom:id> - This element contains a name for the document that is unique over the parent Section.
				For child Sections this name is the path segment for the child Section, as defined in the root.xml document.
				This element MUST be identical to the DocumentId element in the document metadata (see section 2.6.3).
				 */
				atomSections.add(id); // found associated section path in root.xml
			} else {
				addLogWarning("id " + id + " in atom entry not found in associated root.xml document");
			}
			/*
			if (sectionPath == null) {
				addWarning("id " + id + " in atom entry not found in associated root.xml document");
				continue;
			}
			*/
			// System.out.println("XXX: id=" + entry.getChildText("id", atomNs)); // debug
			for(Object entryChild : entry.getChildren("link", atomNs)) {
				if (!(entryChild instanceof Element)) continue;
				Element link = (Element)entryChild;
				String href = link.getAttributeValue("href"); // required
				// note: an atom entry may contain multiple link elements that may or may not map to the root.xml section path
				// ignore links with missing href
				if (StringUtils.isBlank(href)) continue;
				// System.out.println("XXX: link href=" + href);
				try {
					URI entryUri = new URI(href);
					if (!entryUri.isAbsolute()) {
						// REVIEW: is relative URI legal wrt HL7 spec
						// legal wrt ATOM http://tools.ietf.org/html/rfc4287
						entryUri = baseURI.resolve(entryUri);
						log.trace("relative URL {} -> {}", href, entryUri);
					}
					if ("localhost".equals(entryUri.getHost())) {
						if (addWarning("section entry URL cannot be localhost"))
							log.warn("section entry URL cannot be localhost " + entryUri.toASCIIString());
					}
					/*
					String entryUrl = entryUri.toASCIIString();
					if (!entryUrl.startsWith(baseUrl)) {
						//log.trace("link href={}", entryUrl);
						log.debug("other link href={}", entryUrl);
						continue;
					}

					String path = entryUrl.substring(baseUrl.length());
					// System.out.println("XXX: atom path=" + path);
					if (sectionPaths.contains(path)) atomSections.add(path); // found associated section path in root.xml
					else if (debug) log.debug("link href path " + path + " does not match root.xml section path");
					*/
				} catch (URISyntaxException e) {
					if (addLogWarning("Bad entry URL")) log.warn("", e);
				}
			}
		} // for each atom entry

		if (debug) log.debug("atom section paths=" + atomSections);
		assertEquals(sectionPaths.size(), atomSections.size());

		setStatus(StatusEnumType.SUCCESS);
	}

}
