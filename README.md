# QuickChat – Registration, Login, Messaging & Stored Messages (Java)

A console chat application. Users register, log in, send / store / disregard messages through QuickChat,
and manage stored messages. Package: `registrationandloginfeature`

## Project structure

**Source Packages → `registrationandloginfeature`**

| File | Part | Purpose |
|---|---|---|
| `RegistrationAndLoginFeature.java` | 1 (edited in 2) | Registration, login, validation, main menu |
| `Message.java` | 2 + 3 | Message class, the five arrays, JSON storage, stored-message operations |
| `QuickChat.java` | 2 + 3 | QuickChat menu, message entry, and the Stored Messages menu |

**Test Packages → `registrationandloginfeature`** (JUnit 4)

| File | Tests | Covers |
|---|---|---|
| `RegistrationAndLoginFeatureTest.java` | 18 | Part 1: username, password, cell number, register, login |
| `MessageTest.java` | 18 | Part 2: length, recipient, ID, hash, send/disregard/store, totals |
| `StoredMessagesTest.java` | 21 | Part 3: arrays, JSON read-back, longest, searches, delete, reports |
| `MainTest.java` | 8 | `main()` driven by scripted input that ends with Exit/Quit |

## How the app works

1. **Chat App menu:** `1. Register`, `2. Login`, `3. Exit`.
2. After a successful login: "Welcome to QuickChat." and the app asks how many messages you want to enter.
3. **QuickChat menu:** `1) Send Messages`, `2) Show recently sent messages` ("Coming Soon."), `3) Quit`, `4) Stored Messages`.
4. For each message: recipient (`+27` + 9 digits), message (max 250 characters), then **Send**, **Disregard** or **Store**.
5. Quit shows the sent messages and the total sent, then ends the application.

## Part 3 – arrays (nothing hard-coded)

All five arrays are filled while the program runs, never typed in:

| Array | Filled when… |
|---|---|
| Sent messages | a message is sent |
| Disregarded messages | a message is disregarded |
| Stored messages | a message is stored, and at QuickChat start by **reading the JSON file** (`storedMessages.json`) |
| Message hashes | any message is sent, stored or disregarded (also loaded ones) |
| Message IDs | same as above |

## Part 3 – Stored Messages menu (QuickChat option 4)

| Option | What it does |
|---|---|
| 1 | Sender and recipient of all stored messages |
| 2 | The longest stored message |
| 3 | Search a message ID → recipient and message (looks through sent, stored and disregarded) |
| 4 | All sent or stored messages for a recipient |
| 5 | Delete a message by its hash (removed from the arrays and from the JSON file if it was stored) |
| 6 | Report with the full details of all stored messages (ID, hash, sender, recipient, message) |
| 7 | Back |

`Message.displayReport()` gives the report of all **sent** messages (hash, recipient, message) used by the brief's "Display Report" unit test.

## Message rules (Part 2)

- **Message ID:** random ten-digit number (`checkMessageID()` ensures it is not more than 10 characters).
- **Recipient:** `+27` followed by 9 digits (re-uses Part 1's `checkCellPhoneNumber`).
- **Length:** up to 250 characters ("Message ready to send." / "Message exceeds 250 characters by X; please reduce the size.").
- **Hash:** first two digits of the ID `:` message number `:` first + last word, capitals. Example `00:0:HITONIGHT`.
- **Send / Disregard / Store:** "Message successfully sent." / "Press 0 to delete the message." / "Message successfully stored."

## Edits to your earlier files

**`RegistrationAndLoginFeature.java`** (three small edits, marked `// PART 2`):
1. New field `static boolean isLoggedIn = false;`
2. `returnLoginStatus()` saves the result: `isLoggedIn = loginUser();`
3. `main`, menu option 2: if login succeeded, run `QuickChat.run(input)`, then log out and end the program.

**`Message.java` / `QuickChat.java`** (Part 3): added the arrays, a sender field, JSON reading, the stored-message operations, and menu option 4.
The JSON file now also contains a `"sender"` field, and storing a message rewrites the whole file from the stored array.

## Assumptions to check against your brief

- **Menu option 4:** "fourth main menu option" is read as QuickChat option 4, so Quit stays option 3.
- **Test message 4:** the brief lists "Developer 0838884567". It is treated as the developer-entered **message ID** (the ID search test uses that number). The brief gives no recipient for message 4, so the tests use the placeholder `+27830000004`.
- **Message 4 text** is `It is dinner time!` (the test-data table has a space before the `!`, but the expected results do not).
- **Delete message text:** the result shows the message exactly as stored, i.e. including its final full stop: `Message: "…on time." successfully deleted.` The brief's example has no full stop inside the quotes.
- **Arrays** are `ArrayList`s (they grow as messages are added). Change to plain arrays if your lecturer insists.
- **Sender** is the logged-in username.
- **Total sent** counts only sent messages (stored and disregarded are not counted).
- **JSON** is read with a small built-in reader, so no library (Gson etc.) is needed in NetBeans.

## Running in NetBeans

**App:** put the three source files in Source Packages → right-click `RegistrationAndLoginFeature.java` → **Run File**.

**Tests:** right-click the project → Properties → Libraries → add **JUnit 4** under the test libraries; put the four test files in Test Packages → `registrationandloginfeature`; right-click the project → **Test** (Alt+F6).
`storedMessages.json` is created in the folder the program runs from.

## Verification

Compiled with JDK 21. All 65 tests pass (18 + 18 + 21 + 8), and two real runs of the app confirmed that stored messages survive a restart, are read back from the JSON file, and are removed from the file when deleted by hash.
The test environment could not download the real JUnit jar, so the tests were run with a small JUnit-4-compatible harness. The test code itself is standard JUnit 4.
