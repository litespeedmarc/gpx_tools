package ca.scibrazeau.gpx_tools.api;


import ca.scibrazeau.gpx_tools.model.TcxSummary;
import ca.scibrazeau.gpx_tools.strava.TcxGrabber;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import javax.ws.rs.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Path("/stravaActivity")
public class StravaActivity {
    @GET
    @Path("/{userId}/{activityId}")
    @Produces("text/plain")
    public String getSummarizedActivity(
            @PathParam("userId") String userId,
            @PathParam("activityId") long activityId,
            @QueryParam("targetDuration") int targetDurationSeconds,
            @QueryParam("targetZone") int targetZone,
            @QueryParam("zones") String zones
    ) throws IOException {
        TcxGrabber grabber = new TcxGrabber(userId, activityId, targetDurationSeconds, targetZone, zones);
        TcxSummary result = grabber.summarize();
        StringWriter strWriter = new StringWriter();
        CsvListWriter writer = new CsvListWriter(strWriter, CsvPreference.EXCEL_PREFERENCE);
        List<Object> columns = new ArrayList<>();
        columns.add(result.getDateTime());
        columns.add(result.getDurationSeconds());
        columns.add(result.getRideAveragePowerWatts());
        columns.add(result.getNumberOfIntervals());
        columns.add(result.getAverageIntervalPowerWatts());
        columns.add(result.getAverageIntervalSeconds());
        columns.add(result.getAverageRestSeconds());
        for (TcxSummary.Interval i : result.getIntervals()) {
            columns.add(i.getRest() + ":" + i.getWatts() + ":" + i.getDuration());
        }
        writer.write(columns);
        writer.close();
        strWriter.close();
        return strWriter.toString();

    }

}
