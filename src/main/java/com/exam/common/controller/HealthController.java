package com.exam.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
/***********************************
 *  URL      :  "/health"
 *  이름      :   healthz
 *  기능      :   헬스체크
 *  method   :   GET
 *  param    :
 *  result   :   Map<String, Object>
 ************************************/
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> healthz() {
        return Map.of("status", "ok");
    }
}
