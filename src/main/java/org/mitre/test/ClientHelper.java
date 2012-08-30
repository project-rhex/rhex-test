package org.mitre.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.mitre.rhex.security.RhexMitreOidcSecurityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Jason Mathews, MITRE Corp.
 *
 * Date: 2/22/12 4:46 PM
 */
public final class ClientHelper {

    private static final Logger log = LoggerFactory.getLogger(ClientHelper.class);

    /**
     * Dump HTTP Response status code and headers
     *
     * @param req Optional Http request may be null
     * @param response Http Response, never null
     */
    public static void dumpResponse(HttpRequestBase req, HttpResponse response) {
        dumpResponse(req, response, false);
    }

    /**
     * Dump HTTP Response status code and headers
     *
     * @param req Optional Http request may be null
     * @param response Http Response, never null
     * @param dumpEntity True if want to dump the response body otherwise body entity is ignored
     */
    public static void dumpResponse(HttpRequestBase req, HttpResponse response, boolean dumpEntity) {
        if (req != null)
            System.out.printf("%s Response %s%n", req.getMethod(), response.getStatusLine());
        else
            System.out.println("Response " + response.getStatusLine());
        System.out.println("Headers:");
        for (Header header : response.getAllHeaders()) {
            String name = header.getName();
            // suppress set-cookie header in output unless debug enabled
            if (log.isDebugEnabled() || !"Set-Cookie".equals(name)) {
				System.out.println("\t" + name + ": " + header.getValue());
			}
        }
        if (dumpEntity) {
            HttpEntity entity = response.getEntity();
            if (entity != null)
                try {
                    String bodyText = EntityUtils.toString(entity);
					System.out.println("----------------------------------------");
                    if (bodyText != null && bodyText.length() > 68)
                        System.out.println("Response body:");
                    else
                        System.out.print("Response body: ");
                    System.out.println(bodyText);
                } catch(Exception e) {
                    log.debug("Failed to get response body", e);
                }
            else
                System.out.println("XXX: No body");
        }
		System.out.println("----------------------------------------");
    }

	/**
	 * Return content-type value with optional character encoding
	 * stripped off.
	 *
	 * @param entity HttpEntity returned from HttpResponse
	 * @return content-type from HTTP response header, null if not present
	 */
	@CheckForNull
	public static String getContentType(HttpEntity entity) {
		return getContentType(entity, false);
	}

	/**
	 * Return content-type value and optionally strip off the character encoding
	 * following the content MIME type.
	 *
	 * @param entity HttpEntity returned from HttpResponse
	 * @param includeEncoding Flag if true it include character encoding if provided
	 *                        otherwise strip encoding from the content type
	 * @return content-type from HTTP response header, null if not present
	 * @see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17
	 */
	@CheckForNull
	public static String getContentType(HttpEntity entity, boolean includeEncoding) {
		// NOTE: similar to EntityUtils.getContentMimeType(HttpEntity)
		if (entity == null) return null;
		Header header = entity.getContentType();
		if (header == null) return null;
		String type = header.getValue();
		if (type != null) {
			if (!includeEncoding) {
				// remove character encoding from mime type
				// Content-Type = text/html; charset=ISO-8859-4 => text/html
				int ind = type.indexOf(';');
				if (ind >= 0) type = type.substring(0, ind);
			}
			type = StringUtils.trimToNull(type);
		}
		return type;
	}

	public static boolean isXmlContentType(String contentType) {
		if (contentType == null || contentType.indexOf('/') <= 0) return false;
		if (contentType.endsWith("+xml")) return true; // e.g. application/atom+xml, application/rdf+xml, image/svg+xml, etc.
		//if (TestUnit.MIME_APPLICATION_ATOM_XML.equals(contentType)) return true;
		if (contentType.endsWith("/xml")) return true; // e.g. text/xml, application/xml
		// REVIEW: missing any other XML mime types that may appear in section ATOM feeds ?
		return false;
	}

	/**
	 * Get redirect location from response header and return as URI if available.
	 *
	 * @param response the HTTP response, never null
	 *
	 * @return URI for redirect location
	 *
	 * @throws NullPointerException if response is null
	 */
	@CheckForNull
	public static URI getRedirectURI(HttpResponse response) {
		final StatusLine statusLine = response.getStatusLine();
		if (statusLine == null || statusLine.getStatusCode() != 302) {
			return null;
		}
		Header location = response.getFirstHeader("Location");
		if (location == null) return null;
		String locationValue = location.getValue();
		if (StringUtils.isBlank(locationValue)) return null;
		try {
			/*
            // debug start
            URI uri = new URI(locationValue);
            if (log.isDebugEnabled()) {
                String query = uri.getQuery();
                if (query != null) {
                    System.out.println("XXX: params");
                    for(String s : query.split("&")) {
                        int ind = s.indexOf('=');
                        if (ind == -1) continue;
                        String name = s.substring(0,ind);
                        String value = s.substring(ind+1);
                        System.out.printf("\t%s=%s%n", name, value);
                        if ("request".equals(name)) {
                            // request parameter is base64-encoded JWT + binary token
                            // e.g. {"typ":"JWT","alg":"HS256"}{"id_token":{"claims"...
                            byte[] bytes = Base64.decodeBase64(value);
                            if (bytes != null) {
                                try {
                                    value = new String(bytes, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    value = new String(bytes); // use default encoding
                                }
                                System.out.println("\t\t" + value);
                            }
                        }
                        //if ("nonce".equals(name)) {
                            //context.setProperty(userEmail.replaceAll("[^a-zA-Z0-9]+","_") + ".nonce", value);
                        //}
                    }
                    // log.debug("XXX: params\n\t" + query.replace("&", "\n\t"));
                }
            }
            return uri;
            // debug end
            */
			return new URI(locationValue);
		} catch (URISyntaxException e) {
			log.error("", e);
			return null;
		}
	}

}
