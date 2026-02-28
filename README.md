# MealDB App (Java full-stack)

A small Spring Boot app that queries TheMealDB and returns the meal requiring the least number of ingredients.

Run:

```bash
mvn spring-boot:run
```

Endpoints:
- GET `/` - web UI (form accepts `search`, default `Arrabiata`)
- GET `/api/least-ingredients?search=Arrabiata` - JSON result

Postman: create a GET request to `http://localhost:8080/api/least-ingredients?search=Arrabiata`.
