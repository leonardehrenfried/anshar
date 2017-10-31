package no.rutebanken.anshar.messages;

import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import uk.org.siri.siri20.ProductionTimetableDeliveryStructure;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
public class ProductionTimetables implements SiriRepository<ProductionTimetableDeliveryStructure> {
    private Logger logger = LoggerFactory.getLogger(ProductionTimetables.class);

    @Autowired
    private IMap<String, ProductionTimetableDeliveryStructure> timetableDeliveries;

    @Autowired
    @Qualifier("getProductionTimetableChangesMap")
    private IMap<String, Set<String>> changesMap;

    @Autowired
    @Qualifier("getLastPtUpdateRequest")
    private IMap<String, Instant> lastUpdateRequested;

    /**
     * @return All PT-elements
     */
    public Collection<ProductionTimetableDeliveryStructure> getAll() {
        return timetableDeliveries.values();
    }

    public int getSize() {
        return timetableDeliveries.size();
    }

    public Collection<ProductionTimetableDeliveryStructure> getAll(String datasetId) {
        if (datasetId == null) {
            return getAll();
        }        
        Map<String, ProductionTimetableDeliveryStructure> datasetIdSpecific = new HashMap<>();
        timetableDeliveries.keySet().stream().filter(key -> key.startsWith(datasetId + ":")).forEach(key -> {
            ProductionTimetableDeliveryStructure element = timetableDeliveries.get(key);
            if (element != null) {
                datasetIdSpecific.put(key, element);
            }
        });

        return new ArrayList<>(datasetIdSpecific.values());
    }


    public Collection<ProductionTimetableDeliveryStructure> getAllUpdates(String requestorId, String datasetId) {
        if (requestorId != null) {

            Set<String> idSet = changesMap.get(requestorId);
            lastUpdateRequested.set(requestorId, Instant.now(), trackingPeriodMinutes, TimeUnit.MINUTES);
            if (idSet != null) {
                Set<String> datasetFilteredIdSet = new HashSet<>();

                if (datasetId != null) {
                    idSet.stream().filter(key -> key.startsWith(datasetId + ":")).forEach(key -> {
                        datasetFilteredIdSet.add(key);
                    });
                } else {
                    datasetFilteredIdSet.addAll(idSet);
                }

                Collection<ProductionTimetableDeliveryStructure> changes = timetableDeliveries.getAll(datasetFilteredIdSet).values();

                Set<String> existingSet = changesMap.get(requestorId);
                if (existingSet == null) {
                    existingSet = new HashSet<>();
                }
                existingSet.removeAll(idSet);
                changesMap.set(requestorId, existingSet);


                logger.info("Returning {} changes to requestorRef {}", changes.size(), requestorId);
                return changes;
            } else {

                logger.info("Returning all to requestorRef {}", requestorId);
                changesMap.set(requestorId, new HashSet<>());
            }
        }

        return getAll(datasetId);
    }

    @Override
    public int cleanup() {
        long t1 = System.currentTimeMillis();
        Set<String> keysToRemove = new HashSet<>();
        timetableDeliveries.keySet()
                .stream()
                .forEach(key -> {
                    if (getExpiration(timetableDeliveries.get(key)) < 0) {
                        keysToRemove.add(key);
                    }
                });

        logger.info("Cleanup removed {} expired elements in {} seconds.", keysToRemove.size(), (int)(System.currentTimeMillis()-t1)/1000);
        keysToRemove.forEach(key -> timetableDeliveries.delete(key));
        return keysToRemove.size();
    }
    public long getExpiration(ProductionTimetableDeliveryStructure s) {

        ZonedDateTime validUntil = s.getValidUntil();
        if (validUntil != null) {
            return ZonedDateTime.now().until(validUntil, ChronoUnit.MILLIS);
        }

        return -1;
    }

    public Collection<ProductionTimetableDeliveryStructure> addAll(String datasetId, List<ProductionTimetableDeliveryStructure> ptList) {

        Set<String> changes = new HashSet<>();

        ptList.forEach(pt -> {
            String key = createKey(datasetId, pt);


            ProductionTimetableDeliveryStructure existing = timetableDeliveries.get(key);

            if (existing == null || pt.getResponseTimestamp().isAfter(existing.getResponseTimestamp())) {

                long expiration = getExpiration(pt);

                if (expiration > 0) {
                    changes.add(key);
                    timetableDeliveries.set(key, pt, expiration, TimeUnit.MILLISECONDS);
                }
            } else {
                //Newer update has already been processed
            }
        });

        changesMap.keySet().forEach(requestor -> {
            if (lastUpdateRequested.get(requestor) != null) {
                Set<String> tmpChanges = changesMap.get(requestor);
                tmpChanges.addAll(changes);
                changesMap.set(requestor, tmpChanges);
            } else {
                changesMap.remove(requestor);
            }
        });

        return timetableDeliveries.getAll(changes).values();
    }

    public ProductionTimetableDeliveryStructure add(String datasetId, ProductionTimetableDeliveryStructure timetableDelivery) {
        if (timetableDelivery == null) {
            return null;
        }

        List<ProductionTimetableDeliveryStructure> ptList = new ArrayList<>();
        ptList.add(timetableDelivery);
        addAll(datasetId, ptList);
        return timetableDeliveries.get(createKey(datasetId, timetableDelivery));
    }
    private String createKey(String datasetId, ProductionTimetableDeliveryStructure element) {
        StringBuffer key = new StringBuffer();

        key.append(datasetId).append(":")
                .append(element.getVersion());
        return key.toString();
    }
}
