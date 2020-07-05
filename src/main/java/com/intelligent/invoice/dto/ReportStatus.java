package com.intelligent.invoice.dto;

public enum ReportStatus {
	
	NEW("New Application"), IN_PROCESS("In Process"), APPROVED("Approved");
	
	private String status;
	
	ReportStatus(String status){
		this.status = status;
	}
	
	public String getStatus() {
		return status;
	}

}
