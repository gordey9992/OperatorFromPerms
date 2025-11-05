# OperatorFromPerms

Плагин для Minecraft серверов (1.21.1+), который позволяет выдавать права оператора через разрешение LuckPerms.

## 📦 Автоматическая сборка

[![Java CI with Maven](https://github.com/gordey9992/OperatorFromPerms/actions/workflows/maven.yml/badge.svg)](https://github.com/gordey9992/OperatorFromPerms/actions/workflows/maven.yml)

Плагин автоматически собирается при каждом коммите в ветку `main`. Скачать готовый JAR можно во вкладке **Actions**.

## 🚀 Быстрый старт

1. Скачайте последнюю версию из [Releases](https://github.com/gordey9992/OperatorFromPerms/releases)
2. Поместите файл `.jar` в папку `plugins/`
3. Перезагрузите сервер
4. Настройте конфигурацию в `plugins/OperatorFromPerms/config.yml`

## ⚙️ Использование

### Выдать права оператора:
```bash
/lp user <игрок> permission set operatorfromperms.all true
