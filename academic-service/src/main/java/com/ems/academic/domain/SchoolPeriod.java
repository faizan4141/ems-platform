package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "periods")
public class SchoolPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "period_no", nullable = false) private int number;
    @Column(nullable = false, length = 30) private String name;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(name = "break_period", nullable = false) private boolean breakPeriod;

    protected SchoolPeriod() {}

    public SchoolPeriod(UUID tenantId, int number, String name, LocalTime startTime, LocalTime endTime, boolean breakPeriod) {
        this.tenantId = tenantId;
        update(number, name, startTime, endTime, breakPeriod);
    }

    public void update(int number, String name, LocalTime startTime, LocalTime endTime, boolean breakPeriod) {
        this.number = number;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakPeriod = breakPeriod;
    }

    public UUID getId() { return id; }
    public int getNumber() { return number; }
    public String getName() { return name; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isBreakPeriod() { return breakPeriod; }
}