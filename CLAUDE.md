# LitRPG: Подземелье Системы — памятка для Claude

Мод для Minecraft **1.21.1 NeoForge** (21.1.265), Java 21. Владелец — Андрей (GitHub `Naholde`, репо `Naholde/litrpg-dungeon`),
играет через **TLauncher**, общается по-русски, в программировании не разбирается.
Это переписывание с нуля старого мода `C:\Users\arsen\litrpg-mod` (1.20.1 Forge) — тот только как справочник.

## Как работать с владельцем
- Делать масштабно, качественно и красиво. Вопросы — только по-настоящему важные.
- Все тексты в игре — **на русском**.
- План и решения владельца — `PLAN.md` (фазы 0–10). Описание игры — `docs/VISION.md`, идеи — `docs/IDEAS.txt` (**не руководство**).
- После каждой фазы: сборка + тесты, проверка на сервере, коммит в `main`, `git push origin main`, запись в `HANDOVER.md`.
  В конце сообщения коммита — строка Co-Authored-By от системы.
- Отчёт — по-русски, коротко, по пунктам, с командами проверки.
- **Напомнить про музыку**: в фазе 9 попросить у Андрея его фонк-треки для хранителей.

## Окружение
- Проект: `C:\Users\arsen\litrpg-dungeon`. JDK 21: `C:\Users\arsen\.litrpg-tools\jdk-21.0.13+9`.
- Сборка (bash): `export JAVA_HOME="C:/Users/arsen/.litrpg-tools/jdk-21.0.13+9"; ./gradlew.bat build` → `build/libs/litrpg-dungeon-<версия>.jar`.
- Игровые тесты: `./gradlew.bat runGameTestServer` (то же делает GitHub Actions после push).
- Dev-клиент: `bash tools/dev/prepare_run.sh` один раз, затем `./gradlew.bat runClient -Pquickplay=<мир>` или `-Pjoin=109.248.163.212:25565`.
- Перед новым запуском — убить java-процессы с `litrpg-dungeon` в командной строке.
- Python 3.10 без PIL. Текстуры — `python tools/gen_textures.py [--preview build/preview]` (не править png руками).
- Ванильная сложность закреплена на «Нормально»: на «Тяжело» мобы бьют ×1,5 мимо наших формул.
- Длинные многострочные python-heredoc в bash ломаются — правки класть в `build/preview/*.py` и запускать.
- GameTest: проверки «по взгляду» — только на шаблоне `arena` (вокруг `empty` тестовый сервер ставит барьер).
  У мок-игрока нет игрового тика — таймеры навыков в тесте двигать `SkillService.tick(p)`.
- GameTest: мок-игрок в творческом режиме и 3 с неуязвим после входа — `setGameMode(SURVIVAL)` и `runAfterDelay(70)`;
  PvP на тестовом сервере выключен — `setPvpAllowed(true)`.

## Сервер (VPS hostvds, Ubuntu 24.04, 8 ядер, 16 ГБ, Москва-время)
- `ssh -i ~/.ssh/litrpg_vps root@109.248.163.212` — только по ключу, пароль отключён; ufw: открыты 22 и 25565.
- Сервер: `/opt/litrpg/server`, служба `systemctl {start|stop|restart|status} litrpg`, пользователь `mc`.
- Команды серверу: `mc-cmd <команда>` (RCON на localhost). Лог: `/opt/litrpg/server/logs/latest.log`.
- Белый список (online-mode=false): `cd /opt/litrpg && sudo -u mc python3 whitelist.py add Ник`, затем `mc-cmd "whitelist reload"`.
  Обычный `/whitelist add` записывает UUID лицензии — игроков TLauncher не пустит.
- Установка мода: `bash tools/server/deploy.sh` (сборка → scp → перезапуск). Бэкапы мира — каждую ночь в 04:00, 7 дней, `/opt/litrpg/backups`.
- Первичная настройка нового VPS: `tools/server/setup.sh` (повторный запуск безопасен).
- Сервер запущен с `-Dlitrpg.dev=true` — работает `dev_commands.txt` в `/opt/litrpg/server`.

