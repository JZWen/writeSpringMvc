package com.jzw.controller;

@MyController
@MyRequestMapping("")
public class Login {
	@MyRequestMapping("/index")
	public String index() {
		System.out.println("ok 进来了");
		return "newindex";
	}
}
