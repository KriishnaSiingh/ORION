package io.orion.app.system;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemInfoController {

    private final String appName;

    public SystemInfoController(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "name", appName,
            "status", "UP",
            "javaVersion", System.getProperty("java.version"),
            "timestamp", Instant.now().toString()
        );
    }
}
