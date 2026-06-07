package com.ecoroute.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DistanceService {

    private static final Logger log = LoggerFactory.getLogger(DistanceService.class);
    private final JdbcTemplate jdbcTemplate;

    public DistanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieve all unique cities from database lookups to populate the dropdown selects.
     */
    public List<String> getAllUniqueCities() {
        String sql = "SELECT DISTINCT city_a FROM distance_lookups UNION SELECT DISTINCT city_b FROM distance_lookups ORDER BY city_a";
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            log.error("Failed to retrieve unique cities from distance lookup table", e);
            return Arrays.asList("Bengaluru", "Mysuru", "Mumbai", "Chennai"); // Fallback defaults
        }
    }

    /**
     * Canonical alias used by ShipmentService and controllers.
     * Delegates to findDistance() which performs JdbcTemplate lookup + Dijkstra fallback.
     */
    public double getDistanceBetween(String origin, String destination) {
        return findDistance(origin, destination);
    }

    /**
     * Finds the shortest distance in kilometers between two cities.
     * Direct lookups are checked first. If none exist, Dijkstra's algorithm computes the shortest path.
     */
    public Double findDistance(String origin, String destination) {
        if (origin == null || destination == null || origin.trim().isEmpty() || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Origin and destination cities must be specified");
        }

        String orig = origin.trim();
        String dest = destination.trim();

        if (orig.equalsIgnoreCase(dest)) {
            return 0.0;
        }

        log.debug("Resolving distance between '{}' and '{}'", orig, dest);

        // 1. Direct Lookup Check
        String directSql = "SELECT distance_km FROM distance_lookups WHERE " +
                "((LOWER(city_a) = ? AND LOWER(city_b) = ?) OR (LOWER(city_a) = ? AND LOWER(city_b) = ?)) LIMIT 1";
        try {
            List<Double> directDist = jdbcTemplate.query(directSql,
                    (rs, rowNum) -> rs.getDouble("distance_km"),
                    orig.toLowerCase(), dest.toLowerCase(),
                    dest.toLowerCase(), orig.toLowerCase());

            if (!directDist.isEmpty()) {
                Double d = directDist.get(0);
                log.info("Direct route found: {} -> {} = {} km", orig, dest, d);
                return d;
            }
        } catch (Exception e) {
            log.error("Error performing direct distance lookup query via JdbcTemplate", e);
        }

        // 2. Shortest-Path Graph Resolution via Dijkstra
        log.info("Direct route not found. Resolving using Dijkstra pathfinder...");
        String allRoutesSql = "SELECT city_a, city_b, distance_km FROM distance_lookups";
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(allRoutesSql);
        } catch (Exception e) {
            log.error("Failed to query distance lookups table for graph routing", e);
            throw new IllegalStateException("Distance database lookup is currently unavailable", e);
        }

        // Build Graph
        Map<String, List<Edge>> graph = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String u = (String) row.get("city_a");
            String v = (String) row.get("city_b");
            Double dist = ((Number) row.get("distance_km")).doubleValue();

            graph.computeIfAbsent(u.toLowerCase(), k -> new ArrayList<>()).add(new Edge(v, dist));
            graph.computeIfAbsent(v.toLowerCase(), k -> new ArrayList<>()).add(new Edge(u, dist));
        }

        String startNode = orig.toLowerCase();
        String endNode = dest.toLowerCase();

        if (!graph.containsKey(startNode) || !graph.containsKey(endNode)) {
            throw new IllegalArgumentException("No shipping route found between " + orig + " and " + dest);
        }

        Map<String, Double> distances = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        pq.add(new Node(orig, 0.0));
        distances.put(startNode, 0.0);

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            Node curr = pq.poll();
            String u = curr.name.toLowerCase();

            if (u.equals(endNode)) {
                log.info("Shortest path resolved: {} -> {} = {} km", orig, dest, curr.distance);
                return curr.distance;
            }

            if (!visited.add(u)) {
                continue;
            }

            List<Edge> edges = graph.get(u);
            if (edges == null) continue;

            for (Edge edge : edges) {
                String v = edge.target;
                String vKey = v.toLowerCase();
                Double weight = edge.weight;

                Double newDist = curr.distance + weight;
                Double oldDist = distances.getOrDefault(vKey, Double.MAX_VALUE);

                if (newDist < oldDist) {
                    distances.put(vKey, newDist);
                    pq.add(new Node(v, newDist));
                }
            }
        }

        log.warn("No route could be resolved between {} and {}", orig, dest);
        throw new IllegalArgumentException("No shipping route found between " + orig + " and " + dest);
    }

    private static class Edge {
        String target;
        Double weight;

        Edge(String target, Double weight) {
            this.target = target;
            this.weight = weight;
        }
    }

    private static class Node {
        String name;
        Double distance;

        Node(String name, Double distance) {
            this.name = name;
            this.distance = distance;
        }
    }
}
