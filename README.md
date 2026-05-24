# ImagesObserver

Android-приложение для просмотра списка изображений из удалённого манифеста: сетка превью, полноэкранный просмотр с зумом и свайпом, офлайн-кэш на диске.

Источник данных: [`https://it-link.ru/test/images.txt`](https://it-link.ru/test/images.txt) — по одному URL изображения на строку.

---

## Поддерживаемые версии Android

| Параметр | Значение | Версия ОС |
|----------|----------|-----------|
| **minSdk** | 24 | Android **7.0** (Nougat) и выше |
| **targetSdk** | 37 | — |
| **compileSdk** | 37 | — |

Приложение рассчитано на устройства с **Android 7.0+**. На более старых версиях установка невозможна.

---

## Возможности

### Список изображений (grid)

- Загрузка манифеста `images.txt` с сервера с **offline-first** стратегией: сначала кэш на диске, затем обновление из сети.
- Адаптивная сетка: число колонок подбирается по ширине экрана (целевая ширина ячейки ~100–120 px).
- Превью берутся **только из локального кэша**; при отсутствии файла показывается placeholder «—», без прямой загрузки URL в Coil на сетке.
- Thumbnail генерируются на лету из оригинала (downsample + JPEG) и сохраняются на диск.
- При ошибке загрузки списка без данных — сообщение и **автоматический retry** при появлении сети (без кнопки Retry на grid).

### Детальный просмотр

- Горизонтальный **pager** по всем валидным ссылкам из манифеста.
- Полноразмерные изображения из дискового кэша (скачивание при необходимости).
- **Pinch-to-zoom**, pan одним пальцем, **double-tap** для сброса масштаба.
- **Immersive mode**: тап по изображению скрывает/показывает top bar и system bars.
- Кнопка **Retry** только на экране детали при ошибке загрузки.

### Кэширование

- Манифест `images.txt` — в private files.
- Оригиналы — `filesDir/image_cache/originals/`.
- Thumbnail — `filesDir/image_cache/thumbnails/` (ключ: URL + размер в px).
- LRU-очистка при превышении **100 MB** (до ~80% лимита).
- Фоновый prefetch манифеста при старте приложения и retry при восстановлении сети.

### Прочее

- **Share** URL изображения (chooser + `ContentProvider` для `content://` URI).
- **Open in browser** — открыть URL во внешнем браузере.
- **Светлая и тёмная тема** (Material 3, `isSystemInDarkTheme()`).
- Adaptive launcher icon (лупа на фиолетовом фоне).

---

## Архитектура

Проект построен по **Clean Architecture** с разделением на слои и dependency rule: зависимости направлены **внутрь** (presentation → domain ← data).

```
┌─────────────────────────────────────────────────────────┐
│  presentation                                           │
│  Compose UI, ViewModel, Navigation, Theme               │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  domain                                                 │
│  Use cases, repository ports, models, domain errors     │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  data                                                   │
│  Repository impl, Retrofit, disk cache, ContentProvider │
└─────────────────────────────────────────────────────────┘
```

### Слои

| Слой | Назначение | Примеры |
|------|------------|---------|
| **presentation** | UI на Jetpack Compose, состояние экранов | `ImagesScreen`, `ImageDetailScreen`, `ImagesViewModel` |
| **domain** | Бизнес-правила, абстракции (ports) | `ObserveImagesGridUseCase`, `LoadCachedOriginalUseCase`, `ImagesListRepository` |
| **data** | Реализации портов, сеть, файлы | `ImagesListRepositoryImpl`, `OriginalImageCacheRepositoryImpl`, `ImageBytesDownloader` |
| **di** | Hilt-модули, `@IoDispatcher` | `RepositoryModule`, `NetworkModule`, `DispatcherModule` |

### Ключевые паттерны

- **MVVM** в presentation: `ViewModel` + `StateFlow` / `UiState`.
- **Repository** в domain как интерфейс, реализация в data.
- **Use cases** инкапсулируют сценарии (grid, cache, share, prefetch).
- **Hilt** для DI, **`@IoDispatcher`** для disk/network/encode на `Dispatchers.IO`.
- **Offline-first**: `LoadImagesGridOfflineFirstUseCase` + `ObserveImagesGridUseCase` с подпиской на сеть.
- **Navigation Compose** + type-safe routes (`kotlinx.serialization`).

### Структура пакетов

```
app/src/main/java/com/example/imagesobserver/
├── constants/          # ProjectConstants, CacheConstants
├── data/
│   ├── api/            # Retrofit, OkHttp interceptors
│   ├── cache/          # Disk cache, thumbnail encoder, downloader
│   ├── connectivity/   # NetworkConnectivityObserver
│   ├── local/          # Manifest/list cache stores
│   ├── repository/     # Repository implementations
│   ├── session/        # ImageGallerySession (grid ↔ detail handoff)
│   └── sharing/        # Share gateway impl, ContentProvider
├── di/                 # Hilt modules
├── domain/
│   ├── connectivity/   # NetworkAvailabilitySource
│   ├── error/          # AppException, AppErrorFactory
│   ├── model/          # ImageUrl, ManifestGridRow
│   ├── prefetch/       # Prefetch gates
│   ├── repository/     # Repository ports
│   ├── sharing/        # ImageUrlShareGateway
│   └── usecase/        # Use cases
├── presentation/
│   ├── detail/         # Detail ViewModel, fullscreen image
│   ├── images/         # Grid, detail screen, zoom
│   ├── navigation/     # NavHost, routes
│   ├── systemui/       # ImmersiveSystemUi
│   └── theme/          # Material 3 theme, Dimens
└── util/               # smartRetry
```

---

## Технологии

- **Kotlin** 2.0, **Jetpack Compose**, **Material 3**
- **Hilt** — dependency injection
- **Navigation Compose** — type-safe navigation
- **Retrofit** + **OkHttp** — HTTP (манифест и байты изображений)
- **Coil** — отображение bitmap из `File` на UI
- **Coroutines** + **Flow** — асинхронность и reactive grid
- **Timber** — логирование (debug builds)

---

## Сборка и запуск

Требования: **JDK 11+**, Android SDK с **API 37**.

```bash
# Debug-сборка
./gradlew :app:assembleDebug

# Установка на подключённое устройство / эмулятор
./gradlew :app:installDebug
```

Откройте проект в **Android Studio** (Ladybug или новее) и запустите конфигурацию `app`.

---

## Разрешения

- `INTERNET` — загрузка манифеста и изображений
- `ACCESS_NETWORK_STATE` — отслеживание сети для auto-retry и prefetch

---

