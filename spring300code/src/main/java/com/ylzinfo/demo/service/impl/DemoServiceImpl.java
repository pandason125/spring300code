package com.ylzinfo.demo.service.impl;

import com.ylzinfo.demo.service.DemoService;
import com.ylzinfo.mvcframework.annotation.ModelService;

@ModelService
public class DemoServiceImpl implements DemoService{

	public DemoServiceImpl() {
		// TODO Auto-generated constructor stub
	}

	public String printname(String name) {
		// TODO Auto-generated method stub
		return "hello "+name;
	}

}
