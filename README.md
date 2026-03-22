# FSE — FatSecret Nutrition Tracker

Android‑приложение (API 36) для трекинга питания на основе [FatSecret Platform API](https://platform.fatsecret.com/docs/guides).

## Функции

- **Дневник питания** — логирование еды по приёмам пищи
- **Поиск еды** — поиск по базе FatSecret и добавление в дневник
- **Рецепты** — поиск рецептов FatSecret
- **Избранное** — сохранение любимых продуктов
- **Недавние** — список недавно добавленных продуктов
- **Нормы** — дневные нормы по макро‑ и микронутриентам; при нажатии на нутриент — список избранных продуктов, богатых им

## Настройка

1. Зарегистрируйтесь на [FatSecret Platform](https://platform.fatsecret.com/register) и получите Client ID и Client Secret.

2. Добавьте в `local.properties`:
   ```properties
   fatsecret_client_id=YOUR_CLIENT_ID
   fatsecret_client_secret=YOUR_CLIENT_SECRET
   ```
   Пример есть в `local.properties.example`.

3. **Важно:** FatSecret выдает OAuth 2.0 токены только с разрешённых IP. Для приложения используйте Backend for Frontend (BFF) или укажите IP вашего сервера в личном кабинете FatSecret.

## Сборка

```bash
./gradlew :app:assembleDebug
```

## Структура

- `data/api` — FatSecret REST API, DTO, маппинг
- `data/auth` — OAuth 2.0, TokenStore
- `data/local` — DataStore для недавних, избранного, дневника
- `data/repository` — FoodRepository, RecipeRepository
- `domain/model` — Food, Serving, Recipe, Nutrient, DiaryEntry
- `ui/screens` — Diary, Search, Recipes, Favorites, Norms

## Стек

Compose, Material 3, Navigation, Retrofit, Kotlinx Serialization, DataStore, Coil
