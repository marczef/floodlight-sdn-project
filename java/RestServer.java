package pl.edu.agh.kt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
public class RestServer extends ServerResource {
	private static final ObjectMapper mapper;
	private TopologyManager topManager;
	
	static {
		mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}
	protected static Logger log = LoggerFactory.getLogger(RestServer.class);
	
	public RestServer() {
		this.topManager = TopologyManager.getInstance();
	}
	
	@Get("json")
	public String handleGet() throws JsonProcessingException {
		return mapper.writeValueAsString(getSampleTopology());
	}
	
	private Topology getSampleTopology() {
		return topManager.getTopologyWithDiffTimestamp();
	}
}