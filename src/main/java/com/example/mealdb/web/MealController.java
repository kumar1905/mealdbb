package com.example.mealdb.web;

import com.example.mealdb.service.MealService;
import com.example.mealdb.service.dto.MealResult;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MealController {
    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false, defaultValue = "Arrabiata") String search, Model model) {
        try {
            MealResult res = mealService.findMealWithLeastIngredients(search);
            model.addAttribute("result", res);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("search", search);
        return "index";
    }

    @GetMapping("/api/least-ingredients")
    @ResponseBody
    public ResponseEntity<MealResult> apiLeastIngredients(@RequestParam(required = false, defaultValue = "Arrabiata") String search) {
        try {
            MealResult res = mealService.findMealWithLeastIngredients(search);
            if (res == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
