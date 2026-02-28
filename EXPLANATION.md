# Technical Workflow & Code Walkthrough

This file describes the architecture and walks through the main classes.

1) Workflow
- The UI or API endpoint accepts a `search` parameter (example: `Arrabiata`).
- The `MealService` calls TheMealDB API (`search.php?s=...`) and parses the JSON response.
- For each meal returned, `MealService` counts `strIngredient1..strIngredient20` that are non-empty.
- The meal with the smallest ingredient count is chosen and returned as a `MealResult` DTO.

2) Key files
- `src/main/java/com/example/mealdb/service/MealService.java` — core logic that fetches JSON and computes the least-ingredient meal.
- `src/main/java/com/example/mealdb/web/MealController.java` — provides `/` for the Thymeleaf UI and `/api/least-ingredients` for JSON.
- `src/main/resources/templates/index.html` — simple server-side UI with a search form.

3) How to test with Postman
- Start the app: `mvn spring-boot:run`
- In Postman create a GET request to: `http://localhost:8080/api/least-ingredients?search=Arrabiata`
- Inspect JSON response which includes `name`, `ingredientCount`, `ingredients`.

4) Notes
- The implementation is resilient to missing fields: it inspects the JSON tree rather than requiring a strict POJO with 20 fields.
- The app uses server-side rendering to keep the stack purely Java (Spring Boot + Thymeleaf).
