package chatapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageManagerTest.java
 *
 * Unit tests for the MessageManager class, using the test data supplied in
 * the PROG5121 Part 3 brief:
 *
 *   Message 1: +27834557896, "Did you get the cake?"                                   (Sent)
 *   Message 2: +27838884567, "Where are you? You are late! I have asked you to be on time." (Stored)
 *   Message 3: +27834484567, "Yohoooo, I am at your gate."                              (Disregard)
 *   Message 4: 0838884567,   "It is dinner time!"                                       (Sent)
 *   Message 5: +27838884567, "Ok, I am leaving without you."                            (Stored)
 *
 * The brief lists message 4 as "Developer 0838884567" and searches for that
 * number as a message ID, so 0838884567 is used as both its ID and recipient.
 */
class MessageManagerTest {

    private static final String TEXT_1 = "Did you get the cake?";
    private static final String TEXT_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String TEXT_3 = "Yohoooo, I am at your gate.";
    private static final String TEXT_4 = "It is dinner time!";
    private static final String TEXT_5 = "Ok, I am leaving without you.";

    private static final String RECIPIENT_2_AND_5 = "+27838884567";
    private static final String MESSAGE_4_ID = "0838884567";

    @TempDir
    Path tempDir;

    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() {
        // Every test gets its own JSON file, so the real storage file is never touched.
        Message.resetSentMessages();
        Message.setStorageFile(tempDir.resolve("storedMessages.json"));

        message1 = new Message("1000000001", 0, "+27834557896", TEXT_1);
        message2 = new Message("1000000002", 1, RECIPIENT_2_AND_5, TEXT_2);
        message3 = new Message("1000000003", 2, "+27834484567", TEXT_3);
        message4 = new Message(MESSAGE_4_ID, 3, "0838884567", TEXT_4);
        message5 = new Message("1000000005", 4, RECIPIENT_2_AND_5, TEXT_5);
    }

    // Adds the five test messages with the flags from the brief.
    private MessageManager populatedManager() {
        MessageManager manager = new MessageManager(5);
        manager.addMessage(message1, Message.SEND_MESSAGE);
        manager.addMessage(message2, Message.STORE_MESSAGE);
        manager.addMessage(message3, Message.DISREGARD_MESSAGE);
        manager.addMessage(message4, Message.SEND_MESSAGE);
        manager.addMessage(message5, Message.STORE_MESSAGE);
        return manager;
    }

