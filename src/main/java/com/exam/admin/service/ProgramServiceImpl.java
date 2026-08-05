package com.exam.admin.service;

import com.exam.admin.dto.ProgramDTO;
import com.exam.admin.dto.RoleProgramDTO;
import com.exam.admin.mapper.ProgramMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProgramServiceImpl implements ProgramService {

    private final ProgramMapper programMapper;

    public ProgramServiceImpl(ProgramMapper programMapper) {
        this.programMapper = programMapper;
    }

    @Override
    public List<ProgramDTO> getPrograms() {
        return programMapper.findAll();
    }

    @Override
    public void createProgram(ProgramDTO programDTO) {
        programMapper.save(programDTO);
    }

    @Override
    public void updateProgram(ProgramDTO programDTO) {
        programMapper.updateProgram(programDTO);
    }

    @Override
    public void deleteProgram(Long programId) {
        programMapper.deleteRoleProgramsByProgramId(programId);
        programMapper.deleteProgram(programId);
    }

    @Override
    public List<RoleProgramDTO> getRolePrograms() {
        return programMapper.findAllRolePrograms();
    }

    @Override
    public void updateRolePrograms(List<RoleProgramDTO> rolePrograms) {
        // 그리드 저장 = 현재 체크 상태가 곧 전체 최종 상태이므로 통째로 교체
        programMapper.deleteAllRolePrograms();
        for (RoleProgramDTO rp : rolePrograms) {
            programMapper.saveRoleProgram(rp);
        }
    }
}
