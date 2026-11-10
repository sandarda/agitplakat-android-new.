# АГИТПЛАКАТ — Android source project

Локальная мастерская винтажных плакатов формата А3. Русский текст плаката
рендерится программно отдельно от нейросетевой иллюстрации.

## Что есть в исходниках

- Compose UI для задания сюжета, надписи, стиля, композиции и палитры.
- Импорт иллюстрации и локального ZIP-пакета SD 1.5.
- Проверка структуры и безопасная распаковка модельного архива.
- PNG 1403×1984 с метаданными 120 DPI и PDF с физическим размером страницы А3.
- Отдельные скрипты сборки/упаковки официального MNN Android runtime с проверкой AArch64 ELF и diffusion_demo.
- Чек-лист проверки на целевом Honor 20.

## Важное состояние 1.2.0

**Генерация пока не подключена.** Скрипты MNN создают основу для получения настоящих
нативных библиотек из отдельного upstream checkout; в этот архив они не включены.
`MnnDiffusionEngine` остаётся выключенным до проверки точных JNI-сигнатур и
успешной инференс-генерации на физическом телефоне. APK ещё не собран и не
проверен на Honor 20.

## Дальнейший путь

См. `docs/MNN_INTEGRATION.md` и `docs/DEVICE_VALIDATION.md`.
\n\n## v1.3.0 native backend status\n\nA first JNI adapter now targets the public MNN Stable Diffusion 1.5 API. It is conditionally compiled when a matching MNN source checkout and ARM64 `libMNN.so` are supplied. This archive still contains neither MNN binaries nor model weights, and has not been built into an APK or tested on Honor 20.\n