package pl.edu.agh.kt;
import java.util.Collections;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.engine.application.CorsFilter;
import org.restlet.routing.Router;
import net.floodlightcontroller.restserver.RestletRoutable;


public class Rest implements RestletRoutable {
	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/data", RestServer.class);
		
		CorsFilter corsFilter = new CorsFilter(context, router);
        corsFilter.setAllowedOrigins(Collections.singleton("*")); // Allow all origins
        corsFilter.setAllowedCredentials(true);                  // Allow credentials
        return corsFilter;
	}
	
	@Override
	public String basePath() {
		return "/backend";
	}
}