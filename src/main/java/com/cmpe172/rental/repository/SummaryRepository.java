package com.cmpe172.rental.repository;

import com.cmpe172.rental.dto.FeaturedEquipmentDto;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SummaryRepository {

    private static final RowMapper<FeaturedEquipmentDto> FEATURED = (rs, i) -> new FeaturedEquipmentDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("asset_tag"),
            rs.getBigDecimal("daily_rate"),
            rs.getString("business_name"));

    private final JdbcTemplate jdbc;

    public SummaryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long countProviders() {
        return count("SELECT count(*) FROM providers");
    }

    public long countActiveEquipment() {
        return count("SELECT count(*) FROM services WHERE status = 'ACTIVE'");
    }

    public long countAvailableSlots() {
        return count("""
                SELECT count(*) FROM availability_slots s
                WHERE s.status = 'OPEN'
                  AND NOT EXISTS (
                      SELECT 1 FROM appointments a
                      WHERE a.availability_slot_id = s.id
                        AND a.status IN ('PENDING', 'CONFIRMED'))
                """);
    }

    public List<FeaturedEquipmentDto> findFeatured(int limit) {
        return jdbc.query("""
                SELECT sv.id, sv.name, sv.category, sv.asset_tag, sv.daily_rate, p.business_name
                FROM services sv
                JOIN providers p ON p.id = sv.provider_id
                WHERE sv.status = 'ACTIVE'
                ORDER BY sv.id
                LIMIT ?
                """, FEATURED, limit);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
