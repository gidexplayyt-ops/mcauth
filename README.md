# MCAuth

MCAuth — мультиплатформенный Minecraft-плагин для регистрации, авторизации, привязки ботов Telegram/VK/Discord/Google Authenticator и поддержки лицензионных аккаунтов.

## Модули
- `core` — общая логика регистрации, хранения аккаунтов и запросов на вход.
- `paper` — модуль для Paper/Spigot/Purpur.
- `velocity` — модуль для Velocity.
- `bungee` — модуль для BungeeCord.

## Сборка
Требуется Gradle 7+ и JDK 17.

Сборка:

```bash
gradle clean build
```

Сборка только модуля Paper:

```bash
gradle :paper:build
```

Сборка всех платформ:

```bash
gradle buildPlugins
```

Сборка и копирование плагинов в корневую папку `dist`:

```bash
gradle assemblePlugins
```

Готовые файлы появятся в `dist/`:
- `McAuth-Paper-1.0.0.jar`
- `McAuth-Velocity-1.0.0.jar`
- `McAuth-Bungee-1.0.0.jar`

## Настройка
Файл конфигурации `paper/src/main/resources/config.yml` содержит параметры для ботов и лицензий.

Пример параметров для интеграции с ботами:

```yaml
telegram-token: "<ваш_telegram_bot_token>"
vk-token: "<ваш_vk_group_token>"
vk-api-version: "5.131"
discord-token: "<ваш_discord_bot_token>"
google-authenticator-issuer: "MCAuth"
license-bypass: true
```

- `telegram-token` — токен бота Telegram от BotFather.
- `vk-token` — токен ВКонтакте с правом отправлять сообщения.
- `discord-token` — токен Discord-бота.
- `google-authenticator-issuer` — имя сервиса для Google Authenticator.

## Команды
- `/mcauth register <пароль>` или `/reg <пароль>`
- `/mcauth login <пароль>` или `/l <пароль>`
- `/mcauth link telegram <chat_id>`
- `/mcauth link vk <peer_id>`
- `/mcauth link discord <user_id>`
- `/mcauth link google_authenticator <ignored>`
- `/mcauth code <telegram|vk|discord> <идентификатор>` — отправка кода привязки в выбранный сервис
- `/mcauth code confirm <код>` — подтверждение привязки аккаунта к боту
- `/mcauth confirm <код>` — подтверждение входа по запросу
- `/mcauth status`
- `/mcauth license <ключ>`
- `/mcauth admin list|reload`

## Возможности
- регистрация и вход на сервер
- привязка аккаунта к ботам Telegram/VK/Discord/Google Authenticator
- автоотправка запроса на вход
- отображение текущей онлайн-статус и количества аккаунтов
- админ-команды
- BossBar при входе с 5-минутным таймером
- пропуск лицензионных аккаунтов без регистрации
- если аккаунт привязан, требуется подтверждение входа
