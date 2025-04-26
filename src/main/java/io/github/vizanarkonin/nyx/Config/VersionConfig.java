package io.github.vizanarkonin.nyx.Config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A simple bean for exposing the version numbers to the process.
 * Primarily used to pass that data down to the model
 */
@Configuration
public class VersionConfig {
    
    @Value("${app.version}")
    private String nyxVersion;
    
    @Value("${keres.version}")
    private String keresVersion;
    
    @Bean
    public Map<String, String> versionInfo() {
        Map<String, String> versions = new HashMap<>();
        versions.put("nyxVersion", nyxVersion);
        versions.put("keresVersion", keresVersion);
        return versions;
    }
}
