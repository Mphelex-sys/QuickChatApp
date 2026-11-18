package quickchatapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Message {

    public static final int MAX_LENGTH = 250;

    // ─── PART 3: the five arrays (filled while the program runs – nothing hard-coded) ──
    private static final List<Message> sentMessages = new ArrayList<>();        // all messages sent
    private static final List<Message> disregardedMessages = new ArrayList<>(); // all messages disregarded
    private static final List<Message> storedMessages = new ArrayList<>();      // read from / saved to the JSON file
    private static final List<String> messageHashes = new ArrayList<>();        // all message hashes
    private static final List<String> messageIDs = new ArrayList<>();           // all message IDs

    private static int totalSent = 0;
    private static String storeFile = "storedMessages.json";
    private static boolean storedLoaded = false;

    // ─── Per-message data ────────────────────────────────────────────────────
    private final String messageID;      // random ten-digit number
    private final int messageNumber;     // 0-based position of the message in this session
    private final String sender;
    private final String recipient;
    private final String text;
    private final String messageHash;

    /** Normal constructor: the Message ID is generated automatically. */
    public Message(int messageNumber, String recipient, String text) {
        this(generateMessageID(), messageNumber, currentSender(), recipient, text);
    }

    /** Constructor that accepts an ID (used by the unit tests so results are predictable). */
    public Message(String messageID, int messageNumber, String recipient, String text) {
        this(messageID, messageNumber, currentSender(), recipient, text);
    }

    /** Constructor that also accepts the sender. */
    public Message(String messageID, int messageNumber, String sender, String recipient, String text) {
        this.messageID = messageID;
        this.messageNumber = messageNumber;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.messageHash = createMessageHash();
    }

    /** Used when a message is read back from the JSON file (hash is taken from the file). */
    private Message(String messageID, String messageHash, String sender, String recipient, String text) {
        this.messageID = messageID;
        this.messageHash = messageHash;
        this.messageNumber = numberFromHash(messageHash);
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
    }

    // ─── Part 2 methods ──────────────────────────────────────────────────────

    /** Ensures the message ID is not more than ten characters. */
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= 10;
    }

    /** Validates this message's recipient (re-uses the Part 1 cell number rule). */
    public String checkRecipientCell() {
        return checkRecipientCell(recipient);
    }

    public static String checkRecipientCell(String cell) {
        if (cell != null && QuickChatApp.checkCellPhoneNumber(cell)) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain "
                + "an international code. Please correct the number and try again.";
    }

    /** Message length rule: no more than 250 characters. */
    public static String checkMessageLength(String text) {
        int length = text == null ? 0 : text.length();
        if (length <= MAX_LENGTH) {
            return "Message ready to send.";
        }
        return "Message exceeds 250 characters by " + (length - MAX_LENGTH)
                + "; please reduce the size.";
    }

    /**
     * Hash = first two digits of the ID, ":", message number, ":",
     * first word + last word of the message, all in capitals.
     * e.g. ID 0012345678, message 0, "Hi Mike, can you join us for dinner tonight?" -> 00:0:HITONIGHT
     */
    public String createMessageHash() {
        String idPart = messageID.substring(0, Math.min(2, messageID.length()));

        String[] words = text == null ? new String[]{""} : text.trim().split("\\s+");
        String first = cleanWord(words[0]);
        String last = cleanWord(words[words.length - 1]);

        return (idPart + ":" + messageNumber + ":" + first + last).toUpperCase();
    }

    /** Programmatic version: 1 = Send, 2 = Disregard, 3 = Store. Also fills the Part 3 arrays. */
    public String sentMessage(int choice) {
        switch (choice) {
            case 1:
                sentMessages.add(this);
                totalSent++;
                registerIdentifiers(this);
                return "Message successfully sent.";
            case 2:
                disregardedMessages.add(this);
                registerIdentifiers(this);
                return "Press 0 to delete the message.";
            case 3:
                return storeMessage() ? "Message successfully stored."
                                      : "Message could not be stored.";
            default:
                return "Invalid option. Please choose 1 (Send), 2 (Disregard) or 3 (Store).";
        }
    }

    /** Interactive version: asks the user whether to send, disregard or store. */
    public String sentMessage(Scanner in) {
        int choice = 0;
        while (choice < 1 || choice > 3) {
            System.out.println("What would you like to do with this message?");
            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            System.out.print("Enter choice: ");
            try {
                choice = Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = 0;
            }
            if (choice < 1 || choice > 3) {
                System.out.println("Invalid option. Please choose 1, 2 or 3.");
            }
        }

        String result = sentMessage(choice);
        if (choice == 2) {
            // Keep asking until the user presses 0 to delete the message
            System.out.println(result);
            while (!in.nextLine().trim().equals("0")) {
                System.out.println(result);
            }
            return "Message deleted.";
        }
        return result;
    }

    /** Returns every message sent while the program has been running. */
    public static String printMessages() {
        if (sentMessages.isEmpty()) {
            return "No messages have been sent yet.";
        }
        StringBuilder sb = new StringBuilder();
        for (Message m : sentMessages) {
            sb.append(m.details()).append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    /** Total number of messages sent. */
    public static int returnTotalMessages() {
        return totalSent;
    }

    // ─── Storing messages (JSON) ─────────────────────────────────────────────

    /** Adds this message to the stored array and saves the whole array to the JSON file. */
    public boolean storeMessage() {
        if (!storedLoaded) {
            loadStoredMessages();
        }
        if (!storedMessages.contains(this)) {
            storedMessages.add(this);
        }
        registerIdentifiers(this);
        return saveStoredMessages();
    }

    /** Writes the stored array to the JSON file. */
    private static boolean saveStoredMessages() {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < storedMessages.size(); i++) {
            sb.append(storedMessages.get(i).toJson());
            sb.append(i < storedMessages.size() - 1 ? ",\n" : "\n");
        }
        sb.append("]");
        try {
            Files.writeString(Paths.get(storeFile), sb.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** PART 3: reads the JSON file back into the stored-messages array. */
    public static boolean loadStoredMessages() {
        storedMessages.clear();
        storedLoaded = true;

        Path path = Paths.get(storeFile);
        if (!Files.exists(path)) {
            return true;    // nothing stored yet
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            for (Map<String, String> fields : parseJsonObjects(json)) {
                Message m = new Message(
                        fields.getOrDefault("messageID", ""),
                        fields.getOrDefault("messageHash", ""),
                        fields.getOrDefault("sender", "Unknown"),
                        fields.getOrDefault("recipient", ""),
                        fields.getOrDefault("message", ""));
                storedMessages.add(m);
                registerIdentifiers(m);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ─── PART 3: Stored Messages menu operations ─────────────────────────────

    /** (a) Sender and recipient of every stored message. */
    public static String displaySenderAndRecipient() {
        ensureLoaded();
        if (storedMessages.isEmpty()) {
            return "No stored messages.";
        }
        StringBuilder sb = new StringBuilder("Sender and recipient of all stored messages:");
        for (int i = 0; i < storedMessages.size(); i++) {
            Message m = storedMessages.get(i);
            sb.append(System.lineSeparator())
              .append(i + 1).append(") Sender: ").append(m.sender)
              .append(" | Recipient: ").append(m.recipient);
        }
        return sb.toString();
    }

    /** (b) The longest stored message (just the message text). */
    public static String longestStoredMessage() {
        ensureLoaded();
        if (storedMessages.isEmpty()) {
            return "No stored messages.";
        }
        Message longest = storedMessages.get(0);
        for (Message m : storedMessages) {
            if (m.text.length() > longest.text.length()) {
                longest = m;
            }
        }
        return longest.text;
    }

    /** (c) Finds a message (sent, stored or disregarded) by its ID; null if none. */
    public static Message findByMessageID(String id) {
        ensureLoaded();
        String target = id == null ? "" : id.trim();
        for (List<Message> list : allArrays()) {
            for (Message m : list) {
                if (m.messageID.equals(target)) {
                    return m;
                }
            }
        }
        return null;
    }

    /** (c) Search for a message ID and show the recipient and message. */
    public static String searchByMessageID(String id) {
        Message m = findByMessageID(id);
        if (m == null) {
            return "No message found with ID " + (id == null ? "" : id.trim()) + ".";
        }
        return "Recipient: " + m.recipient + System.lineSeparator() + "Message: " + m.text;
    }

    /** (d) The text of every sent or stored message for one recipient. */
    public static List<String> messagesForRecipient(String recipient) {
        ensureLoaded();
        String target = recipient == null ? "" : recipient.trim();
        List<String> result = new ArrayList<>();
        for (Message m : sentMessages) {
            if (m.recipient.equals(target)) {
                result.add(m.text);
            }
        }
        for (Message m : storedMessages) {
            if (m.recipient.equals(target)) {
                result.add(m.text);
            }
        }
        return result;
    }

    /** (d) Search for all messages sent or stored for a particular recipient. */
    public static String searchByRecipient(String recipient) {
        List<String> found = messagesForRecipient(recipient);
        String target = recipient == null ? "" : recipient.trim();
        if (found.isEmpty()) {
            return "No messages found for " + target + ".";
        }
        StringBuilder sb = new StringBuilder("Messages for " + target + ":");
        for (int i = 0; i < found.size(); i++) {
            sb.append(System.lineSeparator()).append(i + 1).append(") ").append(found.get(i));
        }
        return sb.toString();
    }

    /** (e) Deletes a message using its hash (from every array, and from the JSON file if stored). */
    public static String deleteByHash(String hash) {
        ensureLoaded();
        String target = hash == null ? "" : hash.trim();

        Message found = null;
        for (List<Message> list : allArrays()) {
            for (Message m : list) {
                if (m.messageHash.equalsIgnoreCase(target)) {
                    found = m;
                    break;
                }
            }
            if (found != null) {
                break;
            }
        }
        if (found == null) {
            return "No message found with hash " + target + ".";
        }

        boolean wasStored = storedMessages.remove(found);
        sentMessages.remove(found);
        disregardedMessages.remove(found);
        messageHashes.remove(found.messageHash);
        messageIDs.remove(found.messageID);
        if (wasStored) {
            saveStoredMessages();
        }
        return "Message: \"" + found.text + "\" successfully deleted.";
    }

    /** Report of all SENT messages: hash, recipient and message (plus ID and sender). */
    public static String displayReport() {
        return buildReport("===== Sent Messages Report =====", sentMessages,
                "No messages have been sent yet.");
    }

    /** (f) Report with the full details of all STORED messages. */
    public static String storedMessagesReport() {
        ensureLoaded();
        return buildReport("===== Stored Messages Report =====", storedMessages,
                "No stored messages.");
    }

    // ─── PART 3: access to the arrays ────────────────────────────────────────

    public static List<String> getSentMessageTexts()        { return texts(sentMessages); }
    public static List<String> getDisregardedMessageTexts() { return texts(disregardedMessages); }
    public static List<String> getStoredMessageTexts()      { ensureLoaded(); return texts(storedMessages); }
    public static List<String> getMessageHashes()           { return new ArrayList<>(messageHashes); }
    public static List<String> getMessageIDs()              { return new ArrayList<>(messageIDs); }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Full details in the order required by the brief: ID, Hash, Recipient, Message. */
    public String details() {
        return String.format("Message ID: %s%nMessage Hash: %s%nRecipient: %s%nMessage: %s",
                messageID, messageHash, recipient, text);
    }

    public String toJson() {
        return "  {\n"
                + "    \"messageID\": \"" + escape(messageID) + "\",\n"
                + "    \"messageHash\": \"" + escape(messageHash) + "\",\n"
                + "    \"sender\": \"" + escape(sender) + "\",\n"
                + "    \"recipient\": \"" + escape(recipient) + "\",\n"
                + "    \"message\": \"" + escape(text) + "\"\n"
                + "  }";
    }

    public static String generateMessageID() {
        long number = ThreadLocalRandom.current().nextLong(0L, 10_000_000_000L);
        return String.format("%010d", number);
    }

    private static String currentSender() {
        return QuickChatApp.username != null
                ? QuickChatApp.username : "Unknown";
    }

    private static void ensureLoaded() {
        if (!storedLoaded) {
            loadStoredMessages();
        }
    }

    private static void registerIdentifiers(Message m) {
        if (!messageHashes.contains(m.messageHash)) {
            messageHashes.add(m.messageHash);
        }
        if (!messageIDs.contains(m.messageID)) {
            messageIDs.add(m.messageID);
        }
    }

    private static List<List<Message>> allArrays() {
        List<List<Message>> all = new ArrayList<>();
        all.add(storedMessages);
        all.add(sentMessages);
        all.add(disregardedMessages);
        return all;
    }

    private static List<String> texts(List<Message> list) {
        List<String> result = new ArrayList<>();
        for (Message m : list) {
            result.add(m.text);
        }
        return result;
    }

    private static String buildReport(String title, List<Message> list, String emptyText) {
        StringBuilder sb = new StringBuilder(title);
        if (list.isEmpty()) {
            return sb.append(System.lineSeparator()).append(emptyText).toString();
        }
        for (int i = 0; i < list.size(); i++) {
            Message m = list.get(i);
            sb.append(System.lineSeparator()).append(System.lineSeparator())
              .append("Message ").append(i + 1).append(System.lineSeparator())
              .append("Message ID: ").append(m.messageID).append(System.lineSeparator())
              .append("Message Hash: ").append(m.messageHash).append(System.lineSeparator())
              .append("Sender: ").append(m.sender).append(System.lineSeparator())
              .append("Recipient: ").append(m.recipient).append(System.lineSeparator())
              .append("Message: ").append(m.text);
        }
        return sb.toString();
    }

    private static int numberFromHash(String hash) {
        String[] parts = hash == null ? new String[0] : hash.split(":");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static String cleanWord(String word) {
        return word.replaceAll("[^A-Za-z0-9]", "");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ─── Small JSON reader (array of flat objects whose values are strings) ──

    private static List<Map<String, String>> parseJsonObjects(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        int i = 0;
        int n = json.length();
        while (i < n) {
            if (json.charAt(i) == '{') {
                Map<String, String> fields = new LinkedHashMap<>();
                i++;
                while (i < n && json.charAt(i) != '}') {
                    if (json.charAt(i) == '"') {
                        int[] pos = {i};
                        String key = readJsonString(json, pos);
                        i = pos[0];
                        while (i < n && json.charAt(i) != ':') i++;
                        i++;
                        while (i < n && json.charAt(i) != '"' && json.charAt(i) != '}') i++;
                        if (i < n && json.charAt(i) == '"') {
                            pos[0] = i;
                            String value = readJsonString(json, pos);
                            i = pos[0];
                            fields.put(key, value);
                        }
                    } else {
                        i++;
                    }
                }
                result.add(fields);
            }
            i++;
        }
        return result;
    }

    /** Reads a JSON string starting at the opening quote; pos[0] ends up just after the closing quote. */
    private static String readJsonString(String s, int[] pos) {
        int i = pos[0] + 1;
        StringBuilder sb = new StringBuilder();
        while (i < s.length() && s.charAt(i) != '"') {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        pos[0] = i + 1;
        return sb.toString();
    }

    // Package-private hooks so unit tests start from a clean slate
    static void resetSession() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
        totalSent = 0;
        storedLoaded = false;
    }

    static void setStoreFile(String path) {
        storeFile = path;
    }

    public String getMessageID()   { return messageID; }
    public String getMessageHash() { return messageHash; }
    public String getSender()      { return sender; }
    public String getRecipient()   { return recipient; }
    public String getText()        { return text; }
    public int getMessageNumber()  { return messageNumber; }
}
