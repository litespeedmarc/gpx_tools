package ca.scibrazeau.gpx_tools;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class WebConfig {
	static {
		System.out.println("Test"); //$NON-NLS-1$
	}

}
