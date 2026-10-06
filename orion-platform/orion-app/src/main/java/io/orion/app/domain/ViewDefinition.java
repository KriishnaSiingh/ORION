package io.orion.app.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "view_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String viewId;

    @Column(nullable = false)
    private String type; // TABLE, DETAIL, GRAPH

    @Column(nullable = false)
    private String sourceObjectType;

    @ElementCollection
    private java.util.List<String> columns = new java.util.ArrayList<>();

    @ElementCollection
    private java.util.List<String> links = new java.util.ArrayList<>();

    // Explicit getter/setter to avoid Lombok annotation processing issues
    public String getViewId() { return viewId; }
    public void setViewId(String viewId) { this.viewId = viewId; }
    public String getSourceObjectType() { return sourceObjectType; }
    public void setSourceObjectType(String sourceObjectType) { this.sourceObjectType = sourceObjectType; }
}
