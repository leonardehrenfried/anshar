package no.rutebanken.anshar.routes.siri;

import no.rutebanken.anshar.config.AnsharConfiguration;
import no.rutebanken.anshar.data.EstimatedTimetables;
import no.rutebanken.anshar.data.ProductionTimetables;
import no.rutebanken.anshar.data.Situations;
import no.rutebanken.anshar.data.VehicleActivities;
import no.rutebanken.anshar.routes.siri.handlers.SiriHandler;
import no.rutebanken.anshar.routes.siri.helpers.SiriObjectFactory;
import no.rutebanken.anshar.routes.siri.transformer.SiriValueTransformer;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.helpers.MappingAdapterPresets;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpHeaders;
import org.rutebanken.siri20.util.SiriJson;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.org.siri.siri20.Siri;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service
public class SiriLiteRoute extends RouteBuilder {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AnsharConfiguration configuration;

    @Autowired
    private Situations situations;

    @Autowired
    private VehicleActivities vehicleActivities;

    @Autowired
    private EstimatedTimetables estimatedTimetables;

    @Autowired
    private ProductionTimetables productionTimetables;
    
    @Autowired
    private SiriObjectFactory siriObjectFactory;

    @Autowired
    private MappingAdapterPresets mappingAdapterPresets;

    @Override
    public void configure() throws Exception {

        restConfiguration("jetty")
                .port(configuration.getInboundPort());

        rest("/anshar/rest")
                .get("/sx").to("direct:anshar.rest.sx")
                .get("/vm").to("direct:anshar.rest.vm")
                .get("/et").to("direct:anshar.rest.et")
                .get("/pt").to("direct:anshar.rest.pt");

        // Dataproviders
        from("direct:anshar.rest.sx")
                .log("RequestTracer - Incoming request (SX)")
                .process(p -> {
                    p.getOut().setHeaders(p.getIn().getHeaders());

                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String datasetId = request.getParameter("agencyId");
                    if (datasetId == null) {
                        datasetId = request.getParameter("datasetId");
                    }
                    String requestorId = request.getParameter("requestorId");
                    String originalId = request.getParameter("useOriginalId");
                    String maxSizeStr = request.getParameter("maxSize");

                    int maxSize = datasetId != null ? Integer.MAX_VALUE:configuration.getDefaultMaxSize();
                    if (maxSizeStr != null) {
                        try {
                            maxSize = Integer.parseInt(maxSizeStr);
                        } catch (NumberFormatException nfe) {
                            //ignore
                        }
                    }

                    Siri response = situations.createServiceDelivery(requestorId, datasetId, maxSize);

                    List<ValueAdapter> outboundAdapters = mappingAdapterPresets.getOutboundAdapters(SiriHandler.getIdMappingPolicy(request.getQueryString()));
                    if ("test".equals(originalId)) {
                        outboundAdapters = null;
                    }
                    response = SiriValueTransformer.transform(response, outboundAdapters);

                    HttpServletResponse out = p.getIn().getBody(HttpServletResponse.class);

                    if ("application/json".equals(p.getIn().getHeader(HttpHeaders.CONTENT_TYPE)) |
                            "application/json".equals(p.getIn().getHeader(HttpHeaders.ACCEPT))) {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        SiriJson.toJson(response, out.getOutputStream());
                    } else {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
                        SiriXml.toXml(response, null, out.getOutputStream());
                    }
                })
                .log("RequestTracer - Request done (SX)")
                .routeId("incoming.rest.sx")
        ;

        from("direct:anshar.rest.vm")
                .log("RequestTracer - Incoming request (VM)")
                .process(p -> {
                    p.getOut().setHeaders(p.getIn().getHeaders());

                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String datasetId = request.getParameter("agencyId");
                    if (datasetId == null) {
                        datasetId = request.getParameter("datasetId");
                    }
                    String requestorId = request.getParameter("requestorId");
                    String originalId = request.getParameter("useOriginalId");
                    String maxSizeStr = request.getParameter("maxSize");
                    String lineRef = request.getParameter("lineRef");

                    int maxSize = datasetId != null ? Integer.MAX_VALUE:configuration.getDefaultMaxSize();
                    if (maxSizeStr != null) {
                        try {
                            maxSize = Integer.parseInt(maxSizeStr);
                        } catch (NumberFormatException nfe) {
                            //ignore
                        }
                    }

                    Siri response;
                    if (lineRef != null) {
                        response = vehicleActivities.createServiceDelivery(lineRef);
                    } else {
                        response = vehicleActivities.createServiceDelivery(requestorId, datasetId, maxSize);
                    }


                    List<ValueAdapter> outboundAdapters = mappingAdapterPresets.getOutboundAdapters(SiriHandler.getIdMappingPolicy(request.getQueryString()));
                    if ("test".equals(originalId)) {
                        outboundAdapters = null;
                    }
                    response = SiriValueTransformer.transform(response, outboundAdapters);

                    HttpServletResponse out = p.getIn().getBody(HttpServletResponse.class);

                    if ("application/json".equals(p.getIn().getHeader(HttpHeaders.CONTENT_TYPE)) |
                            "application/json".equals(p.getIn().getHeader(HttpHeaders.ACCEPT))) {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        SiriJson.toJson(response, out.getOutputStream());
                    } else {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
                        SiriXml.toXml(response, null, out.getOutputStream());
                    }
                })
                .log("RequestTracer - Request done (VM)")
                .routeId("incoming.rest.vm")
        ;


        from("direct:anshar.rest.et")
                .log("RequestTracer - Incoming request (ET)")
                .process(p -> {
                    p.getOut().setHeaders(p.getIn().getHeaders());

                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String datasetId = request.getParameter("agencyId");
                    if (datasetId == null) {
                        datasetId = request.getParameter("datasetId");
                    }
                    String requestorId = request.getParameter("requestorId");
                    String originalId = request.getParameter("useOriginalId");
                    String maxSizeStr = request.getParameter("maxSize");
                    String lineRef = request.getParameter("lineRef");

                    int maxSize = datasetId != null ? Integer.MAX_VALUE:configuration.getDefaultMaxSize();
                    if (maxSizeStr != null) {
                        try {
                            maxSize = Integer.parseInt(maxSizeStr);
                        } catch (NumberFormatException nfe) {
                            //ignore
                        }
                    }

                    Siri response;
                    if (lineRef != null) {
                        response = estimatedTimetables.createServiceDelivery(lineRef);
                    } else {
                        response = estimatedTimetables.createServiceDelivery(requestorId, datasetId, maxSize);
                    }

                    List<ValueAdapter> outboundAdapters = mappingAdapterPresets.getOutboundAdapters(SiriHandler.getIdMappingPolicy(request.getQueryString()));
                    if ("test".equals(originalId)) {
                        outboundAdapters = null;
                    }
                    response = SiriValueTransformer.transform(response, outboundAdapters);

                    HttpServletResponse out = p.getIn().getBody(HttpServletResponse.class);

                    if ("application/json".equals(p.getIn().getHeader(HttpHeaders.CONTENT_TYPE)) |
                            "application/json".equals(p.getIn().getHeader(HttpHeaders.ACCEPT))) {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        SiriJson.toJson(response, out.getOutputStream());
                    } else {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
                        SiriXml.toXml(response, null, out.getOutputStream());
                    }
                })
                .log("RequestTracer - Request done (ET)")
                .routeId("incoming.rest.et")
        ;


        from("direct:anshar.rest.pt")
                .log("RequestTracer - Incoming request (PT)")
                .process(p -> {
                    p.getOut().setHeaders(p.getIn().getHeaders());

                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String datasetId = request.getParameter("agencyId");
                    if (datasetId == null) {
                        datasetId = request.getParameter("datasetId");
                    }
                    String requestorId = request.getParameter("requestorId");
                    String originalId = request.getParameter("useOriginalId");

                    Siri response = siriObjectFactory.createPTServiceDelivery(productionTimetables.getAllUpdates(requestorId, datasetId));

                    List<ValueAdapter> outboundAdapters = mappingAdapterPresets.getOutboundAdapters(SiriHandler.getIdMappingPolicy(request.getQueryString()));
                    if ("test".equals(originalId)) {
                        outboundAdapters = null;
                    }
                    response = SiriValueTransformer.transform(response, outboundAdapters);

                    HttpServletResponse out = p.getIn().getBody(HttpServletResponse.class);

                    if ("application/json".equals(p.getIn().getHeader(HttpHeaders.CONTENT_TYPE)) |
                            "application/json".equals(p.getIn().getHeader(HttpHeaders.ACCEPT))) {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        SiriJson.toJson(response, out.getOutputStream());
                    } else {
                        out.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
                        SiriXml.toXml(response, null, out.getOutputStream());
                    }
                })
                .log("RequestTracer - Request done (PT)")
                .routeId("incoming.rest.pt")
        ;

    }

}
