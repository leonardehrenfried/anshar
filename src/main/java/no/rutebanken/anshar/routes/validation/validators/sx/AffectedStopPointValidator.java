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

import no.rutebanken.anshar.routes.validation.validators.CustomValidator;
import no.rutebanken.anshar.routes.validation.validators.Validator;
import no.rutebanken.anshar.subscription.SiriDataType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import javax.xml.bind.ValidationEvent;

import static no.rutebanken.anshar.routes.validation.validators.Constants.AFFECTED_STOP_POINT;

/**
 * Verifies that the value for field StopPointRefStructure is built up correctly
 *
 */
@Validator(profileName = "norway", targetType = SiriDataType.SITUATION_EXCHANGE)
@Component
public class AffectedStopPointValidator extends CustomValidator {

    private String path;

    private String FIELDNAME;

    public AffectedStopPointValidator() {
        FIELDNAME = "StopPointRef";
        path = AFFECTED_STOP_POINT + FIELD_DELIMITER + FIELDNAME;
    }

    @Override
    public String getXpath() {
        return path;
    }

    @Override
    public ValidationEvent isValid(Node node) {
        String nodeValue = getNodeValue(node);

        final boolean validQuayRef = isValidNsrId("NSR:Quay:", nodeValue);
        final boolean validStopPlaceRef = isValidNsrId("NSR:StopPlace:", nodeValue);

        if (!validQuayRef && !validStopPlaceRef) {
            return createEvent(node, FIELDNAME, "NSR:Quay:ID or NSR:StopPlace:ID", nodeValue, ValidationEvent.FATAL_ERROR);
        }
        if (!idExists(nodeValue)) {
            return createCustomFieldEvent(node, "The ID ´" + nodeValue + "` does not exist in NSR.", ValidationEvent.FATAL_ERROR);
        }
        return null;
    }
}
