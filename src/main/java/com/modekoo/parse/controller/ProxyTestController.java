package com.modekoo.parse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ProxyTestController {

    @GetMapping("proxy/test")
    public ModelAndView test(){
        ModelAndView mav = new ModelAndView("Hello");
        return mav;
    }

}
