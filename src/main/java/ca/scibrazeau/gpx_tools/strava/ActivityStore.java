package ca.scibrazeau.gpx_tools.strava;

import ca.scibrazeau.gpx_tools.model.ActivityDurationsAndPowers;
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
    private static final int kStravaClientId = 4782;

    private static volatile ActivityStore mSingleton;

    public static ActivityStore access() {
        if (mSingleton == null) {
            mSingleton = new ActivityStore();
        }
        return mSingleton;
    }

    public ActivityDurationsAndPowers getUserActivityPowerAndDurations(String userId, long activityId) {
        File cacheFile = new File("gpx_tools/cache/" + activityId + "/");
        Gson json = new GsonBuilder().create();
        if (cacheFile.exists()) {
            try (
                    InputStream is = new BufferedInputStream(new FileInputStream(cacheFile), 100000);
                    InputStreamReader reader = new InputStreamReader(is);
            ) {
                ActivityDurationsAndPowers toReturn = json.fromJson(reader, ActivityDurationsAndPowers.class);
                if (toReturn != null) {
                    return toReturn;
                }
            } catch (Exception e) {
                // just go to strava
            }
        }
        String userCode = UserStore.access().getStravaCode(userId);
        AuthorisationService service = new AuthorisationServiceImpl();
        // TODO: Hard code to not be ME!
        Token token = service.tokenExchange(kStravaClientId, kStravaPrivateKey, userCode);
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

        cacheFile.getParentFile().mkdirs();
        try (
                OutputStream os = new BufferedOutputStream(new FileOutputStream(cacheFile), 100000);
                OutputStreamWriter writer = new OutputStreamWriter(os );
        ) {
            json.toJson(toReturn, writer);
        } catch (IOException e) {
            // failed to cache :(
            LoggerFactory.getLogger(ActivityStore.class).warn("Failed to cache to " + cacheFile.getAbsolutePath() + ": " + e.toString());
        }

        return toReturn;


    }


}
