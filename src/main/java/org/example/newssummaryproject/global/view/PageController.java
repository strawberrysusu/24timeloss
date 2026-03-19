package org.example.newssummaryproject.global.view;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping({"/", "/mypage"})
    public String index(HttpServletRequest request, Model model) {
        String initialPage = "/mypage".equals(request.getRequestURI()) ? "mypage" : "home";
        model.addAttribute("initialPage", initialPage);
        model.addAttribute("initialArticleId", "");
        return "index";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("initialPage", "detail");
        model.addAttribute("initialArticleId", id);
        return "index";
    }
}
