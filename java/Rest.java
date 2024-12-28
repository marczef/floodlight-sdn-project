package pl.edu.agh.kt;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import net.floodlightcontroller.restserver.RestletRoutable;


public class Rest implements RestletRoutable {
	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/data", RestServer.class);
		return router;
	}
	
	@Override
	public String basePath() {
		return "/backend";
	}
}