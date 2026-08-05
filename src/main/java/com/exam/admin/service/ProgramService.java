package com.exam.admin.service;

import com.exam.admin.dto.ProgramDTO;
import com.exam.admin.dto.RoleProgramDTO;

import java.util.List;

public interface ProgramService {
    List<ProgramDTO> getPrograms();
    void createProgram(ProgramDTO programDTO);
    void updateProgram(ProgramDTO programDTO);
    void deleteProgram(Long programId);

    List<RoleProgramDTO> getRolePrograms();
    void updateRolePrograms(List<RoleProgramDTO> rolePrograms);
}
