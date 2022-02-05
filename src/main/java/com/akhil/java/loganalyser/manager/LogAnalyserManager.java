package com.akhil.java.loganalyser.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.akhil.java.loganalyser.conf.ApplicationData;
import com.akhil.java.loganalyser.model.Context;
import com.akhil.java.loganalyser.model.Event;
import com.akhil.java.loganalyser.model.State;
import com.akhil.java.loganalyser.model.persistence.Alert;
import com.akhil.java.loganalyser.repository.AlertRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class LogAnalyserManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAnalyserManager.class);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ApplicationData applicationData;

    public void parseAndPersistEvents(Context context) {
        
        Map<String, Event> eventMap = new HashMap<>();

        Map<String, Alert> alerts = new HashMap<>();

        LOGGER.info("Parsing the events and persisting the alerts. This may take a while...");
        try (LineIterator li = FileUtils.lineIterator(new ClassPathResource("samples/" + context.getLogFilePath()).getFile())) {
            String line = null;
            while (li.hasNext()) {
                Event event;
                try {
                    event = new ObjectMapper().readValue(li.nextLine(), Event.class);
                    LOGGER.trace("{}", event);

                    if (eventMap.containsKey(event.getId())) {
                        Event e1 = eventMap.get(event.getId());
                        long executionTime = getEventExecutionTime(event, e1);

                        Alert alert = new Alert(event, Math.toIntExact(executionTime));

                        if (executionTime > applicationData.getAlertThresholdMs()) {
                            alert.setAlert(Boolean.TRUE);
                            LOGGER.trace("!!! Execution time for the event {} is {}ms", event.getId(), executionTime);
                        }

                        alerts.put(event.getId(), alert);

                        eventMap.remove(event.getId());
                    } else {
                        eventMap.put(event.getId(), event);
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.error("Unable to parse the event! {}", e.getMessage());
                }

                if (alerts.size() > applicationData.getTableRowsWriteoffCount()) {
                    persistAlerts(alerts.values());
                    alerts = new HashMap<>();
                }
            } 
            if (alerts.size() != 0) {
                persistAlerts(alerts.values());
            }
        } catch (IOException e) {
            LOGGER.error("!!! Unable to access the file: {}", e.getMessage());
        }
    }

    private void persistAlerts(Collection<Alert> alerts) {
        LOGGER.debug("Persisting {} alerts...", alerts.size());
        alertRepository.saveAll(alerts);
    }

    private long getEventExecutionTime(Event event1, Event event2) {
        Event endEvent = Stream.of(event1, event2).filter(e -> State.FINISHED.equals(e.getState())).findFirst().orElse(null);
        Event startEvent = Stream.of(event1, event2).filter(e -> State.STARTED.equals(e.getState())).findFirst().orElse(null);
        return Objects.requireNonNull(endEvent).getTimestamp() - Objects.requireNonNull(startEvent).getTimestamp();
    }
}
