package com.exam.admin.service.impl;

import com.exam.admin.dto.ProgramDTO;
import com.exam.admin.dto.RoleProgramDTO;
import com.exam.admin.mapper.ProgramMapper;
import com.exam.admin.service.ProgramService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public void updateRolePrograms(List<RoleProgramDTO> rolePrograms, String uptId, String uptIp) {
        // 안 바뀐 조합은 건드리지 않아야 최종수정자/일이 실제로 바뀐 것만 갱신됨 — 통째로 교체하지 않고
        // 이전 상태와 비교해서 바뀐 조합만 손댐. 체크 해제도 행을 지우지 않고 use_yn만 N으로 바꿔서
        // (소프트 삭제) 누가 언제 해제했는지 기록이 남게 함
        List<RoleProgramDTO> before = programMapper.findAllRolePrograms();
        Map<String, RoleProgramDTO> beforeByKey = before.stream()
                .collect(Collectors.toMap(this::key, rp -> rp));
        Set<String> afterKeys = rolePrograms.stream().map(this::key).collect(Collectors.toSet());

        for (RoleProgramDTO rp : before) {
            if ("Y".equals(rp.getUseYn()) && !afterKeys.contains(key(rp))) {
                rp.setUseYn("N");
                rp.setUptId(uptId);
                rp.setUptIp(uptIp);
                programMapper.updateRoleProgramUseYn(rp);
            }
        }
        for (RoleProgramDTO rp : rolePrograms) {
            RoleProgramDTO existing = beforeByKey.get(key(rp));
            rp.setUptId(uptId);
            rp.setUptIp(uptIp);
            if (existing == null) {
                rp.setInsId(uptId);
                rp.setInsIp(uptIp);
                programMapper.saveRoleProgram(rp);
            } else if (!"Y".equals(existing.getUseYn())) {
                rp.setUseYn("Y");
                programMapper.updateRoleProgramUseYn(rp);
            }
            // else: 이미 Y로 존재 — 그대로 둠(안 건드림)
        }
    }

    private String key(RoleProgramDTO rp) {
        return rp.getRoleId() + "-" + rp.getProgramId();
    }
}
