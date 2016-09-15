package no.rutebanken.anshar.routes.outbound;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.org.siri.siri20.Siri;

import javax.xml.bind.JAXBException;
import java.util.UUID;

@Service
public class CamelRouteManager implements CamelContextAware {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static CamelContext camelContext;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }


    /**
     * Creates a new ad-hoc route that sends the SIRI payload to supplied address, executes it, and finally terminates and removes it.
     * @param payload
     * @param consumerAddress
     * @param soapRequest
     */
    public void pushSiriData(Siri payload, String consumerAddress, boolean soapRequest) {
        try {

            SiriPushRouteBuilder siriPushRouteBuilder = new SiriPushRouteBuilder(consumerAddress, soapRequest);
            String routeId = addSiriPushRoute(siriPushRouteBuilder);
            executeSiriPushRoute(payload, siriPushRouteBuilder.getRouteName());
            stopAndRemoveSiriPushRoute(routeId);

        } catch (Exception e) {
            logger.warn("Exception caught when pushing SIRI-data", e);
        }
    }

    private String addSiriPushRoute(SiriPushRouteBuilder route) throws Exception {
        this.camelContext.addRoutes(route);
        return route.getDefinition().getId();
    }

    private boolean stopAndRemoveSiriPushRoute(String routeId) throws Exception {
        this.getCamelContext().stopRoute(routeId);
        return this.camelContext.removeRoute(routeId);
    }


    private void executeSiriPushRoute(Siri payload, String routeName) throws JAXBException {
        String xml = SiriXml.toXml(payload);

        ProducerTemplate template = camelContext.createProducerTemplate();
        template.sendBody(routeName, xml);
    }
    private class SiriPushRouteBuilder extends RouteBuilder {

        private final boolean soapRequest;
        private String remoteEndPoint;
        private RouteDefinition definition;
        private String routeName;

        public SiriPushRouteBuilder(String remoteEndPoint, boolean soapRequest) {
            this.remoteEndPoint=remoteEndPoint;
            this.soapRequest = soapRequest;
        }

        @Override
        public void configure() throws Exception {

            if (remoteEndPoint.startsWith("http://")) {
                remoteEndPoint = remoteEndPoint.substring("http://".length());
            }

            routeName = String.format("direct:%s", UUID.randomUUID().toString());

            if (soapRequest) {
                definition = from(routeName)
                        .to("xslt:xsl/siri_raw_soap.xsl") // Convert SIRI raw request to SOAP version
                        .setHeader("CamelHttpMethod", constant("POST"))
                        .marshal().string("UTF-8")
                        .to("http4://" + remoteEndPoint);
            } else {
                definition = from(routeName)
                        .setHeader("CamelHttpMethod", constant("POST"))
                        .marshal().string("UTF-8")
                        .to("http4://" + remoteEndPoint);
            }

        }

        public RouteDefinition getDefinition() {
            return definition;
        }

        public String getRouteName() {
            return routeName;
        }
    }
}
