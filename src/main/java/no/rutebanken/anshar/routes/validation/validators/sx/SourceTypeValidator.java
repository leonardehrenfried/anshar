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

package no.rutebanken.anshar.routes.validation.validators.sx;

import com.google.common.collect.Sets;
import no.rutebanken.anshar.routes.validation.validators.LimitedSubsetValidator;
import no.rutebanken.anshar.routes.validation.validators.Validator;
import no.rutebanken.anshar.subscription.SiriDataType;
import org.springframework.stereotype.Component;
import uk.org.siri.siri21.SituationSourceTypeEnumeration;

import static no.rutebanken.anshar.routes.validation.validators.Constants.PT_SITUATION_ELEMENT;


/**
 * Verifies that the value for field SourceType is of the allowed type
 *
 */
@Validator(profileName = "norway", targetType = SiriDataType.SITUATION_EXCHANGE)
@Component
public class SourceTypeValidator extends LimitedSubsetValidator {


    private String path;

    public SourceTypeValidator() {
        FIELDNAME = "SourceType";
        path = PT_SITUATION_ELEMENT + "/Source/" + FIELDNAME;
        expectedValues = Sets.newHashSet(SituationSourceTypeEnumeration.DIRECT_REPORT.value());
    }

    @Override
    public String getXpath() {
        return path;
    }

}
