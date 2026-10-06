package io.orion.app.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "app_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppDefinition {
    @Id
    private String appId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "app_id")
    private List<ViewDefinition> views = new ArrayList<>();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String configJson; // Store as JSON string for flexibility

    public Map<String, Object> getConfig() {
        // Simple JSON deserialization would go here (e.g. via Jackson)
        return Collections.emptyMap();
    }

    public void setConfig(Map<String, Object> config) {
        // Simple JSON serialization would go here
        this.configJson = config.toString();
    }

    // Explicit setters to avoid Lombok annotation processing issues
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public List<ViewDefinition> getViews() { return views; }
    public void setViews(List<ViewDefinition> views) { this.views = views; }
}
