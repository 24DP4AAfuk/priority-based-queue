# Priority-Based Queue 🚦

A Java 21 command-line application that ranks users/items by computed priority and stores data in SQLite.

## What This Project Does ✨

This project manages a queue where each item can have one or more attributes (for example: `experience`, `communication`).
Each attribute has a weight (importance), and an item's priority score is the sum of its assigned attribute weights.

The app includes:

- ⚡ An in-memory priority queue for fast ordering.
- 💾 A SQLite persistence layer so data survives restarts.
- 🖥️ A CLI for managing users/items, attributes, and priority weights.

## Tech Stack 🧰

- ☕ Java 21
- 📦 Maven
- 🗃️ SQLite (`org.xerial:sqlite-jdbc`)

## Project Structure 🗂️

```text
src/main/java/lv/priority/queue/
	Main.java              Entry point (demo mode or CLI mode)
	CLI.java               Command-line interface and command parsing
	Queue.java             In-memory priority queue logic
	Item.java              Item model + score computation
	Attribute.java         Attribute model (name + weight)
	db/
		Database.java        SQLite schema initialization and connections
		DatabaseDAO.java     DB operations and queue synchronization
```

## How Priority Is Calculated 🧮

- Each item has a set of attribute names.
- Each attribute has a weight in the range `0.0` to `1.0`.
- Item score is computed as:

$$
	ext{score(item)} = \sum_{a \in \text{item.attributes}} \text{weight}(a)
$$

If an attribute exists on an item but has no configured importance in memory, the fallback weight is `1.0` in score computation.

## Getting Started 🚀

### Prerequisites ✅

- JDK 21+
- Maven 3.8+

### Build 🛠️

```bash
mvn clean compile
```

### Run CLI 💬

```bash
mvn exec:java
```

### Run Demo Mode 🎬

```bash
mvn exec:java -Dexec.args="demo"
```

## CLI Commands ⌨️

| Command | Description | Example |
|---|---|---|
| `help` | Show available commands | `help` |
| `adduser <name>` | Add a new item/user | `adduser Alice` |
| `setattr <name> <attr>` | Assign attribute to item | `setattr Alice communication` |
| `setimp <attr> <weight>` | Set attribute importance | `setimp communication 0.7` |
| `createattr <name> <coef>` | Create/update attribute definition | `createattr experience 0.9` |
| `listattrs` | List attributes and coefficients | `listattrs` |
| `list` | Show all items ordered by score | `list` |
| `show <name>` | Show one item and its score | `show Alice` |
| `remove <name>` | Remove item | `remove Alice` |
| `poll` | Pop highest-priority item | `poll` |
| `imp` | Print current in-memory importance weights | `imp` |
| `exit` / `quit` | End the application | `exit` |

## Data Persistence 🧱

- SQLite file: `priority.db` (created in the project root).
- Schema is initialized automatically on startup.
- Item-to-attribute assignments are stored as presence-only mappings.

## Notes 📝

- `.gitignore` already excludes database files (`*.db`) and explicitly includes `priority.db`.
- `target/` contains build output and should not be committed.

## License ⚖️

No license file is currently defined in this repository.
