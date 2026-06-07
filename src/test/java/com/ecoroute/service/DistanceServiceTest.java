package com.ecoroute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DistanceServiceTest {

    private JdbcTemplate jdbcTemplate;
    private DistanceService distanceService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        distanceService = new DistanceService(jdbcTemplate);
    }

    @Test
    void testFindDistance_SameCity() {
        Double distance = distanceService.findDistance("Bengaluru", "Bengaluru");
        assertEquals(0.0, distance);
    }

    @Test
    void testFindDistance_DirectRoute() {
        // Mock direct lookup query
        List<Double> directResult = Collections.singletonList(145.0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(directResult);

        Double distance = distanceService.findDistance("Bengaluru", "Mysuru");
        assertEquals(145.0, distance);
    }

    @Test
    void testFindDistance_MultiHopRoute() {
        // Direct lookup returns empty to trigger Dijkstra
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Mock database graph query results
        List<Map<String, Object>> graphData = new ArrayList<>();
        graphData.add(createRouteRow("Bengaluru", "Mysuru", 145.0));
        graphData.add(createRouteRow("Bengaluru", "Mumbai", 980.5));
        graphData.add(createRouteRow("Chennai", "Bengaluru", 350.2));

        when(jdbcTemplate.queryForList(anyString())).thenReturn(graphData);

        // Calculate Chennai to Mysuru (via Bengaluru: 350.2 + 145.0 = 495.2)
        Double distance = distanceService.findDistance("Chennai", "Mysuru");
        assertEquals(495.2, distance);
    }

    @Test
    void testFindDistance_NoRouteFound() {
        // Direct lookup returns empty
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Mock database graph query results (Delhi and Kolkata are disjoint from Bengaluru)
        List<Map<String, Object>> graphData = new ArrayList<>();
        graphData.add(createRouteRow("Bengaluru", "Mysuru", 145.0));
        graphData.add(createRouteRow("Delhi", "Kolkata", 1500.0));

        when(jdbcTemplate.queryForList(anyString())).thenReturn(graphData);

        assertThrows(IllegalArgumentException.class, () -> {
            distanceService.findDistance("Chennai", "Delhi");
        });
    }

    private Map<String, Object> createRouteRow(String cityA, String cityB, Double dist) {
        Map<String, Object> row = new HashMap<>();
        row.put("city_a", cityA);
        row.put("city_b", cityB);
        row.put("distance_km", dist);
        return row;
    }
}
