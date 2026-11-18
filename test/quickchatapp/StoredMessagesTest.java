package quickchatapp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** PART 3 unit tests, using the test data from the brief. */
public class StoredMessagesTest {

    private static final String SENDER = "kt_kn";

    private static final String TEXT_1 = "Did you get the cake?";
    private static final String TEXT_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String TEXT_3 = "Yohoooo, I am at your gate.";
    private static final String TEXT_4 = "It is dinner time!";
    private static final String TEXT_5 = "Ok, I am leaving without you.";

    private static final String HASH_1 = "10:0:DIDCAKE";
    private static final String HASH_2 = "20:1:WHERETIME";
    private static final String HASH_4 = "08:3:ITTIME";

    private Path storeFile;

    @Before
    public void setUp() throws IOException {
        Message.resetSession();
        storeFile = Files.createTempFile("quickchat-part3", ".json");
        Files.deleteIfExists(storeFile);
        Message.setStoreFile(storeFile.toString());
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(storeFile);
    }

    /**
     * Developer entry of the brief's test data (messages 1-5), using the flag given for each:
     * 1 Sent, 2 Stored, 3 Disregard, 4 Sent (developer ID 0838884567), 5 Stored.
     */
    private void loadTestData() {
        new Message("1000000001", 0, SENDER, "+27834557896", TEXT_1).sentMessage(1);
        new Message("2000000002", 1, SENDER, "+27838884567", TEXT_2).sentMessage(3);
        new Message("3000000003", 2, SENDER, "+27834484567", TEXT_3).sentMessage(2);
        new Message("0838884567", 3, SENDER, "+27830000004", TEXT_4).sentMessage(1);
        new Message("5000000005", 4, SENDER, "+27838884567", TEXT_5).sentMessage(3);
    }

    // ─── 1. The arrays ───────────────────────────────────────────────────────

    @Test
    public void sentMessagesArray_isCorrectlyPopulated() {
        loadTestData();
        assertEquals(Arrays.asList("Did you get the cake?", "It is dinner time!"),
                Message.getSentMessageTexts());
    }

    @Test
    public void disregardedMessagesArray_isCorrectlyPopulated() {
        loadTestData();
        assertEquals(Collections.singletonList("Yohoooo, I am at your gate."),
                Message.getDisregardedMessageTexts());
    }

    @Test
    public void storedMessagesArray_isCorrectlyPopulated() {
        loadTestData();
        assertEquals(Arrays.asList(TEXT_2, TEXT_5), Message.getStoredMessageTexts());
    }

    @Test
    public void hashAndIdArrays_holdEveryMessage() {
        loadTestData();
        assertEquals(5, Message.getMessageHashes().size());
        assertEquals(5, Message.getMessageIDs().size());
        assertTrue(Message.getMessageHashes().contains(HASH_1));
        assertTrue(Message.getMessageHashes().contains(HASH_2));
        assertTrue(Message.getMessageHashes().contains(HASH_4));
        assertTrue(Message.getMessageIDs().contains("0838884567"));
    }

    @Test
    public void storedMessagesArray_isReadBackFromTheJsonFile() {
        loadTestData();
        Message.resetSession();                 // forget everything held in memory
        assertTrue(Message.loadStoredMessages());   // ...then read the JSON file again
        assertEquals(Arrays.asList(TEXT_2, TEXT_5), Message.getStoredMessageTexts());
        assertTrue(Message.getMessageHashes().contains(HASH_2));
    }

    @Test
    public void jsonFile_keepsQuotesAndSenderWhenReadBack() {
        new Message("4000000004", 0, SENDER, "+27831112222", "He said \"hi\" today").sentMessage(3);
        Message.resetSession();
        Message.loadStoredMessages();
        assertEquals(Collections.singletonList("He said \"hi\" today"), Message.getStoredMessageTexts());
        assertTrue(Message.displaySenderAndRecipient().contains("Sender: kt_kn"));
    }

    // ─── 2a. Sender and recipient ────────────────────────────────────────────

    @Test
    public void displaySenderAndRecipient_listsStoredMessagesOnly() {
        loadTestData();
        String out = Message.displaySenderAndRecipient();
        assertTrue(out.contains("Sender: kt_kn | Recipient: +27838884567"));
        assertFalse(out.contains("+27834557896"));    // message 1 was sent, not stored
    }

    // ─── 2b. Longest message ─────────────────────────────────────────────────

