# Keeply

App Android **local-first** para inventario del hogar: productos, stock, caducidades, listas de
compra, alertas y un resumen mensual basado en reglas (sin IA).

- **Package:** `com.gabow95k.keeply`
- **Min SDK:** 29 · **Target SDK:** 36 · **Compile SDK:** 37
- **UI:** XML + ViewBinding + Material 3
- **Navegación:** `FragmentManager` + bottom navigation (sin Navigation Component)

---

## Tecnologías

| Área           | Tecnología                                                          |
|----------------|---------------------------------------------------------------------|
| Lenguaje       | Kotlin                                                              |
| Build          | Gradle (Kotlin DSL) + Version Catalog (`gradle/libs.versions.toml`) |
| UI             | AppCompat, Material, ConstraintLayout, RecyclerView, ViewBinding    |
| Async          | Coroutines + Flow (`lifecycle-runtime-ktx`)                         |
| Persistencia   | Room (`keeply.db`, versión actual **3**)                            |
| Preferencias   | `SharedPreferences` vía `KeeplyPreferences`                         |
| Background     | WorkManager (`InventoryAlertWorker`)                                |
| Imágenes       | Glide                                                               |
| Escáner        | Play Services Code Scanner (código de barras)                       |
| OCR / etiqueta | ML Kit Text Recognition + Barcode Scanning                          |
| Tipografía     | Onest (`res/font`)                                                  |

Dependencias principales en `app/build.gradle.kts`.

---

## Arquitectura

Keeply sigue una arquitectura **por capas**, ligera (sin Hilt/Navigation), orientada a features en
`presentation/` y lógica de dominio/utilidades en paquetes dedicados.

```
┌─────────────────────────────────────────┐
│  presentation/  (UI: Activities,        │
│  Fragments, Adapters, Swipe callbacks)  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  domain/model + insights/ + shopping/   │
│  + prompts/ + notifications/ + scanner/ │
│  (reglas, generación, evaluadores)      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  data/  (Room entities/DAO/DB,          │
│  mappers, preferences, StockChangeLogger)│
└─────────────────────────────────────────┘
```

### Flujo de arranque

1. `SplashActivity` (launcher)
2. Gate de privacidad si no se ha aceptado la versión actual → `PrivacyPolicyActivity`
3. Shell principal → `ControllerActivity` (tabs)

### Navegación (tabs)

`ControllerActivity` hace **hide/show** de fragments en `R.id.fragmentContainer`:

| Tab        | Destino                 |
|------------|-------------------------|
| Inicio     | `HomeFragment`          |
| Inventario | `BotiquinFragment`      |
| Compras    | `ShoppingListsFragment` |
| Ajustes    | `SettingsFragment`      |

Pantallas “push” (producto, detalle de lista, editar perfil) usan `replace` + `addToBackStack` sobre
el mismo contenedor. Cambiar de tab limpia el back stack.

### Capas y paquetes

```
app/src/main/java/com/gabow95k/keeply/
├── KeeplyApplication.kt          # Canales de notificación + schedule WorkManager
├── presentation/
│   ├── base/                     # BaseActivity / BaseFragment (ViewBinding)
│   ├── splash/
│   ├── privacy/
│   ├── controller/               # Shell + bottom nav
│   ├── home/
│   ├── botiquin/                 # Inventario (nombre histórico del package)
│   ├── shopping/
│   └── settings/
├── data/
│   ├── local/
│   │   ├── db/                   # KeeplyDatabase + migraciones + seeds
│   │   ├── entity/
│   │   ├── dao/
│   │   ├── mapper/
│   │   └── StockChangeLogger.kt
│   └── preferences/              # KeeplyPreferences, LookupOptionsStore, schedules
├── domain/model/                 # Modelos de dominio (mapeados desde Room)
├── insights/                     # Resumen mensual (reglas)
├── shopping/                     # Generador de listas auto
├── prompts/                      # Soft prompts en Home
├── notifications/                # Channels, Worker, Notifier, Scheduler
├── scanner/                      # OCR / hints desde foto de etiqueta
└── util/                         # p.ej. ProductPhotoStore
```

Layouts XML en `app/src/main/res/layout/`, strings en `res/values/strings.xml`, tema/colores en
`res/values/`.

### Base de datos (Room v3)

| Tabla / entidad                                 | Uso                                     |
|-------------------------------------------------|-----------------------------------------|
| `UserProfileEntity`                             | Nombre / perfil                         |
| `CategoryEntity`                                | Categorías (seed en `CategoryDefaults`) |
| `InventoryItemEntity`                           | Productos del inventario                |
| `ShoppingListEntity` / `ShoppingListItemEntity` | Listas de compra                        |
| `StockChangeEventEntity`                        | Historial de consumo / ajustes de stock |

