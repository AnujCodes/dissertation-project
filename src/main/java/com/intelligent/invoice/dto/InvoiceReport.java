package com.intelligent.invoice.dto;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "Invoice_Report")
public class InvoiceReport {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	private String employeeId;
	
	private String invoiceId;
	
	private String invoiceDate;
	
	private String vendor;
	
	private String totalAmount;
	
	private String category;
	
	private String currency;
	
	private boolean reimbursable;
	
	private ReportStatus status;
	
	public String getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(String invoiceId) {
		this.invoiceId = invoiceId;
	}

	public String getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(String invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public boolean isReimbursable() {
		return reimbursable;
	}

	public void setReimbursable(boolean reimbursable) {
		this.reimbursable = reimbursable;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public ReportStatus getStatus() {
		return status;
	}

	public void setStatus(ReportStatus status) {
		this.status = status;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public void setCurrency(String currency) {
		Map<String,String> currencyMap = new HashMap<String,String>();
		currencyMap.put("$", "USD");
		currencyMap.put("£", "GBP");
		currencyMap.put("₹", "INR");
		currencyMap.put("€", "EUR");
		currencyMap.put("¥", "YEN");
		if(currencyMap.containsKey(currency)) {
			this.currency = currencyMap.get(currency);
		}
		this.currency = currency;
	}

	public InvoiceReport(long id, String employeeId, String invoiceId, String invoiceDate, String vendor, String totalAmount, String category,
			String currency, boolean reimbursable, ReportStatus status) {
		super();
		this.id = id;
		this.employeeId = employeeId;
		this.invoiceId = invoiceId;
		this.invoiceDate = invoiceDate;
		this.vendor = vendor;
		this.totalAmount = totalAmount;
		this.category = category;
		this.currency = currency;
		this.reimbursable = reimbursable;
		this.status = status;
	}
	
	public InvoiceReport() {
		super();
	}
	

}
