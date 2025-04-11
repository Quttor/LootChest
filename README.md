# LootChest

A powerful and configurable Minecraft plugin for scheduled loot chest refills.

## 🌟 Features

- ✅ Define loot tables in `config.yml` with custom items, amounts, and enchantments
- 📦 Mark chests or barrels as loot chests using `/lootchest set <lootTableId>`
- 🥒 Support for **interval** and **specific time** refill modes (`schedule.yml`)
- 🧪 Built-in PlaceholderAPI support (`%lootchest_nextrefill%`)
- 💬 Configurable refill broadcast message
- 🔐 Full permission control via `lootchest.admin`

---

## ⚙️ Setup

### 1. Install the plugin
- Drop the compiled `LootChest.jar` into your server's `/plugins` folder

### 2. Configure loot tables

In `config.yml`:
```yaml
loot-tables:
  default:
    items:
      - type: DIAMOND
        min: 1
        max: 3
        enchantments:
          SHARPNESS: 2
```

### 3. Set chest locations

In-game:
```
/lootchest set <lootTableId>
```

---

## ⏰ Refill Scheduling

Configure `schedule.yml`:

**Interval mode:**
```yaml
mode: interval
interval: 3600  # every 1 hour
```

**Time-based mode:**
```yaml
mode: times
times:
  - "12:00"
  - "18:30"
```

---

## 🧩 PlaceholderAPI

Make sure PlaceholderAPI is installed. Available placeholder:

- `%lootchest_nextrefill%` – time remaining until next refill

---

## 🛡️ Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `lootchest.admin` | Allows use of all `/lootchest` commands | OP |

---

## 🧐 Commands

| Command | Description |
|--------|-------------|
| `/lootchest set <tableId>` | Marks the container you're looking at |
| `/lootchest unset` | Unmarks the container you're looking at |
| `/lootchest begin` | Starts auto-refill based on schedule |
| `/lootchest pause` | Stops refilling |

---

## 👨‍💻 Author

Made by [Quttor](https://github.com/Quttor)  
Enjoy the loot!
