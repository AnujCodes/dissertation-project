package com.intelligent.invoice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ProcessInvoice {
	
	public boolean checkForInvoiceId(String word) {
		List<String> list = new ArrayList<String>();
		list.add("invoice id");
		list.add("bill id");
		list.add("bill #");
		list.add("bill#");
		list.add("invoice number");
		list.add("invoiceid");
		list.add("invoice #");
		list.add("invoice#");
		return list.contains(word.toLowerCase());
	}
	
	public boolean checkForInvoiceDate(String word) {
		List<String> list = new ArrayList<String>();
		list.add("issue date");
		list.add("invoice date");
		list.add("invoice date :");
		list.add("order date");
		list.add("order date :");
		list.add("date");
		list.add("bill date");
		return list.contains(word.toLowerCase());
	}
	
	public boolean checkValidDate(String word) {
		List<String> regex = new ArrayList<String>();
		regex.add("^(1[0-2]|0[1-9])/(3[01]|[12][0-9]|0[1-9])/[0-9]{4}$");
		regex.add("^(1[0-2]|0[1-9])-(3[01]|[12][0-9]|0[1-9])-[0-9]{4}$");
		regex.add("^(3[01]|[12][0-9]|0[1-9])/(1[0-2]|0[1-9])/[0-9]{4}$");
		regex.add("^(3[01]|[12][0-9]|0[1-9])-(1[0-2]|0[1-9])-[0-9]{4}$");
		regex.add("^(1[0-2]|0[1-9]).(3[01]|[12][0-9]|0[1-9]).[0-9]{4}$");
		regex.add("^(3[01]|[12][0-9]|0[1-9]).(1[0-2]|0[1-9]).[0-9]{4}$");
		for(String reg : regex) {
			Pattern pattern = Pattern.compile(reg);
			Matcher matcher = pattern.matcher(word);
			if(matcher.matches()) {
				return true;
			}
		}
		return false;
		
	}
	
	public boolean checkForAmount(String word) {
		List<String> list = new ArrayList<String>();
		list.add("subtotal");
		list.add("total");
		list.add("total:");
		list.add("total amount");
		list.add("grand amount");
		list.add("payments");
		return list.contains(word.toLowerCase());
	}
	
	public String getCurrency(String word) {
		Matcher matcher = Pattern.compile("(\\D+)|(\\d+(?:\\.\\d+)?)").matcher(word);
		while(matcher.find()) {
			if(matcher.group(1) != null) {
				return matcher.group(1);
			}
		}
		return "";
	}
	
	public String getAmount(String word) {
		String amount = word.replaceAll("[^0-9.]", "");
//		Matcher matcher = Pattern.compile("(\\D+)|(\\d+(?:\\.\\d+)?)").matcher(word);
//		while(matcher.find()) {
//			if(matcher.group(2) != null) {
//				return matcher.group(2);
//			}
//		}
		return amount;
	}

}
