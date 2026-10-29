package com.ems.academic.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "class_id", nullable = false, updatable = false) private UUID classId;
    @Column(nullable = false, length = 20) private String name;
    private Integer capacity;

    protected Section() {}

    public Section(UUID tenantId, UUID classId, String name, Integer capacity) {
        this.tenantId = tenantId;
        this.classId = classId;
        this.name = name;
        this.capacity = capacity;
    }

    public UUID getId() { return id; }
    public UUID getClassId() { return classId; }
    public String getName() { return name; }
    public Integer getCapacity() { return capacity; }
}