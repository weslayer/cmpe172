package com.cmpe172.rental.repository;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.model.Appointment;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppointmentRepository {

    // COMPLETED is derived on read: a BOOKED appointment whose slot has already ended.
    private static final String SELECT_DTO = """
            SELECT a.id,
                   CASE WHEN a.status = 'BOOKED' AND s.ends_at <= now() THEN 'COMPLETED' ELSE a.status END AS status,
                   s.id AS slot_id, s.starts_at, s.ends_at, sv.name, sv.asset_tag, p.business_name,
                   u.full_name, u.email
            FROM appointments a
            JOIN availability_slots s ON s.id = a.availability_slot_id
            JOIN services sv ON sv.id = s.service_id
            JOIN providers p ON p.id = sv.provider_id
            JOIN users u ON u.id = a.customer_id
            """;

    private static final RowMapper<AppointmentDto> DTO = (rs, i) -> new AppointmentDto(
            rs.getLong("id"),
            rs.getString("status"),
            rs.getLong("slot_id"),
            rs.getObject("starts_at", OffsetDateTime.class),
            rs.getObject("ends_at", OffsetDateTime.class),
            rs.getString("name"),
            rs.getString("asset_tag"),
            rs.getString("business_name"),
            rs.getString("full_name"),
            rs.getString("email"));

    private static final RowMapper<Appointment> LOCKED = (rs, i) -> new Appointment(
            rs.getLong("customer_id"),
            rs.getString("status"),
            rs.getObject("starts_at", OffsetDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    public AppointmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AppointmentDto get(long id) {
        return jdbc.queryForObject(SELECT_DTO + " WHERE a.id = :id", Map.of("id", id), DTO);
    }

    public List<AppointmentDto> findByCustomer(long customerId) {
        return jdbc.query(SELECT_DTO + " WHERE a.customer_id = :id ORDER BY s.starts_at, a.id",
                Map.of("id", customerId), DTO);
    }

    public List<AppointmentDto> findByProvider(long providerId) {
        return jdbc.query(SELECT_DTO + " WHERE sv.provider_id = :id ORDER BY s.starts_at, a.id",
                Map.of("id", providerId), DTO);
    }

    public boolean existsBooked(long slotId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM appointments WHERE availability_slot_id = :id AND status = 'BOOKED')
                """, Map.of("id", slotId), Boolean.class));
    }

    // Status defaults to BOOKED; the partial unique index rejects a second BOOKED row for the slot.
    public long insertBooked(long slotId, long customerId) {
        return jdbc.queryForObject("""
                INSERT INTO appointments (availability_slot_id, customer_id)
                VALUES (:slotId, :customerId)
                RETURNING id
                """, Map.of("slotId", slotId, "customerId", customerId), Long.class);
    }

    public Optional<Appointment> lock(long id) {
        return jdbc.query("""
                SELECT a.customer_id, a.status, s.starts_at
                FROM appointments a
                JOIN availability_slots s ON s.id = a.availability_slot_id
                WHERE a.id = :id
                FOR UPDATE OF a
                """, Map.of("id", id), LOCKED).stream().findFirst();
    }

    public void markCancelled(long id) {
        jdbc.update("UPDATE appointments SET status = 'CANCELLED' WHERE id = :id", Map.of("id", id));
    }
}
