package ServerCloud.Services;

import ServerCloud.Model.EdgeNode;
import ServerCloud.Model.Model;
import ServerCloud.Model.Statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("edgenetwork")
public class EdgeNetworkServices {

    @Path("nodes")
    @GET
    @Produces({"application/json"})
    public Response getNodes(){
        return Response.ok(Model.getInstance().getGrid().getNodes()).build();
    }

    @Path("addnode")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response addNode(EdgeNode node){
        try{
            Model.getInstance().getGrid().addNode(node);
        } catch (IllegalArgumentException e){
            return  Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
        return Response.ok(Model.getInstance().getGrid().getNodes()).build();
    }

    @Path("nodes/{id}")
    @DELETE
    public Response removeNode(@PathParam("id") int nodeId){
        try{
            Model.getInstance().getGrid().removeNode(nodeId);
        } catch (IllegalArgumentException e){
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
        return Response.ok().build();
    }


    @Path("statistics")
    @POST
    @Consumes({"application/json"})
    public Response updateStatistics(Statistics stats){
        Model.getInstance().getStats().update(stats);
        return Response.ok().build();
    }

    @Path("statistics")
    @GET
    @Produces({"application/json"})
    public Response getStatistics(){
        return Response.ok(Model.getInstance().getStats()).build();
    }

}
