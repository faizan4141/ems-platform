package com.ems.tenant.service;

import com.ems.tenant.domain.*;
import com.ems.tenant.dto.*;
import com.ems.tenant.exception.*;
import com.ems.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository repository;

    public TenantService(TenantRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest req) {
        if (req.contractEndDate().isBefore(req.contractStartDate())) {
            throw new InvalidContractException("Contract end date must not be before start date");
        }
        if (repository.existsByCode(req.code())) {
            throw new DuplicateTenantException(req.code());
        }
        Tenant t = new Tenant();
        t.setCode(req.code());
        t.setName(req.name());
        t.setOrgType(req.orgType());
        t.setStatus(TenantStatus.ACTIVE);
        t.setContractStartDate(req.contractStartDate());
        t.setContractEndDate(req.contractEndDate());
        return TenantResponse.from(repository.save(t));
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return repository.findById(id)
                .map(TenantResponse::from)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return repository.findAll().stream().map(TenantResponse::from).toList();
    }

    @Transactional
    public TenantResponse updateStatus(UUID id, TenantStatus status) {
        Tenant t = repository.findById(id).orElseThrow(() -> new TenantNotFoundException(id));
        t.setStatus(status);
        return TenantResponse.from(t);
    }
}