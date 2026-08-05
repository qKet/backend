package com.exam.admin.mapper;

import com.exam.admin.dto.ProgramDTO;
import com.exam.admin.dto.RoleProgramDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProgramMapper {
    List<ProgramDTO> findAll();
    int save(ProgramDTO programDTO);
    int updateProgram(ProgramDTO programDTO);
    int deleteProgram(Long programId);

    List<RoleProgramDTO> findAllRolePrograms();
    int deleteRoleProgramsByProgramId(Long programId);
    int deleteAllRolePrograms();
    int saveRoleProgram(RoleProgramDTO roleProgramDTO);
}
