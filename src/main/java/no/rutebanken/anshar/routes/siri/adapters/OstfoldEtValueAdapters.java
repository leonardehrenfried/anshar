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

package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.processor.OstfoldIdPlatformPostProcessor;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.StopPlaceRegisterMapper;
import no.rutebanken.anshar.subscription.SubscriptionSetup;
import uk.org.siri.siri20.DestinationRef;
import uk.org.siri.siri20.JourneyPlaceRefStructure;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="ostfoldet")
public class OstfoldEtValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(SubscriptionSetup subscriptionSetup) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();
        valueAdapters.add(new OstfoldIdPlatformPostProcessor(subscriptionSetup));

        valueAdapters.add(new StopPlaceRegisterMapper(
                subscriptionSetup.getSubscriptionType(),
                subscriptionSetup.getDatasetId(),
                JourneyPlaceRefStructure.class,
                subscriptionSetup.getIdMappingPrefixes(),
                "StopPlace"));

        valueAdapters.add(new StopPlaceRegisterMapper(
                subscriptionSetup.getSubscriptionType(),
                subscriptionSetup.getDatasetId(),
                DestinationRef.class,
                subscriptionSetup.getIdMappingPrefixes(),
                "StopPlace"));

        if (subscriptionSetup.getDatasetId() != null && !subscriptionSetup.getDatasetId().isEmpty()) {
            List<ValueAdapter> datasetPrefix = createIdPrefixAdapters(subscriptionSetup.getDatasetId());
            if (!subscriptionSetup.getMappingAdapters().containsAll(datasetPrefix)) {
                subscriptionSetup.getMappingAdapters().addAll(datasetPrefix);
            }
        }

        return valueAdapters;
    }
}
