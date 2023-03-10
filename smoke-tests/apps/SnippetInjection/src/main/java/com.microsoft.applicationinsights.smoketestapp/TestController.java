// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

//  @GetMapping(path="/hello")
//  public String getHelloPage(){
//    return "hello.html";
//  }

  @GetMapping("/hello")
  public @ResponseBody String myPage() {
    return "<!DOCTYPE html>\n"
        + "<html lang=\"en\">\n"
        + "<head>\n"
        + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n"
        + "  <meta charset=\"UTF-8\">\n"
        + "  <title>Hello</title>\n"
        + "  <style>\n"
        + "    h1 {color:red;}\n"
        + "    p {color:blue;}\n"
        + "  </style>\n"
        + "</head>\n"
        + "<body>\n"
        + "<h1>h1 h1</h1>\n"
        + "<p>hello hello</p>\n"
        + "</body>\n"
        + "</html>\n";
  }
}
