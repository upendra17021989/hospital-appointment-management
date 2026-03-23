package com.hospital.repository;

import com.hospital.model.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorScheduleRepo extends JpaRepository<DoctorSchedule, UUID> {
    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(UUID doctorId);
    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndIsActiveTrue(UUID doctorId, Integer dayOfWeek);
}
