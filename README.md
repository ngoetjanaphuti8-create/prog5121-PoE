# PROG5121 - PoE: QuickChat (Parts 1, 2 and 3)

A console-only Java application. It combines the Part 1 registration and
login, the Part 2 message sending, and the Part 3 arrays, searches and
reports into one finished chat app.

## What's included

- `src/main/java/chatapp/Login.java` — registration and login (Part 1).
- `src/main/java/chatapp/Message.java` — a single message: ID, hash, validation,
  send / disregard / store logic and JSON storage (Part 2).
- `src/main/java/chatapp/MessageManager.java` — the five arrays and the search,
  delete and report features (Part 3).
- `src/main/java/chatapp/Main.java` — the console app and its menus.
- `src/test/java/chatapp/LoginTest.java`, `MessageTest.java` and
  `MessageManagerTest.java` — the unit tests for each part.
- `.github/workflows/TestJava.yml` — GitHub Actions workflow that runs every
  test automatically on each push.
- `pom.xml` — Maven project file (JUnit 5 and Gson).

## How the app works

1. The user registers and logs in (three attempts allowed).
2. After `Welcome to QuickChat.` the user says how many messages they want to
   enter. The arrays are sized from that number, and the messages already
   stored in `storedMessages.json` are read into the stored messages array.
3. The main menu runs until the user quits:
   1. **Send Messages** — each message can be sent, disregarded or stored.
   2. **Show recently sent messages** — shows `Coming Soon.`
   3. **Quit**
   4. **Stored Messages** — opens the Stored Messages menu below.
4. The **Stored Messages** menu lets the user:
   - display the sender and recipient of all stored messages,
   - display the longest stored message,
   - search for a message ID and see its recipient and message,
   - search for all sent or stored messages for a recipient,
   - delete a message using its message hash,
   - display a report of all stored messages (and of all sent messages).

## The arrays (Part 3)

All five arrays are filled while the program runs, so nothing is hard-coded:

| Array | Contents |
|---|---|
| `sentMessages` | every message that was sent |
| `disregardedMessages` | every message that was disregarded |
| `storedMessages` | every stored message, read from the JSON file at start-up |
| `messageHashes` | the hash of every message |
| `messageIDs` | the ID of every message |

`messageHashes` and `messageIDs` are parallel arrays, so the entries at the same
position belong to the same message. Deleting a message shifts the later
entries left and removes the message's hash and ID too. Deleting a stored
message also rewrites the JSON file, so the deletion is kept next time the
app runs.

## Design notes

- **Message ID** — a random ten-digit ID built one digit at a time and kept as a
  `String`, so IDs that start with zero are not lost.
- **Message hash** — the first two digits of the ID, a colon, the message number,
  a colon, then the first and last words in capitals with punctuation removed,
  e.g. `00:0:HITONIGHT`.
- **JSON storage** — stored messages are written to `storedMessages.json` with
  Gson. Messages already in the file are read first, so storing a new message
  never overwrites earlier ones.
- **Unit tests** — the JSON tests use a temporary file, so the real storage file
  is never touched.

## How to run it

You'll need a JDK (17+) and Maven installed, or just NetBeans/IntelliJ.

```bash
# Run the unit tests
mvn test

# Run the console app
mvn compile exec:java
```

In NetBeans, open this folder as an existing Maven project, right-click the
project and choose Test to run the tests, and right-click `Main.java` > Run File
to start the app.

## References

Google, "Gson User Guide". Available at: https://google.github.io/gson/UserGuide.html
(accessed October 2026). Used for converting messages to and from JSON, and for
reading the JSON file back into the stored messages array.

Oracle, "Pattern (Java Platform SE 8)", Oracle Help Center. Available at:
https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html.
Used for the regular expressions in Part 1.
