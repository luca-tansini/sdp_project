package ServerCloud.Services;

import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Model;
import ServerCloud.Model.Position;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("sensor")
public class SensorServices {

    @Path("getnearestnode")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response getNearestNode(Position pos) {
        EdgeNodeRepresentation nearestNode = Model.getInstance().getGrid().getNearestNode(pos);
        if(nearestNode != null)
            return Response.ok(nearestNode).build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}