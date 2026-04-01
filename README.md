# Priority-Based Queue 🚦

A Java 21 command-line application that ranks users/items by computed priority and stores data in SQLite.

## What This Project Does ✨

This project manages a queue where each item can have one or more attributes with values (0.0 to 1.0).
Each attribute has a weight (importance) and a rule (ASC/DESC) for priority calculation.
An item's priority score is the sum of its assigned attribute values adjusted by weights and rules.

The app includes:

- ⚡ An in-memory priority queue for fast ordering.
- 💾 A SQLite persistence layer so data survives restarts.
- 🔐 User authentication with roles (Admin, Worker, Client).
- 📊 System performance statistics (uptime, reordering time, last processed).
- 📜 History of processed items.
- 🔍 Search by ID or name.
- 🖥️ A CLI for managing users/items, attributes, and priority weights.

## Tech Stack 🧰

- ☕ Java 21
- 📦 Maven
- 🗃️ SQLite (`org.xerial:sqlite-jdbc`)

## Project Structure 🗂️

```text
src/main/java/lv/priority/queue/
	Main.java              Entry point (demo mode or CLI mode)
	CLI.java               Command-line interface with authentication
	Queue.java             In-memory priority queue logic
	Item.java              Item model with attribute values
	Attribute.java         Attribute model (name + weight + rule)
	db/
		Database.java        SQLite schema initialization
		DatabaseDAO.java     DB operations and queue synchronization
```

## How Priority Is Calculated 🧮

- Each item has attributes with values in [0.0, 1.0].
- Each attribute has a weight in [0.0, 1.0] and a rule (ASC or DESC).
- For ASC: score contribution = value * weight
- For DESC: score contribution = (1.0 - value) * weight
- Item score = sum of contributions from all attributes.

## User Roles 🔐

- **Admin**: Manage attributes, users, remove items.
- **Worker**: Process (poll) items.
- **Client**: View items, search.

Default users:
- admin/admin (Admin)
- worker/worker (Worker)
- client/client (Client)

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

Login with username/password.

### Run Demo Mode 🎬

```bash
mvn exec:java -Dexec.args="demo"
```

## CLI Commands ⌨️

| Command | Description | Example | Role |
|---|---|---|---|
| `help` | Show available commands | `help` | All |
| `adduser <name>` | Add a new item/user | `adduser Alice` | Admin |
| `setattr <name> <attr> <value>` | Assign attribute value to item | `setattr Alice experience 0.8` | All |
| `setimp <attr> <weight> <rule>` | Set attribute importance and rule | `setimp experience 0.7 ASC` | Admin |
| `createattr <name> <coef> <rule>` | Create/update attribute definition | `createattr experience 0.9 ASC` | Admin |
| `listattrs` | List attributes and rules | `listattrs` | All |
| `list` | Show CURRENT PRIORITY QUEUE table | `list` | All |
| `show <name>` | Show item details and score | `show Alice` | All |
| `remove <name>` | Remove an item | `remove Alice` | Admin |
| `poll` | Process highest-priority item | `poll` | Worker |
| `search <query>` | Search items by ID or name | `search Alice` | All |
| `history` | Show processing history | `history` | All |
| `stats` | Show system statistics | `stats` | All |
| `exit` / `quit` | End the application | `exit` | All |

## Data Persistence 🧱

- SQLite file: `priority.db`
- Tables: atributs, lietotajs, objekts, objekta_vertiba, history, system_stats, last_processed

## Notes 📝

- Attributes have values 0.0-1.0, rules ASC/DESC.
- Authentication required to access CLI.
- Performance stats track uptime and reordering times.
- History and last 10 processed items maintained.

## Data Persistence 🧱

- SQLite file: `priority.db` (created in the project root).
- Schema is initialized automatically on startup.
- Item-to-attribute assignments are stored with values.
- User authentication, history, and stats persisted.

## Notes 📝

- `.gitignore` already excludes database files (`*.db`) and explicitly includes `priority.db`.
- `target/` contains build output and should not be committed.
- Attributes now have values and ASC/DESC rules for flexible priority calculation.
- Authentication ensures role-based access control.

## License ⚖️

No license file is currently defined in this repository.