    @Test
    public void longestMessage_isDisplayed() {
        loadTestData();
        assertEquals("Where are you? You are late! I have asked you to be on time.",
                Message.longestStoredMessage());
    }

    @Test
    public void longestMessage_whenNothingStored() {
        assertEquals("No stored messages.", Message.longestStoredMessage());
    }

    // ─── 2c. Search for a message ID ─────────────────────────────────────────

    @Test
    public void searchMessageID_returnsTheMessage() {
        loadTestData();
        assertEquals("It is dinner time!", Message.findByMessageID("0838884567").getText());
    }

    @Test
    public void searchMessageID_showsRecipientAndMessage() {
        loadTestData();
        String out = Message.searchByMessageID("0838884567");
        assertTrue(out.contains("Recipient: +27830000004"));
        assertTrue(out.contains("Message: It is dinner time!"));
    }

    @Test
    public void searchMessageID_unknownId() {
        loadTestData();
        assertEquals("No message found with ID 9999999999.", Message.searchByMessageID("9999999999"));
    }

    // ─── 2d. Search by recipient ─────────────────────────────────────────────

    @Test
    public void searchByRecipient_returnsAllSentOrStoredMessages() {
        loadTestData();
        assertEquals(Arrays.asList("Where are you? You are late! I have asked you to be on time.",
                                   "Ok, I am leaving without you."),
                Message.messagesForRecipient("+27838884567"));
    }

    @Test
    public void searchByRecipient_formattedResult() {
        loadTestData();
        String out = Message.searchByRecipient("+27838884567");
        assertTrue(out.contains("1) " + TEXT_2));
        assertTrue(out.contains("2) " + TEXT_5));
    }

    @Test
    public void searchByRecipient_unknownRecipient() {
        loadTestData();
        assertEquals("No messages found for +27831111111.", Message.searchByRecipient("+27831111111"));
    }

    // ─── 2e. Delete using the message hash ───────────────────────────────────

    @Test
    public void deleteByHash_removesTheMessage() {
        loadTestData();
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time.\" "
                + "successfully deleted.", Message.deleteByHash(HASH_2));
        assertEquals(Collections.singletonList(TEXT_5), Message.getStoredMessageTexts());
        assertFalse(Message.getMessageHashes().contains(HASH_2));
    }

    @Test
    public void deleteByHash_alsoUpdatesTheJsonFile() throws IOException {
        loadTestData();
        Message.deleteByHash(HASH_2);

        assertFalse(Files.readString(storeFile).contains("Where are you"));
        Message.resetSession();
        Message.loadStoredMessages();
        assertEquals(Collections.singletonList(TEXT_5), Message.getStoredMessageTexts());
    }

    @Test
    public void deleteByHash_unknownHash() {
        loadTestData();
        assertEquals("No message found with hash 00:9:NOPE.", Message.deleteByHash("00:9:NOPE"));
        assertEquals(2, Message.getStoredMessageTexts().size());
    }

    // ─── 2f / Display Report ─────────────────────────────────────────────────

    @Test
    public void displayReport_showsAllSentMessages() {
        loadTestData();
        String report = Message.displayReport();

        assertTrue(report.contains("Message Hash: " + HASH_1));
        assertTrue(report.contains("Recipient: +27834557896"));
        assertTrue(report.contains("Message: " + TEXT_1));

        assertTrue(report.contains("Message Hash: " + HASH_4));
        assertTrue(report.contains("Recipient: +27830000004"));
        assertTrue(report.contains("Message: " + TEXT_4));

        assertFalse(report.contains(TEXT_2));      // stored, not sent
    }

    @Test
    public void storedMessagesReport_showsFullDetailsOfStoredMessages() {
        loadTestData();
        String report = Message.storedMessagesReport();

        assertTrue(report.contains("Message ID: 2000000002"));
        assertTrue(report.contains("Message Hash: " + HASH_2));
        assertTrue(report.contains("Sender: kt_kn"));
        assertTrue(report.contains("Recipient: +27838884567"));
        assertTrue(report.contains("Message: " + TEXT_2));
        assertTrue(report.contains("Message: " + TEXT_5));
        assertFalse(report.contains(TEXT_1));
    }

    @Test
    public void storedMessagesReport_whenNothingStored() {
        assertTrue(Message.storedMessagesReport().contains("No stored messages."));
    }
}
