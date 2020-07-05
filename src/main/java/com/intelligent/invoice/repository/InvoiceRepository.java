package com.intelligent.invoice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.intelligent.invoice.dto.InvoiceEntity;

@Repository
public interface InvoiceRepository extends CrudRepository<InvoiceEntity, Long>{

}
