package com.intelligent.invoice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.intelligent.invoice.dto.InvoiceReport;

@Repository
public interface ReportRepository extends CrudRepository<InvoiceReport, Long>{

}
