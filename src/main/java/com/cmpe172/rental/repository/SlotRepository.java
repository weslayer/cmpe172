package com.cmpe172.rental.repository;

import com.cmpe172.rental.dto.SlotDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SlotRepository {

    // A slot is available when it is OPEN and has no active (PENDING/CONFIRMED) appointment.
    private static final String FROM_AVAILABLE = """
            FROM availability_slots s
            JOIN services sv ON sv.id = s.service_id
            JOIN providers p ON p.id = s.provider_id
            WHERE s.status = 'OPEN'
              AND NOT EXISTS (
                  SELECT 1 FROM appointments a
                  WHERE a.availability_slot_id = s.id
                    AND a.status IN ('PENDING', 'CONFIRMED'))
            """;

    private static final RowMapper<SlotDto> MAPPER = (rs, i) -> new SlotDto(
            rs.getLong("id"),
            rs.getLong("service_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("asset_tag"),
            rs.getLong("provider_id"),
            rs.getString("business_name"),
            rs.getBigDecimal("daily_rate"),
            rs.getObject("starts_at", OffsetDateTime.class),
            rs.getObject("ends_at", OffsetDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public SlotRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SlotDto> findAvailable(Long serviceId, Long providerId, LocalDate date, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = "SELECT s.id, s.service_id, sv.name, sv.category, sv.asset_tag, "
                + "s.provider_id, p.business_name, sv.daily_rate, s.starts_at, s.ends_at "
                + FROM_AVAILABLE + filters(serviceId, providerId, date, params)
                + " ORDER BY s.starts_at, s.id LIMIT :size OFFSET :offset";
        params.addValue("size", size);
        params.addValue("offset", (long) page * size);
        return jdbc.query(sql, params, MAPPER);
    }

    public long countAvailable(Long serviceId, Long providerId, LocalDate date) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = "SELECT count(*) " + FROM_AVAILABLE + filters(serviceId, providerId, date, params);
        Long n = jdbc.queryForObject(sql, params, Long.class);
        return n == null ? 0 : n;
    }

    private static String filters(Long serviceId, Long providerId, LocalDate date, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder();
        if (serviceId != null) {
            sb.append(" AND s.service_id = :serviceId");
            params.addValue("serviceId", serviceId);
        }
        if (providerId != null) {
            sb.append(" AND s.provider_id = :providerId");
            params.addValue("providerId", providerId);
        }
        if (date != null) {
            sb.append(" AND CAST(s.starts_at AS date) = :date");
            params.addValue("date", date);
        }
        return sb.toString();
    }
}
