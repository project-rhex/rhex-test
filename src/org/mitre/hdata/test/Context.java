package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:55 AM
 */
public class Context {

	private static final Logger log = LoggerFactory.getLogger(Context.class);

	private SAXBuilder builder, validatingBuilder;

	/** Validation feature id */
	protected static final String VALIDATION_FEATURE =
		"http://xml.org/sax/features/validation";

	/** Schema validation feature id */
	protected static final String SCHEMA_VALIDATION_FEATURE =
		"http://apache.org/xml/features/validation/schema";

	/** Schema full checking feature id */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE =
		"http://apache.org/xml/features/validation/schema-full-checking";

	protected static final String LOAD_DTD_GRAMMAR =
		"http://apache.org/xml/features/nonvalidating/load-dtd-grammar";  // [TRUE]

	protected static final String LOAD_EXTERNAL_DTD =
		"http://apache.org/xml/features/nonvalidating/load-external-dtd"; // [TRUE]

	protected static final String CONTINUE_AFTER_FATAL_FEATURE =
		"http://apache.org/xml/features/continue-after-fatal-error"; // [FALSE]

	@NonNull
	private URI baseURL;

	private HttpHost proxy;

	// security info
	// root.xml contents ?

	private XMLConfiguration config;
	private String baseUrlString; // cached copy of baseURL.toASCIIString()

	@NonNull
	public URI getBaseURL() {
		return baseURL;
	}

	@NonNull
	public URI getBaseURL(String relativePath) throws URISyntaxException {
		if (StringUtils.isBlank(relativePath)) {
			return baseURL;
		}
		String uri = baseUrlString; // baseURL.toASCIIString();
		if (relativePath.startsWith("/")) {
			// relative paths are relative to the baseURL and server-relative URLs
			// will be assumed to be relative to baseURL not the server root
			// if (uri.endsWith("/")) // always true
			// relativePath = relativePath.substring(1);
			uri += relativePath.substring(1); // strip off the leading '/'
		} else {
			// relative path is correctly relative so append to base URL
			uri += relativePath;
			//if (uri.endsWith("/")) uri += relativePath; // always true
			//else uri += "/" + relativePath;
		}
		return new URI(uri);
	}

	/**
	 * Load configuration
	 *
	 * @param config
	 * @throws IllegalArgumentException
	 * @exception NumberFormatException
	 */
	public void load(XMLConfiguration config) {
		this.config = config;
		final String url = config.getString("baseURL");
		// see http://commons.apache.org/configuration/userguide/howto_xml.html
		if (StringUtils.isBlank(url)) {
			// TODO: if any tests don't require baseURL then may this optional and have tests check and mark status = SKIPPED
			throw new IllegalArgumentException("baseURL property must be defined");
		} else {
			try {
				baseURL = new URI(url);
				if (baseURL.getQuery() != null) {
					log.error("6.1.1 baseURL MUST NOT contain a query component, baseURL=" + baseURL);
				}
				baseUrlString = baseURL.toASCIIString();
				final String baseRawString = baseURL.toString();
				if (!baseUrlString.equals(baseRawString)) {
					log.warn("baseURL appears to have non-ASCII characters and comparisons using URL may have problems");
					log.debug("baseURL ASCII String=" + baseUrlString);
					log.debug("baseURL raw String=" + baseRawString);
				}
				if (!baseUrlString.endsWith("/")) baseUrlString += '/'; // end the baseURL with slash
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}

		// setup HTTP proxy if required
		String proxyHost = config.getString("proxy.host");
		String proxyPort = config.getString("proxy.port");
		if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
			proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
		}
	}
	
	public String getString(String key) {
		return config != null ? config.getString(key) : null;
	}

	public SAXBuilder getBuilder(ErrorHandler errorHandler) {
		if (builder == null) {
			builder = new SAXBuilder(false);
			builder.setFeature(VALIDATION_FEATURE, false); // [false]
			builder.setFeature(SCHEMA_FULL_CHECKING_FEATURE, false); // [false]
			builder.setFeature(SCHEMA_VALIDATION_FEATURE, false); // [false]
			builder.setFeature(LOAD_DTD_GRAMMAR, false); // [true]
			builder.setFeature(LOAD_EXTERNAL_DTD, false); // [true]
			builder.setFeature("http://xml.org/sax/features/external-general-entities", false); // TRUE
			builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // TRUE
			// http://xml.org/sax/features/namespace-prefixes [false]
			// builder.setFeature("http://xml.org/sax/features/namespaces", true); [true]
		}
		builder.setErrorHandler(errorHandler);
		return builder;
	}

	public SAXBuilder getValidatingBuilder(ErrorHandler errorHandler) {
		if (validatingBuilder == null) {
			validatingBuilder = new SAXBuilder(true);
			validatingBuilder.setFeature(VALIDATION_FEATURE, true);
			validatingBuilder.setFeature(SCHEMA_FULL_CHECKING_FEATURE, true);
			validatingBuilder.setFeature(SCHEMA_VALIDATION_FEATURE, true);
			validatingBuilder.setFeature(LOAD_DTD_GRAMMAR, false);
			validatingBuilder.setFeature(LOAD_EXTERNAL_DTD, false);
		}
		validatingBuilder.setErrorHandler(errorHandler);
		return validatingBuilder;
	}

	//private HttpClient client;
	public HttpClient getHttpClient() {
		// TODO/REVIEW: Can/should we create HttpClient once and store here in Context class ?
		// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e582
		HttpClient client = new DefaultHttpClient();
		if (proxy != null) {
			// System.out.println("XXX: use HTTP proxy");
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
		return client;
	}

}
