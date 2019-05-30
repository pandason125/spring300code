package com.ylzinfo.demo.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ylzinfo.demo.service.DemoService;
import com.ylzinfo.mvcframework.annotation.ModelAutowired;
import com.ylzinfo.mvcframework.annotation.ModelController;
import com.ylzinfo.mvcframework.annotation.ModelRequestMapping;
import com.ylzinfo.mvcframework.annotation.ModelRequestParam;

@ModelController
@ModelRequestMapping("/demo")
public class DemoController {
	@ModelAutowired
	private DemoService demoservice;

	@ModelRequestMapping("/query.json")
	public void query(HttpServletRequest req, HttpServletResponse resp,@ModelRequestParam("name") String name){
		String result =demoservice.printname(name);
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