    private static String[] textsOf(Message[] messages) {
        String[] texts = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            texts[i] = messages[i].getMessageText();
        }
        return texts;
    }

    // ---------- Arrays correctly populated ----------

    @Test
    void testSentMessagesArrayCorrectlyPopulated() {
        MessageManager manager = populatedManager();
        assertArrayEquals(new String[] {"Did you get the cake?", "It is dinner time!"},
                manager.getSentMessageTexts());
    }

    @Test
    void testDisregardedMessagesArrayCorrectlyPopulated() {
        MessageManager manager = populatedManager();
        assertArrayEquals(new String[] {"Yohoooo, I am at your gate."},
                manager.getDisregardedMessageTexts());
    }

    @Test
    void testStoredMessagesArrayCorrectlyPopulated() {
        MessageManager manager = populatedManager();
        assertArrayEquals(new String[] {TEXT_2, TEXT_5}, manager.getStoredMessageTexts());
    }

    @Test
    void testMessageHashArrayCorrectlyPopulated() {
        MessageManager manager = populatedManager();
        assertArrayEquals(new String[] {"10:0:DIDCAKE", "10:1:WHERETIME", "10:2:YOHOOOOGATE",
                "08:3:ITTIME", "10:4:OKYOU"}, manager.getMessageHashes());
    }

    @Test
    void testMessageIDArrayCorrectlyPopulated() {
        MessageManager manager = populatedManager();
        assertArrayEquals(new String[] {"1000000001", "1000000002", "1000000003",
                MESSAGE_4_ID, "1000000005"}, manager.getMessageIDs());
    }

    @Test
    void testStoredMessagesAreReadFromTheJsonFile() {
        message2.sentMessage(Message.STORE_MESSAGE);
        message5.sentMessage(Message.STORE_MESSAGE);

        // A new manager reads the JSON file into the stored messages array.
        MessageManager manager = new MessageManager(0);

        assertArrayEquals(new String[] {TEXT_2, TEXT_5}, manager.getStoredMessageTexts());
        assertArrayEquals(new String[] {"10:1:WHERETIME", "10:4:OKYOU"}, manager.getMessageHashes());
    }

    @Test
    void testLoadedStoredMessagesLeaveRoomForNewMessages() {
        message2.sentMessage(Message.STORE_MESSAGE);

        MessageManager manager = new MessageManager(1);
        manager.addMessage(message5, Message.STORE_MESSAGE);

        assertArrayEquals(new String[] {TEXT_2, TEXT_5}, manager.getStoredMessageTexts());
    }

    @Test
    void testAddingMoreMessagesThanTheArrayHoldsIsRejected() {
        MessageManager manager = new MessageManager(1);
        manager.addMessage(message1, Message.SEND_MESSAGE);

        assertThrows(IllegalStateException.class,
                () -> manager.addMessage(message4, Message.SEND_MESSAGE));
    }

    @Test
    void testUnknownMessageFlagIsRejected() {
        MessageManager manager = new MessageManager(1);
        assertThrows(IllegalArgumentException.class, () -> manager.addMessage(message1, 9));
    }

    // ---------- Sender and recipient of stored messages ----------

    @Test
    void testDisplaySenderAndRecipient() {
        MessageManager manager = populatedManager();
        assertEquals("Sender: Kyle Bezuidenhout | Recipient: +27838884567\n"
                + "Sender: Kyle Bezuidenhout | Recipient: +27838884567",
                manager.displaySenderAndRecipient("Kyle Bezuidenhout"));
    }

    @Test
    void testDisplaySenderAndRecipient_noStoredMessages() {
        MessageManager manager = new MessageManager(1);
        assertEquals("There are no stored messages.",
                manager.displaySenderAndRecipient("Kyle Bezuidenhout"));
    }

    // ---------- Longest message ----------

    @Test
    void testLongestStoredMessage() {
        MessageManager manager = populatedManager();
        assertEquals("Where are you? You are late! I have asked you to be on time.",
                manager.longestStoredMessage().getMessageText());
    }

    @Test
    void testLongestStoredMessage_noneStored() {
        MessageManager manager = new MessageManager(1);
        assertNull(manager.longestStoredMessage());
    }

    // ---------- Search for a message ID ----------

    @Test
    void testSearchForMessageID() {
        MessageManager manager = populatedManager();
        Message found = manager.searchByMessageID(MESSAGE_4_ID);

        assertNotNull(found);
        assertEquals("It is dinner time!", found.getMessageText());
        assertEquals("0838884567", found.getRecipient());
    }

    @Test
    void testSearchForMessageID_storedMessageCanBeFound() {
        MessageManager manager = populatedManager();
        assertEquals(TEXT_2, manager.searchByMessageID("1000000002").getMessageText());
    }

    @Test
    void testSearchForMessageID_unknownID() {
        MessageManager manager = populatedManager();
        assertNull(manager.searchByMessageID("9999999999"));
    }

    // ---------- Search for a recipient ----------

    @Test
    void testSearchForRecipient() {
        MessageManager manager = populatedManager();
        Message[] found = manager.searchByRecipient(RECIPIENT_2_AND_5);

        assertArrayEquals(new String[] {
            "Where are you? You are late! I have asked you to be on time.",
            "Ok, I am leaving without you."}, textsOf(found));
    }

    @Test
    void testSearchForRecipient_includesSentMessages() {
        MessageManager manager = populatedManager();
        assertArrayEquals(new String[] {TEXT_1}, textsOf(manager.searchByRecipient("+27834557896")));
    }

    @Test
    void testSearchForRecipient_disregardedMessagesAreNotIncluded() {
        MessageManager manager = populatedManager();
        assertEquals(0, manager.searchByRecipient("+27834484567").length);
    }

    // ---------- Delete using the message hash ----------

    @Test
    void testDeleteMessageUsingMessageHash() {
        MessageManager manager = populatedManager();

        String result = manager.deleteMessageByHash(message2.getMessageHash());

        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time.\""
                + " successfully deleted.", result);
        assertArrayEquals(new String[] {TEXT_5}, manager.getStoredMessageTexts());
    }

    @Test
    void testDeleteMessage_removesItsHashAndIDFromTheArrays() {
        MessageManager manager = populatedManager();

        manager.deleteMessageByHash(message2.getMessageHash());

        assertArrayEquals(new String[] {"10:0:DIDCAKE", "10:2:YOHOOOOGATE", "08:3:ITTIME", "10:4:OKYOU"},
                manager.getMessageHashes());
        assertArrayEquals(new String[] {"1000000001", "1000000003", MESSAGE_4_ID, "1000000005"},
                manager.getMessageIDs());
    }

    @Test
    void testDeleteStoredMessage_updatesTheJsonFile() {
        MessageManager manager = populatedManager();

        manager.deleteMessageByHash(message2.getMessageHash());

        List<Message> inFile = Message.loadStoredMessages();
        assertEquals(1, inFile.size());
        assertEquals(TEXT_5, inFile.get(0).getMessageText());
    }

    @Test
    void testDeleteSentMessage_leavesTheJsonFileAlone() {
        MessageManager manager = populatedManager();

        String result = manager.deleteMessageByHash(message1.getMessageHash());

        assertEquals("Message: \"Did you get the cake?\" successfully deleted.", result);
        assertArrayEquals(new String[] {TEXT_4}, manager.getSentMessageTexts());
        assertFalse(Files.exists(tempDir.resolve("storedMessages.json")));
    }

    @Test
    void testDeleteMessage_unknownHash() {
        MessageManager manager = populatedManager();
        assertEquals("No message found with the message hash: 99:9:NOPE",
                manager.deleteMessageByHash("99:9:NOPE"));
    }

    // ---------- Reports ----------

    @Test
    void testDisplayReport_listsAllStoredMessages() {
        MessageManager manager = populatedManager();
        assertEquals("STORED MESSAGES REPORT\n\n" + message2.getDetails()
                + "\n\n" + message5.getDetails(), manager.displayReport());
    }

    @Test
    void testDisplaySentReport_listsAllSentMessages() {
        MessageManager manager = populatedManager();
        String report = manager.displaySentReport();

        assertEquals("SENT MESSAGES REPORT\n\n" + message1.getDetails()
                + "\n\n" + message4.getDetails(), report);
    }

    @Test
    void testDisplaySentReport_showsHashRecipientAndMessage() {
        MessageManager manager = populatedManager();
        String report = manager.displaySentReport();

        assertTrue(report.contains("Message Hash: 10:0:DIDCAKE"));
        assertTrue(report.contains("Recipient: +27834557896"));
        assertTrue(report.contains("Message: Did you get the cake?"));
    }

    @Test
    void testDisplayReport_noMessages() {
        MessageManager manager = new MessageManager(1);
        assertEquals("STORED MESSAGES REPORT\nThere are no messages to display.",
                manager.displayReport());
    }
}
