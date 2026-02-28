package com.example.mealdb.service;

import com.example.mealdb.service.dto.MealResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MealService {
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public MealResult findMealWithLeastIngredients(String search) throws Exception {
        String q = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        String url = "https://www.themealdb.com/api/json/v1/1/search.php?s=" + q;
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode meals = root.get("meals");
        if (meals == null || !meals.isArray() || meals.size() == 0) return null;

        MealResult best = null;

        for (JsonNode meal : meals) {
            List<String> ingredients = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                String ingField = "strIngredient" + i;
                JsonNode ingNode = meal.get(ingField);
                if (ingNode != null) {
                    String ing = ingNode.asText(null);
                    if (ing != null && !ing.isBlank()) {
                        ingredients.add(ing.trim());
                    }
                }
            }

            int count = ingredients.size();

            if (best == null || count < best.getIngredientCount()) {
                MealResult r = new MealResult();
                r.setId(meal.path("idMeal").asText(null));
                r.setName(meal.path("strMeal").asText(null));
                r.setInstructions(meal.path("strInstructions").asText(null));
                r.setThumbnail(meal.path("strMealThumb").asText(null));
                r.setIngredientCount(count);
                r.setIngredients(ingredients);
                best = r;
            }
        }

        return best;
    }
}
