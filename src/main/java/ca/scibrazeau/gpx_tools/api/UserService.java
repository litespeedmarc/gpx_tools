package ca.scibrazeau.gpx_tools.api;


import ca.scibrazeau.gpx_tools.model.User;
import ca.scibrazeau.gpx_tools.store.StoreUtils;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.NotFoundException;
import org.json.JSONObject;

import javax.ws.rs.*;

@Path("/user")
public class UserService {
    @GET
    @Path("/{userId}")
    @Produces("application/json")
    public String getInfo(
            @PathParam("userId") String userId
    ) {
        User user = fetchUser(userId);
        if (user == null) {
            return null;
        }

        JSONObject obj = new JSONObject();
        obj.put("userId", user.getUserId());
        obj.put("isRegistered", user.isRegistered());
        return obj.toString();
    }

    public User fetchUser(String userId) {
        User user = StoreUtils.load(User.class, "users", userId, "info");
        return user;
    }

    public String update( String userId, String confirmKey, String stravaKey) {
        User user = StoreUtils.load(User.class, "users", userId, "info");
        if (user == null) {
            throw new NotFoundException("User " + userId + " does not exist");
        }

        if (!user.isCheckKeyValid(confirmKey)) {
            throw new IllegalArgumentException("Confirmation GUID provided is not valid");
        }

        user.setStravaKey(stravaKey);
        user.setConfirmKey(null);

        return update(user);
    }

    public String update(User user) {
        StoreUtils.save(user, "users", user.getUserId(), "info");
        return "OK";
    }

}
