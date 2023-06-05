// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//package com.microsoft.applicationinsights.smoketestapp;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//
//@Controller
//public class NormalController {
//  @RequestMapping(path="/")
//  public String root() {
//    return "test.html";
//  }
//}
// both controller and restcontroller has the ability to return html
// in demo project, snippet injection could work in both
//  here, controller could make spring boot and snippet injection work, but have error in health check
// restcontroller could return html, but spring boot and snippet injection dose not work
