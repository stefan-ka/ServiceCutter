package ch.hsr.servicecutter;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import ch.hsr.servicecutter.EngineServiceAppication;
import ch.hsr.servicecutter.model.systemdata.Model;
import ch.hsr.servicecutter.model.systemdata.Nanoentity;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = EngineServiceAppication.class)
@IntegrationTest("server.port=0")
@WebAppConfiguration
public class EngineServiceRestTest {

	@Value("${local.server.port}")
	private int port;
	private RestTemplate restTemplate = new TestRestTemplate();

	@Test
	public void createEmptyModel() {
		Model model = new Model();
		model.setName("testModel");
		HttpEntity<Model> request = IntegrationTestHelper.createHttpRequestWithPostObj(model);
		ResponseEntity<Model> requestResult = this.restTemplate.exchange("http://localhost:" + this.port + "/engine/models", HttpMethod.POST, request, Model.class);
		assertEquals(HttpStatus.OK, requestResult.getStatusCode());
		Long id = requestResult.getBody().getId();

		ResponseEntity<Model> assertResult = this.restTemplate.exchange("http://localhost:" + this.port + "/engine/models/" + id, HttpMethod.GET,
				IntegrationTestHelper.createEmptyHttpRequest(), Model.class);
		assertEquals("testModel", assertResult.getBody().getName());
	}

	@Test
	public void createModelWithEntities() {
		int before = countModels();
		createModelOnApi();
		// test whether created model is visible
		assertEquals(before + 1, countModels());
	}

	private Long createModelOnApi() {
		Model model = createModel();
		HttpEntity<Model> request = IntegrationTestHelper.createHttpRequestWithPostObj(model);
		ResponseEntity<Model> entity = restTemplate.exchange("http://localhost:" + this.port + "/engine/models/", HttpMethod.POST, request, Model.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		return entity.getBody().getId();
	}

	private int countModels() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> response = restTemplate.getForEntity("http://localhost:" + this.port + "/engine/models", List.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		int models = response.getBody().size();
		return models;
	}

	private Model createModel() {
		Model model = new Model();
		model.setName("firstModel");
		Nanoentity nanoentity1 = new Nanoentity();
		nanoentity1.setName("firstNanoentity");
		Nanoentity nanoentity2 = new Nanoentity();
		nanoentity2.setName("secondNanoentity");
		model.addNanoentity(nanoentity1);
		model.addNanoentity(nanoentity2);
		return model;
	}

}