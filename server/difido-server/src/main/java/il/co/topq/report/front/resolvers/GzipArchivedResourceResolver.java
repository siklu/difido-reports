package il.co.topq.report.front.resolvers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.HttpResource;
import org.springframework.web.servlet.resource.ResourceResolverChain;

/**
	 * Serves a gzipped resource preferentially when the client accepts gzip encoding and a .gz
	 * resource exists. Falls back to the original uncompressed resource, or to .gz as a final
	 * fallback. This enables bandwidth optimization for clients that support decompression.
	 */
	public class GzipArchivedResourceResolver extends AbstractResourceResolver {

	private static final Logger logger = LoggerFactory.getLogger(GzipArchivedResourceResolver.class);
	private static final String GZ = ".gz";

	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		// Resolve both original and gzipped resources
		Resource originalResource = chain.resolveResource(request, requestPath, locations);
		Resource gzippedResource = null;
		
		try {
			gzippedResource = chain.resolveResource(request, requestPath + GZ, locations);
		} catch (Exception e) {
			logger.error("Unexpected exception resolving gzipped resource for [{}]", requestPath, e);
		}
		
		// Check if client accepts gzip encoding
		boolean clientAcceptsGzip = acceptsGzip(request);
		
		// Prefer gzipped resource if client accepts gzip and .gz resource exists
		if (clientAcceptsGzip && gzippedResource != null) {
			GzipArchivedResource archived = new GzipArchivedResource(gzippedResource);
			if (archived.exists()) {
				return archived;
			}
		}
		
		// Return original resource if it exists
		if (originalResource != null) {
			return originalResource;
		}
		
		// Fallback: return gzipped resource even if client doesn't accept gzip
		if (gzippedResource != null) {
			GzipArchivedResource archived = new GzipArchivedResource(gzippedResource);
			if (archived.exists()) {
				return archived;
			}
		}
		
		return null;
	}

	/**
	 * Checks if the HTTP request indicates that the client accepts gzip encoding.
	 *
	 * @param request the HTTP request
	 * @return true if the Accept-Encoding header contains "gzip", false otherwise
	 */
	private boolean acceptsGzip(HttpServletRequest request) {
		String acceptEncoding = request.getHeader("Accept-Encoding");
		return acceptEncoding != null && acceptEncoding.contains("gzip");
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {
		return chain.resolveUrlPath(resourceUrlPath, locations);
	}

	private static final class GzipArchivedResource extends AbstractResource implements HttpResource {
		private final Resource archived;
		private final String originalFileName;

		public GzipArchivedResource(Resource resource) {
			this.archived = resource;
			String name = resource.getFilename();
			originalFileName = name.substring(0, name.length() - GZ.length());
		}

		@Override
		public boolean isReadable() {
			return this.archived.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.archived.isOpen();
		}

		@Override
		public boolean exists() {
			return this.archived.exists();
		}

		@Override
		public String getDescription() {
			return archived.getDescription();
		}

		@Override
		public File getFile() throws IOException {
			return this.archived.getFile();
		}

		@Override
		public URI getURI() throws IOException {
			return this.archived.getURI();
		}

		@Override
		public URL getURL() throws IOException {
			return this.archived.getURL();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return archived.getInputStream();
		}

		@Override
		public long lastModified() throws IOException {
			return this.archived.lastModified();
		}

		@Override
		public long contentLength() throws IOException {
			return this.archived.contentLength();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.archived.createRelative(relativePath);
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.CONTENT_ENCODING, "gzip");
			return headers;
		}

		@Override
		public String getFilename() {
			return originalFileName;
		}
	}
}