## Правила кода (почему старый мод ломался — см. PLAN.md, раздел 2)
- **Механика — в коде, цифры — в `data/litrpg/balance/*.json`.** Раздел баланса = record + `MapCodec` с обязательными полями,
  регистрируется в `Balance`. Значения по умолчанию в коде = содержимое json (проверяет тест). `/reload` перечитывает.
- **Навык = один класс**, подсказка в игре строится из тех же параметров, которые использует код.
- Сервер решает, клиент показывает. Клиентский код — только в `client/` и только через `Dist.CLIENT`.
- Новый/изменённый пакет → поднять `Protocol.VERSION` и дописать строку в `src/test/resources/net_protocol.txt`.
- `.gitignore` — служебные папки от корня (`/run/`), пакеты не называть как служебные папки.
- Каждую механику — с автотестом (JUnit для чистой логики, GameTest для игровой).

## Устройство (`src/main/java/com/andrey/litrpg`)
- `LitRPG` — вход, подключает модули. `core/` — `LitrpgCommand` (`/litrpg version|balance`), `core/balance/` — `Balance`,
  `BalanceKey`, `BalanceLoader`, разделы (`Progression`).
- `net/` — `Protocol` (версия и список пакетов), `ModNetwork`, пакеты. `dev/` — `DevFiles`. `client/` — `DevClientFiles`.
- `player/` — `PlayerData`, `PlayerService` (все изменения персонажа только через него), `PlayerEvents`, `StatBreakdown`
  (один расчёт для боя и окна), `Derived`, `Stat`, `Role`, `RoleInfo`, `Leveling`, `PlayerCommands`.
- `combat/` — `DamageService` (конвейер урона), `DamageMath`, `WeaponKind`, `HealService`, `Popups`, `StarterKit`,
  `threat/` (`ThreatTable`, `ThreatService`), `item/StaffItem`, `entity/` (`MagicBolt`, `TrainingDummy`), `CombatCommands`.
- `status/` — метки со сроком (`StatusType`, `StatusList`, `StatusService`). `fx/` — `Fx`, `FxPayload` (фигуры эффектов).
- `skill/` — `Skill`, `SkillParam`, `SkillContext`, `SkillService`, `SkillText`, `SkillRegistry`, `ClassRegistry`,
  `PlayerClass`, `ClassOffers`, `Style`, `StyleTracker`, `Resource`, `Zones`; `role/` и `classes/` — навыки.
  **Новый навык:** класс в `skill/classes/<Класс>Skills.java`, параметры через `damage()/heal()/seconds()/…`,
  шаблон с `{ключами}`, регистрация в `SkillRegistry`, затем `gradlew test -Pregen` (обновит skills.json).
- `party/` — `Party`, `PartyService`. `core/` — `ModItems`, `ModEntities`, `ModTags`, `ModAttachments`, `SystemChat`.
- `client/` — `HudOverlay`, `fx/` (`ClientPopups`, `ClientAggro`, `WorldText`), `render/`, `ClientParty`, `ClientPlayerData`, `ClientSetup`, `KeyBinds`, `ui/` (`UiKit`, `SysButton`), `screen/`.
- `gametest/` — игровые тесты (шаблон `litrpg:empty`, генератор `tools/make_empty_structure.py`).

## Dev-проверка без человека
- `run/dev_commands.txt` — сервер выполняет строки раз в секунду (лог `run/logs/latest.log`).
- `run/dev_screenshot.txt` с именем → `run/screenshots/<имя>.png` (смотреть через Read).
- Помощники: `bash tools/dev/shot.sh имя`, `bash tools/dev/open.sh character 2`, `bash tools/dev/srv.sh "litrpg level set Dev 20"`.
