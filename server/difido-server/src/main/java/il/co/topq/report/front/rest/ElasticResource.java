package il.co.topq.report.front.rest;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.core.MediaType;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import il.co.topq.report.Configuration;
import il.co.topq.report.Configuration.ConfigProps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;

@RestController
public class ElasticResource {

	private static final Logger log = LoggerFactory.getLogger(ElasticResource.class);

	private RestTemplate template;

	private URL base;

	private boolean enabled = true;

	public ElasticResource() {
		String elasticHost = null;
		int elasticPort = 0;
		elasticHost = Configuration.INSTANCE.readList(ConfigProps.ELASTIC_HOST).get(0);
		elasticPort = Integer.parseInt(Configuration.INSTANCE.readList(ConfigProps.ELASTIC_HTTP_PORT).get(0));
		try {
			base = new URL("http://" + elasticHost + ":" + elasticPort + "/");
		} catch (MalformedURLException e) {
			log.error("Failed to create url due to " + e.getMessage());
			enabled = false;
			return;
		}
		template = new RestTemplate();
	}

	@RequestMapping(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON, value = "api/elastic/{index}/{doc}", method = RequestMethod.POST)
	public String post(@Context HttpServletRequest request, @DefaultValue("report") @PathVariable("index") String index,
			@DefaultValue("test") @PathVariable("doc") String doc, @RequestBody String body) {
		log.debug("PUT (" + request.getRemoteAddr() + ") - Query from Elastic");
		if (!enabled) {
			log.warn("Aborting request from " + request.getRemoteAddr() + " to post to Elastic since it is disabled");
			return "";
		}
		ResponseEntity<String> response = null;
		try {
			response = template.postForEntity(base.toString() + index + "/" + doc + "/_search?pretty=true", body,
					String.class);

		} catch (RestClientException e) {
			log.warn("Aborting request from " + request.getRemoteAddr()
					+ " to post to Elastic since no connection can be established");
			return "";
		}

		return response.getBody();
	}

}
