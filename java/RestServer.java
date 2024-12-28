package pl.edu.agh.kt;
import java.io.IOException;
import java.util.ArrayList;

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
	static {
		mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}
	protected static Logger log = LoggerFactory.getLogger(RestServer.class);
	
	@Get("json")
	public String handleGet() throws JsonProcessingException {
		log.info("handleGet");
		return mapper.writeValueAsString(getSampleTopology());
	}
	
	private Topology getSampleTopology() {
		Topology top = new Topology();
		
		ArrayList<Topology.Link> links = new ArrayList<Topology.Link>();
		links.add(new Topology.Link("h1", "s1"));
		links.add(new Topology.Link("s1", "h1"));
		links.add(new Topology.Link("s1", "h2"));
		links.add(new Topology.Link("h2", "s1"));
		top.setLinks(links);
		
		ArrayList<Topology.Node> nodes = new ArrayList<Topology.Node>();
		nodes.add(new Topology.Node("h1"));
		nodes.add(new Topology.Node("s1"));
		nodes.add(new Topology.Node("s1"));
		top.setNodes(nodes);
		
		return top;
	}
}