package org.example.newssummaryproject.global.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/mypage", "/detail/{id}"})
    public String index() {
        return "forward:/react/index.html";
    }
}
