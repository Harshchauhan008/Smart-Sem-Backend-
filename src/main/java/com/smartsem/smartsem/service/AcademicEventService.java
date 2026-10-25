
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.AcademicEvent;
import com.smartsem.smartsem.repository.AcademicEventRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AcademicEventService {

    private final AcademicEventRepository eventRepository;


    public AcademicEventService(
            AcademicEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }


    // Get all events for a semester
    public List<AcademicEvent> getEventsBySemester(
            Long semesterId) {

        return eventRepository.findBySemesterId(semesterId);
    }


    // Filter events by type
    public List<AcademicEvent> getEventsByType(
            Long semesterId,
            String eventType) {

        return eventRepository
                .findBySemesterIdAndEventType(
                        semesterId,
                        eventType
                );
    }


    // Create a new academic event
    public AcademicEvent createEvent(AcademicEvent event) {
        return eventRepository.save(event);
    }
}
