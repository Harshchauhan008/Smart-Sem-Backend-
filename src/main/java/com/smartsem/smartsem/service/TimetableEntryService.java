
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.TimetableEntry;
import com.smartsem.smartsem.repository.TimetableEntryRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TimetableEntryService {

    private final TimetableEntryRepository timetableEntryRepository;


    public TimetableEntryService(
            TimetableEntryRepository timetableEntryRepository) {

        this.timetableEntryRepository = timetableEntryRepository;
    }



    // Get timetable entries for a subject
    public List<TimetableEntry> getEntriesBySubject(Long subjectId) {

        return timetableEntryRepository.findBySubjectId(subjectId);
    }



    // Create a timetable entry
    public TimetableEntry createEntry(TimetableEntry entry) {

        return timetableEntryRepository.save(entry);
    }

}
