package io.orion.graph.web;

import io.orion.graph.application.GraphAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/graph/analytics")
public class AnalyticsController {
    private final GraphAnalyticsService analyticsService;

    public AnalyticsController(GraphAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/activity")
    public List<Map<String, Object>> getActivityData() {
        // Mocking a time-series for the dashboard chart
        // In a real system, this would query an audit log or temporal table
        List<Map<String, Object>> data = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Random random = new Random();
        for (String day : days) {
            Map<String, Object> point = new HashMap<>();
            point.put("day", day);
            point.put("objects", 100 + random.nextInt(500));
            point.put("searches", 200 + random.nextInt(800));
            data.add(point);
        }
        return data;
    }

    @PostMapping("/pagerank")
    public Map<String, Double> getInfluenceScores() {
        return analyticsService.computeInfluenceScores();
    }

    @PostMapping("/communities")
    public Map<String, Long> getCommunities() {
        return analyticsService.detectCommunities();
    }
}
