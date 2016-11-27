package ca.scibrazeau.gpx_tools;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@SuppressWarnings("nls")
@Controller
@RequestMapping("/stravaToZones")
public class StravaToZonesController {

	@RequestMapping(method = RequestMethod.GET)
	public String buildPage(ModelMap model) {
		model.addAttribute("message", "Hello Spring MVC Framework!");
		return null;
	}

}
