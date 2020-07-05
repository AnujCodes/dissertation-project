package com.intelligent.invoice.dto;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "Invoice")
public class InvoiceEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	private String invoiceId;
	
	private String invoiceDate;
	
	private String vendor;
	
	private String totalAmount;
	
	private String currency;
	
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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		Map<String,String> currencyMap = new HashMap<String,String>();
		currencyMap.put("$", "USD");
		currencyMap.put("£", "GBP");
		currencyMap.put("₹", "INR");
		currencyMap.put("€", "EUR");
		currencyMap.put("¥", "YEN");
		if(currencyMap.containsKey(currency.replaceAll("\\s", ""))) {
			this.currency = currencyMap.get(currency);
		} else {
			this.currency = currency;
		}
	}

	public InvoiceEntity(String invoiceId, String invoiceDate, String vendor, String totalAmount,
			String currency) {
		super();
		this.invoiceId = invoiceId;
		this.invoiceDate = invoiceDate;
		this.vendor = vendor;
		this.totalAmount = totalAmount;
		this.currency = currency;
	}
	
	public InvoiceEntity() {
		super();
	}
	

}
