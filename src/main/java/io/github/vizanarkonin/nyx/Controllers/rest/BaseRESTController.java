package io.github.vizanarkonin.nyx.Controllers.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("")
public class BaseRESTController {

    /**
     * This endpoint is used to keep session alive for Socket-IO powered pages.
     * @param session   - HttpSession instance
     * @return          - ResponseEntity with OK code
     */
    @GetMapping("/keep-alive")
    public ResponseEntity<?> keepAlive(HttpSession session) {
        return ResponseEntity.ok().build();
    }
}