Migraciones: `MIGRATION_1_2` (compras), `MIGRATION_2_3` (eventos de stock) en `KeeplyDatabase.kt`.

### Patrones recurrentes

- **ViewBinding** en Activities/Fragments base.
- **ListAdapter + DiffUtil** en listas.
- **ItemTouchHelper** para swipe (inventario y listas de compra).
- **Flows** desde DAO → `repeatOnLifecycle` en UI.
- Preferencias locales para privacidad, notificaciones, prompts y opciones de spinners (“Agregar
  otra…”).

---

## Funcionalidades y dónde encontrarlas

### Arranque, privacidad y shell

| Funcionamiento                  | Dónde                                                                 |
|---------------------------------|-----------------------------------------------------------------------|
| Splash / launcher               | `presentation/splash/SplashActivity.kt`                               |
| Aceptación de privacidad (gate) | `presentation/privacy/PrivacyPolicyActivity.kt`                       |
| Versión/fecha de aceptación     | `data/preferences/KeeplyPreferences.kt` (`hasAcceptedCurrentPrivacy`) |
| Bottom nav + tabs               | `presentation/controller/ControllerActivity.kt`                       |
| Menú tabs                       | `res/menu/menu_bottom_nav.xml`                                        |

### Inicio (Home)

| Funcionamiento                             | Dónde                                                                                    |
|--------------------------------------------|------------------------------------------------------------------------------------------|
| Pantalla Inicio                            | `presentation/home/HomeFragment.kt`, `res/layout/fragment_home.xml`                      |
| Saludo / perfil incompleto                 | `HomeFragment` + card de greeting                                                        |
| Stats (productos, por caducar, stock bajo) | `HomeFragment` + `item_home_stat.xml`                                                    |
| Alertas caducados / stock bajo + diálogo   | `HomeFragment` + `item_home_alert_card.xml`                                              |
| Soft prompts (fin de mes, reponer, tips)   | `prompts/SoftPromptEvaluator.kt`, `view_soft_prompt.xml`                                 |
| Resumen mensual “Este mes”                 | `insights/MonthlyInsightsEvaluator.kt`, `view_home_insights.xml`, bind en `HomeFragment` |
| Recientes + búsqueda                       | `HomeRecentAdapter.kt`                                                                   |
| CTA agregar producto                       | navega a `AddInventoryItemFragment`                                                      |
| CTA crear lista desde insights/prompts     | `ControllerActivity.navigateToShoppingAutoGenerate()`                                    |

### Inventario

| Funcionamiento                                                          | Dónde                                                                |
|-------------------------------------------------------------------------|----------------------------------------------------------------------|
| Lista de inventario                                                     | `presentation/botiquin/BotiquinFragment.kt`                          |
| Adapter / UI model                                                      | `InventoryItemsAdapter.kt`, `InventoryItemUi.kt`                     |
| Layout card + acciones swipe                                            | `res/layout/item_inventory.xml`                                      |
| Swipe izquierda → Editar / Eliminar                                     | `InventorySwipeCallback.kt`                                          |
| Swipe derecha → consumir (−1 con contador, commit ~900 ms)              | `InventorySwipeCallback` + `BotiquinFragment.commitConsumeSession()` |
| Búsqueda y filtro por categoría                                         | `BotiquinFragment`                                                   |
| Alta / edición de producto                                              | `AddInventoryItemFragment.kt`, `fragment_add_inventory_item.xml`     |
| Spinners + “Agregar otra…” (presentación, unidad, ubicación, categoría) | `AddInventoryItemFragment` + `LookupOptionsStore.kt`                 |
| Foto de producto                                                        | `util/ProductPhotoStore.kt` + FileProvider en Manifest               |
| Prefill por OCR / barcode en foto                                       | `scanner/ProductLabelAnalyzer.kt`, `ProductLabelHints.kt`            |
| Escaneo de código de barras                                             | Play Services Code Scanner (en `AddInventoryItemFragment`)           |
| Persistencia productos                                                  | `InventoryItemDao`, `InventoryItemEntity`                            |

### Historial de stock e insights

| Funcionamiento                     | Dónde                                                  |
|------------------------------------|--------------------------------------------------------|
| Entidad / DAO de eventos           | `StockChangeEventEntity.kt`, `StockChangeEventDao.kt`  |
| Logger (consume, ajuste, alta)     | `data/local/StockChangeLogger.kt`                      |
| Escritura al consumir              | `BotiquinFragment.commitConsumeSession()`              |
| Escritura al editar/crear cantidad | `AddInventoryItemFragment.saveProduct()`               |
| Reglas del resumen mensual         | `insights/MonthlyInsightsEvaluator.kt`                 |
| UI del resumen                     | `HomeFragment.bindInsights()`, `item_home_insight.xml` |

