
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.repository.SemesterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    public List<Semester> getSemestersByUser(Long userId) {
        return semesterRepository.findByUserId(userId);
    }

    public Semester createSemester(Semester semester) {
        return semesterRepository.save(semester);
    }
}
