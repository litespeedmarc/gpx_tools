package ca.scibrazeau.gpx_tools.mvc;

import ca.scibrazeau.gpx_tools.model.StravaToZones;
import ca.scibrazeau.gpx_tools.model.TcxSummary;
import ca.scibrazeau.gpx_tools.strava.TcxGrabber;
import ca.scibrazeau.gpx_tools.store.UserStore;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
@Controller
@RequestMapping("/stravaToZones")
public class StravaToZonesController {

	public static final String REQUEST_PARAMETERS = "requestParameters";

	@RequestMapping(method = RequestMethod.GET)
	public String buildPage(
					@ModelAttribute(REQUEST_PARAMETERS) StravaToZones stravaToZones,
					@CookieValue(value = "lastUserId", defaultValue="") String lastUserId,
					Model model,
					HttpServletResponse response
	) {
		if (StringUtils.isEmpty(stravaToZones.getUserId())) {
			stravaToZones = UserStore.access().getLastStravaToZonesForUser(lastUserId);
			model.addAttribute(REQUEST_PARAMETERS, stravaToZones);
			return "stravaToZones";
		}
		model.addAttribute(REQUEST_PARAMETERS, stravaToZones);
		// remember user id for convenience
		response.addCookie(new Cookie("lastUserId", stravaToZones.getUserId()));

		// remember last zones for convenience (we don't use cookie, we keep track of that ourselves.
		if (!StringUtils.isEmpty(stravaToZones.getUserId())) {
			UserStore.access().setLastStravaToZonesParams(stravaToZones.getUserId(), stravaToZones);
		}
		try {
			TcxGrabber grabber = new TcxGrabber(stravaToZones.getUserId(), stravaToZones.getActivityId(), stravaToZones.getTargetTimeInZone(), stravaToZones.getTargetZone(), stravaToZones.getZones());
			TcxSummary summary = grabber.summarize();
			model.addAttribute("summary", summary);
			model.addAttribute("realPoints", grabber.getRealPoints());
			model.addAttribute("zonedPoints", grabber.getZonedPoints());
			model.addAttribute("matchingZonePoints", grabber.getMatchingZonePoints());
		} catch (Exception e) {
			model.addAttribute("error", e.toString());

		}
		return "stravaToZones";
	}

}
