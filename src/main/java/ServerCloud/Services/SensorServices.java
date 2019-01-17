package ServerCloud.Services;

import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Model;
import ServerCloud.Model.Position;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("sensor")
public class SensorServices {

    Gson gson = new Gson();

    @Path("getnearestnode")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response getNearestNode(String posJson) {
        EdgeNodeRepresentation nearestNode = Model.getInstance().getGrid().getNearestNode(gson.fromJson(posJson, Position.class));
        if(nearestNode != null)
            return Response.ok(gson.toJson(nearestNode)).build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}