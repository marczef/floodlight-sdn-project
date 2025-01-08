package pl.edu.agh.kt;

import java.io.IOException;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Router;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.floodlightcontroller.restserver.RestletRoutable;

public class RestLab implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/timeout", LabRestServer.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/sdnlab";
	}
}
