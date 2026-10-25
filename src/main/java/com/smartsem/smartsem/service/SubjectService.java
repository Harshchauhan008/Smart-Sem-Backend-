
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // Get all subjects for a semester
    public List<Subject> getSubjectsBySemester(Long semesterId) {
        return subjectRepository.findBySemesterId(semesterId);
    }

    // Create a subject without duplicating its name
    public Subject createSubject(Subject subject) {

        Optional<Subject> existingSubject =
                subjectRepository.findBySemesterIdAndNameIgnoreCase(
                        subject.getSemesterId(),
                        subject.getName().trim()
                );

        if (existingSubject.isPresent()) {
            return existingSubject.get();
        }

        subject.setName(subject.getName().trim());
        return subjectRepository.save(subject);
    }
}
