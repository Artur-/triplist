package fi.jole.triplog.workaround;

import org.jsoup.nodes.Element;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ServiceInitEvent;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.server.VaadinServletService;

/**
 * Various hacks that should really be features in Flow instead
 */
public class TrippyServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        ValoDependencyFilter dependencyFilter = new ValoDependencyFilter();
        if (dependencyFilter.hasValoWebJar()) {
            event.addDependencyFilter(dependencyFilter);
        }

        VaadinServletService service = (VaadinServletService) event.getSource();

        WebApplicationContext applicationContext = WebApplicationContextUtils
                .findWebApplicationContext(
                        service.getServlet().getServletContext());

        String apiKey = applicationContext.getEnvironment()
                .getProperty("map.apikey");
        if (apiKey == null) {
            throw new RuntimeException(
                    "Configure a map.apikey in your application.properties");
        }

        event.addBootstrapListener(new BootstrapListener() {
            @Override
            public void modifyBootstrapPage(BootstrapPageResponse response) {
                Element head = response.getDocument().head();
                head.appendElement("meta").attr("name", "viewport")
                        .attr("content", "width=device-width, initial-scale=1");

                // Flow sets element properties too late for google-map to get
                // the right API key. As a temporary workaround, we put our key
                // in the map's prototype instead.
                head.appendElement("script").html("window.mapApiKey = '"
                        + apiKey + "';\n"
                        + "customElements.whenDefined('google-map').then(function() {customElements.get('google-map').prototype.apiKey = window.mapApiKey})");

                head.appendElement("link").attr("rel", "import").attr("href",
                        response.getUriResolver().resolveVaadinUri(
                                "frontend://bower_components/vaadin-valo-theme/typography.html"));
                head.appendElement("link").attr("rel", "import").attr("href",
                        response.getUriResolver().resolveVaadinUri(
                                "frontend://bower_components/vaadin-valo-theme/color.html"));

                head.appendElement("custom-style").appendElement("style")
                        .attr("include", "valo-typography valo-colors");
            }
        });
    }

}