### Compras

| Funcionamiento                                            | Dónde                                                    |
|-----------------------------------------------------------|----------------------------------------------------------|
| Listado de listas                                         | `presentation/shopping/ShoppingListsFragment.kt`         |
| Swipe lista → Renombrar / Eliminar                        | `ShoppingListSwipeCallback.kt`, `item_shopping_list.xml` |
| Crear lista manual o auto                                 | `ShoppingListsFragment.showCreateOptions()`              |
| Diálogo auto (stock / bajo / caducados + chips categoría) | `dialog_auto_shopping_list.xml` + fragment               |
| Generador de ítems auto                                   | `shopping/ShoppingListGenerator.kt`                      |
| Detalle de lista (check / agregar / borrar ítems)         | `ShoppingListDetailFragment.kt`                          |
| DAOs                                                      | `ShoppingListDao.kt` (+ items)                           |

### Ajustes y perfil

| Funcionamiento                              | Dónde                                                                |
|---------------------------------------------|----------------------------------------------------------------------|
| Pantalla Ajustes                            | `presentation/settings/SettingsFragment.kt`                          |
| Editar perfil                               | `EditProfileFragment.kt`                                             |
| Perfil en Room                              | `UserProfileDao`, `UserProfileEntity`                                |
| Toggle notificaciones + cadencia / horarios | `SettingsFragment` + `NotificationSchedule.kt` + `KeeplyPreferences` |
| Días “por caducar”                          | preferencia `expiringSoonDays`                                       |
| Ver privacidad de nuevo                     | desde Settings → `PrivacyPolicyActivity`                             |
| Placeholders (Drive, export, etc.)          | filas en `fragment_settings.xml` / strings                           |

### Notificaciones

| Funcionamiento                         | Dónde                                                 |
|----------------------------------------|-------------------------------------------------------|
| Canales                                | `notifications/KeeplyNotificationChannels.kt`         |
| Schedule al arrancar app               | `KeeplyApplication.kt` + `InventoryAlertScheduler.kt` |
| Worker periódico                       | `InventoryAlertWorker.kt`                             |
| Evaluación de horario                  | `NotificationScheduleEvaluator.kt`                    |
| Construcción / envío                   | `InventoryAlertNotifier.kt`                           |
| Alertas inventario + prompt de compras | Worker + Notifier                                     |

### Diseño / recursos

| Qué                   | Dónde                                               |
|-----------------------|-----------------------------------------------------|
| Colores / tema        | `res/values/colors.xml`, `themes.xml`, `styles.xml` |
| Strings (ES)          | `res/values/strings.xml`                            |
| Iconos nav / acciones | `res/drawable/`                                     |
| Seed categorías       | `data/local/db/CategoryDefaults.kt`                 |

---

## Diagrama de pantallas (resumen)

```
SplashActivity
    └─► PrivacyPolicyActivity (si falta aceptación)
    └─► ControllerActivity
            ├─ HomeFragment
            │     └─ AddInventoryItemFragment (back stack)
            ├─ BotiquinFragment (Inventario)
            │     └─ AddInventoryItemFragment (crear/editar)
            ├─ ShoppingListsFragment
            │     └─ ShoppingListDetailFragment
            └─ SettingsFragment
                  └─ EditProfileFragment
                  └─ PrivacyPolicyActivity (consulta)
```

---

## Cómo construir

```bash
./gradlew :app:assembleDebug
```

Abrir el proyecto en Android Studio y ejecutar el run configuration `app`.

---

## Principios de producto (actuales)

- **Local-first:** sin backend; todo en Room + preferencias.
- **Inventario usable con gestos:** swipe para editar/eliminar y para consumir stock.
- **Compras conectadas al inventario:** generación automática por estado de stock/caducidad.
- **Insights sin IA:** reglas sobre eventos de stock + snapshot del inventario.
- **Privacidad explícita:** gate al entrar y reconsulta desde Ajustes.

---

## Notas para contribuidores

- El package `presentation/botiquin` es el módulo de **Inventario** (nombre histórico).
- Hay carpetas vacías legacy bajo `presentation/` (`alerts`, `main`, `scanner`); la lógica de
  escaneo vive en `com.gabow95k.keeply.scanner`.
- Al cambiar el esquema Room, subir `version` en `KeeplyDatabase` y añadir una `Migration`.
- Mantener strings en español en `strings.xml`; evitar hardcode en UI cuando ya exista recurso.
