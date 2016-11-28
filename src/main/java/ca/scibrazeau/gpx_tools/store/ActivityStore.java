package ca.scibrazeau.gpx_tools.store;

import ca.scibrazeau.gpx_tools.api.UserService;
import ca.scibrazeau.gpx_tools.model.ActivityDurationsAndPowers;
import ca.scibrazeau.gpx_tools.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javastrava.api.v3.auth.AuthorisationService;
import javastrava.api.v3.auth.impl.retrofit.AuthorisationServiceImpl;
import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaStream;
import javastrava.api.v3.model.reference.StravaStreamResolutionType;
import javastrava.api.v3.model.reference.StravaStreamSeriesDownsamplingType;
import javastrava.api.v3.model.reference.StravaStreamType;
import javastrava.api.v3.service.Strava;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

/**
 * Created by pssemr on 2016-11-26.
 */
public class ActivityStore {

    private static final String kStravaPrivateKey = "93ad62f7e22f49f119335e8c39a770c66dec41cd";
    public static final int kStravaClientId = 4782;

    private static volatile ActivityStore mSingleton;

    public static ActivityStore access() {
        if (mSingleton == null) {
            mSingleton = new ActivityStore();
        }
        return mSingleton;
    }

    public ActivityDurationsAndPowers getUserActivityPowerAndDurations(String userId, long activityId) {
        ActivityDurationsAndPowers cachedValue = StoreUtils.load(ActivityDurationsAndPowers.class, StoreUtils.kCacheName, activityId);
        if (cachedValue != null) {
            return cachedValue;
        }

        AuthorisationService service = new AuthorisationServiceImpl();
        User user = new UserService().fetchUser(userId);
        if (user == null || !user.isRegistered()) {
            throw new RuntimeException("User " + userId + " is not registered");
        }
        Token token = service.tokenExchange(kStravaClientId, kStravaPrivateKey, user.getStravaKey());
        Strava strava = new Strava(token);
        StravaActivity activity = strava.getActivity((int) activityId);
        List<StravaStream> powerAndTimeStream = strava.getActivityStreams(
                (int) activityId,
                StravaStreamResolutionType.HIGH,
                StravaStreamSeriesDownsamplingType.TIME,
                StravaStreamType.TIME,
                StravaStreamType.POWER);

        List<Float> durationSeconds = powerAndTimeStream.get(0).getData();
        List<Float> powerData = powerAndTimeStream.get(1).getData();

        ActivityDurationsAndPowers toReturn = new ActivityDurationsAndPowers(
                userId,
                activityId,
                activity.getStartDate().toLocalDateTime().toString(),
                durationSeconds,
                powerData
        );

        StoreUtils.save(toReturn, StoreUtils.kCacheName, activityId);

        return toReturn;


    }


}
