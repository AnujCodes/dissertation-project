package com.intelligent.invoice.dto;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class VisionResultEntity {
	
	@SerializedName("0")
	private List<List<String>> response;

	public VisionResultEntity(List<List<String>> response) {
		super();
		this.response = response;
	}

	public List<List<String>> getResponse() {
		return response;
	}

	public void setResponse(List<List<String>> response) {
		this.response = response;
	}

	

}
