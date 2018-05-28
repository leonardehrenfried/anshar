/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.rutebanken.anshar.routes.siri.processor;

import no.rutebanken.anshar.routes.siri.transformer.impl.StopPlaceRegisterMappingFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

@Component
@Configuration
public class BaneNorIdPlatformUpdaterService {
    private final Logger logger = LoggerFactory.getLogger(BaneNorIdPlatformUpdaterService.class);

    private static final Object LOCK = new Object();

    private final ConcurrentMap<String, String> jbvCodeStopPlaceMappings = new ConcurrentHashMap<>();

    @Value("${anshar.mapping.jbvCode.url}")
    private String jbvCodeStopPlaceMappingUrl;

    @Value("${anshar.mapping.jbvCode.update.frequency.min:60}")
    private int updateFrequency = 60;

    @Autowired
    private StopPlaceRegisterMappingFetcher stopPlaceRegisterMappingFetcher;

    public String get(String id) {
        if (jbvCodeStopPlaceMappings.isEmpty()) {
            // Avoid multiple calls at the same time.
            // Could have used a timed lock here.
            synchronized (LOCK) {
                // Check again.
                if (jbvCodeStopPlaceMappings.isEmpty()) {
                    updateIdMapping();
                }
            }
        }
        return jbvCodeStopPlaceMappings.get(id);
    }

    @PostConstruct
    private void initialize() {
        updateIdMapping();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        int initialDelay = updateFrequency + new Random().nextInt(10);
        executor.scheduleAtFixedRate(this::updateIdMapping, initialDelay, updateFrequency, TimeUnit.MINUTES);


        logger.info("Initialized jbvCode_mapping-updater with url:{}, updateFrequency:{} min, initialDelay:{} min", jbvCodeStopPlaceMappingUrl, updateFrequency, initialDelay);
    }

    private void updateIdMapping() {
        try {
            // re-entrant
            synchronized (LOCK) {
                updateStopPlaceMapping();
            }
        } catch (Exception e) {
            logger.warn("Fetching data - caused exception", e);
        }
    }

    private void updateStopPlaceMapping() throws IOException {
        if (jbvCodeStopPlaceMappingUrl != null && !jbvCodeStopPlaceMappingUrl.isEmpty()) {
            logger.info("Fetching mapping-data from {}", jbvCodeStopPlaceMappingUrl);
            jbvCodeStopPlaceMappings.putAll(stopPlaceRegisterMappingFetcher.fetchStopPlaceMapping(jbvCodeStopPlaceMappingUrl));
        }
    }
}
