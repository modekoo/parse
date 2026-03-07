package com.modekoo.parse.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Slf4j
public class ParseController {

    @GetMapping("/test")
    public ModelAndView helloWorld(){
        ModelAndView mav = new ModelAndView("Hello");
        log.info("/test");
        return mav;
    }

}
