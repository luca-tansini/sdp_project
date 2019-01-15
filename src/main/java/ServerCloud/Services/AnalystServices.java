package ServerCloud.Services;

import ServerCloud.Model.Model;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("analyst")
public class AnalystServices {

    @Path("nodes")
    @GET
    @Produces({"application/json"})
    public Response getNodes(){
        return Response.ok(Model.getInstance().getGrid().getNodeList()).build();
    }

    @Path("rawstatistics")
    @GET
    @Produces({"application/json"})
    public Response getRawStatistics(@QueryParam("n") int n){
        if(n < 0)
            return Response.status(Response.Status.BAD_REQUEST).build();
        return Response.ok(Model.getInstance().getStats().getStatistics(n)).build();
    }

    @Path("rawstatistics/{id}")
    @GET
    @Produces({"application/json"})
    public Response getRawStatistics(@PathParam("id") String id, @QueryParam("n") int n) {
        if (n < 0)
            return Response.status(Response.Status.BAD_REQUEST).build();
        try {
            return Response.ok(Model.getInstance().getStats().getLocalStatistics(id, n)).build();
        } catch (IllegalArgumentException e){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
