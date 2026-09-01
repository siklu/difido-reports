package il.co.topq.difido.reporters;

import il.co.topq.difido.model.execution.MachineNode;
import il.co.topq.difido.model.remote.ExecutionDetails;
import il.co.topq.difido.model.test.TestDetails;

import java.io.File;
import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DifidoClient {

	private static final String BASE_URI_TEMPLATE = "%s://%s:%d/api/";
	private final String baseUri;
	private final CloseableHttpClient client;

	public DifidoClient(String host, int port) {
		if (443 == port || 8443 == port) {
			baseUri = String.format(BASE_URI_TEMPLATE, "https", host, port);
		} else {
			baseUri = String.format(BASE_URI_TEMPLATE, "http", host, port);
		}
		client = HttpClients.createDefault();
	}

	public int addExecution(ExecutionDetails details) throws Exception {
		final HttpPost method = new HttpPost(baseUri + "executions/");
		method.setHeader("Content-Type", "application/json");
		if (details != null) {
			final String descriptionJson = new ObjectMapper().writeValueAsString(details);
			method.setEntity(new StringEntity(descriptionJson, ContentType.APPLICATION_JSON));
		}
		return client.execute(method, response -> {
			handleResponseCode(response.getCode());
			return Integer.parseInt(EntityUtils.toString(response.getEntity()));
		});
	}

	public void endExecution(int executionId) throws Exception {
		final HttpPut method = new HttpPut(baseUri + "executions/" + executionId + "?active=false");
		method.setHeader("Content-Type", "text/plain");
		client.execute(method, response -> {
			handleResponseCode(response.getCode());
			return null;
		});
	}

	public int addMachine(int executionId, MachineNode machine) throws Exception {
		HttpPost method = new HttpPost(baseUri + "executions/" + executionId + "/machines/");
		method.setHeader("Content-Type", "application/json");
		final String json = new ObjectMapper().writeValueAsString(machine);
		method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		return client.execute(method, response -> {
			handleResponseCode(response.getCode());
			return Integer.parseInt(EntityUtils.toString(response.getEntity()));
		});
	}

	public void updateMachine(int executionId, int machineId, MachineNode machine) throws Exception {
		HttpPut method = new HttpPut(baseUri + "executions/" + executionId + "/machines/" + machineId);
		method.setHeader("Content-Type", "application/json");
		final String json = new ObjectMapper().writeValueAsString(machine);
		method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		client.execute(method, response -> {
			handleResponseCode(response.getCode());
			return null;
		});
	}

	public void addTestDetails(int executionId, TestDetails testDetails) throws Exception {
		HttpPost method = new HttpPost(baseUri + "executions/" + executionId + "/details");
		method.setHeader("Content-Type", "application/json");
		final String json = new ObjectMapper().writeValueAsString(testDetails);
		method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		client.execute(method, response -> {
			handleResponseCode(response.getCode());
			return null;
		});
	}

	public void addFile(final int executionId, final String uid, final byte[] bytes, String fileName) throws Exception {
		addFile(executionId, uid, MultipartEntityBuilder.create()
				.addBinaryBody("file", bytes, ContentType.APPLICATION_OCTET_STREAM, fileName)
				.build());
	}

	public void addFile(final int executionId, final String uid, final File file) throws Exception {
		addFile(executionId, uid, MultipartEntityBuilder.create()
				.addBinaryBody("file", file)
				.build());
	}

	private void addFile(final int executionId, final String uid, org.apache.hc.core5.http.HttpEntity entity) throws Exception {
		HttpPost method = new HttpPost(baseUri + "executions/" + executionId + "/details/" + uid + "/file/");
		method.setEntity(entity);
		client.execute(method, response -> {
			handleResponseCode(response.getCode());
			return null;
		});
	}

	private void handleResponseCode(int responseCode) throws IOException {
		if (responseCode != 200 && responseCode != 204) {
			throw new IOException("Request was not successful. Response is: " + responseCode);
		}
	}

}


