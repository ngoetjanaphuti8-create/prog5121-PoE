package chatapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MessageManager.java
 *
 * PROG5121 - Part 3: Store Data and Display Task Report.
 *
 * Keeps every message in five arrays that are filled while the program runs
 * (nothing is hard-coded):
 *   - sentMessages          all the messages that were sent
 *   - disregardedMessages   all the messages that were disregarded
 *   - storedMessages        all the stored messages (read from the JSON file at start-up)
 *   - messageHashes         the hash of every message
 *   - messageIDs            the ID of every message
 *
 * messageHashes and messageIDs are parallel arrays: the entries at the same
 * position belong to the same message.
 *
 * The array sizes come from the number of messages the user said they would
 * enter, plus the stored messages already in the JSON file.
 */
public class MessageManager {

    private final Message[] sentMessages;
    private final Message[] disregardedMessages;
    private final Message[] storedMessages;
    private final String[] messageHashes;
    private final String[] messageIDs;

    // How many entries of each array are currently in use.
    private int sentCount = 0;
    private int disregardedCount = 0;
    private int storedCount = 0;
    private int registryCount = 0; // used by both messageHashes and messageIDs

    /**
     * Creates the arrays and reads the previously stored messages from the
     * JSON file into the stored messages array.
     *
     * @param newMessageCapacity how many new messages the user will enter
     */
    public MessageManager(int newMessageCapacity) {
        List<Message> loaded = Message.loadStoredMessages();
        int totalCapacity = loaded.size() + newMessageCapacity;

        sentMessages = new Message[newMessageCapacity];
        disregardedMessages = new Message[newMessageCapacity];
        storedMessages = new Message[totalCapacity];
        messageHashes = new String[totalCapacity];
        messageIDs = new String[totalCapacity];

        for (Message message : loaded) {
            storedMessages[storedCount++] = message;
            messageHashes[registryCount] = message.getMessageHash();
            messageIDs[registryCount] = message.getMessageID();
            registryCount++;
        }
    }

    // ---------- Populating the arrays ----------

    /**
     * Adds a message to the array that matches the option the user chose,
     * and records its hash and ID.
     *
     * @param message the message to add
     * @param flag    Message.SEND_MESSAGE, Message.DISREGARD_MESSAGE or Message.STORE_MESSAGE
     * @throws IllegalStateException    if the array for that flag is already full
     * @throws IllegalArgumentException if the flag is not one of the three options
     */
    public void addMessage(Message message, int flag) {
        if (registryCount >= messageHashes.length) {
            throw new IllegalStateException("The message hash and ID arrays are full.");
        }

        switch (flag) {
            case Message.SEND_MESSAGE:
                requireRoom(sentCount, sentMessages.length, "sent messages");
                sentMessages[sentCount++] = message;
                break;
            case Message.DISREGARD_MESSAGE:
                requireRoom(disregardedCount, disregardedMessages.length, "disregarded messages");
                disregardedMessages[disregardedCount++] = message;
                break;
            case Message.STORE_MESSAGE:
                requireRoom(storedCount, storedMessages.length, "stored messages");
                storedMessages[storedCount++] = message;
                break;
            default:
                throw new IllegalArgumentException("Unknown message option: " + flag);
        }

        messageHashes[registryCount] = message.getMessageHash();
        messageIDs[registryCount] = message.getMessageID();
        registryCount++;
    }

    private static void requireRoom(int count, int capacity, String arrayName) {
        if (count >= capacity) {
            throw new IllegalStateException("The " + arrayName + " array is full.");
        }
    }

    // ---------- Reading the arrays ----------

    public String[] getSentMessageTexts() {
        return texts(sentMessages, sentCount);
    }

    public String[] getDisregardedMessageTexts() {
        return texts(disregardedMessages, disregardedCount);
    }

    public String[] getStoredMessageTexts() {
        return texts(storedMessages, storedCount);
    }

    public String[] getMessageHashes() {
        return Arrays.copyOf(messageHashes, registryCount);
    }

    public String[] getMessageIDs() {
        return Arrays.copyOf(messageIDs, registryCount);
    }

    private static String[] texts(Message[] messages, int count) {
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = messages[i].getMessageText();
        }
        return result;
    }

    // ---------- Stored Messages menu features ----------

    /**
     * Lists the sender and recipient of every stored message.
     *
     * @param senderName the name of the logged-in user, who sent every message
     * @return one line per stored message
     */
    public String displaySenderAndRecipient(String senderName) {
        if (storedCount == 0) {
            return "There are no stored messages.";
        }
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < storedCount; i++) {
            if (i > 0) {
                lines.append("\n");
            }
            lines.append("Sender: ").append(senderName)
                    .append(" | Recipient: ").append(storedMessages[i].getRecipient());
        }
        return lines.toString();
    }

    /**
     * Finds the longest stored message.
     *
     * @return the longest stored message, or null if nothing is stored
     */
    public Message longestStoredMessage() {
        Message longest = null;
        for (int i = 0; i < storedCount; i++) {
            if (longest == null
                    || storedMessages[i].getMessageText().length() > longest.getMessageText().length()) {
                longest = storedMessages[i];
            }
        }
        return longest;
    }

    /**
     * Searches every message (sent, disregarded and stored) for a message ID.
     *
     * @param messageID the ID to look for
     * @return the matching message, or null if there is none
     */
    public Message searchByMessageID(String messageID) {
        // The ID array is checked first, so unknown IDs are rejected straight away.
        if (indexOf(messageIDs, registryCount, messageID) == -1) {
            return null;
        }
        for (int i = 0; i < sentCount; i++) {
            if (sentMessages[i].getMessageID().equals(messageID)) {
                return sentMessages[i];
            }
        }
        for (int i = 0; i < disregardedCount; i++) {
            if (disregardedMessages[i].getMessageID().equals(messageID)) {
                return disregardedMessages[i];
            }
        }
        for (int i = 0; i < storedCount; i++) {
            if (storedMessages[i].getMessageID().equals(messageID)) {
                return storedMessages[i];
            }
        }
        return null;
    }

    /**
     * Finds every sent or stored message for a recipient (sent messages first).
     *
     * @param recipient the recipient's cell number
     * @return the matching messages (an empty array if there are none)
     */
    public Message[] searchByRecipient(String recipient) {
        int matches = countRecipient(sentMessages, sentCount, recipient)
                + countRecipient(storedMessages, storedCount, recipient);
        Message[] found = new Message[matches];
        int next = 0;

        for (int i = 0; i < sentCount; i++) {
            if (sentMessages[i].getRecipient().equals(recipient)) {
                found[next++] = sentMessages[i];
            }
        }
        for (int i = 0; i < storedCount; i++) {
            if (storedMessages[i].getRecipient().equals(recipient)) {
                found[next++] = storedMessages[i];
            }
        }
        return found;
    }

    private static int countRecipient(Message[] messages, int count, String recipient) {
        int matches = 0;
        for (int i = 0; i < count; i++) {
            if (messages[i].getRecipient().equals(recipient)) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * Deletes a message using its message hash. The message is removed from
     * the array it is in, and its hash and ID are removed from the hash and ID
     * arrays. If it was a stored message the JSON file is updated as well.
     *
     * @param messageHash the hash of the message to delete
     * @return a confirmation message, or a message saying no match was found
     */
    public String deleteMessageByHash(String messageHash) {
        Message deleted = null;
        boolean wasStored = false;

        int at = indexOfHash(sentMessages, sentCount, messageHash);
        if (at != -1) {
            deleted = sentMessages[at];
            sentCount = removeAt(sentMessages, sentCount, at);
        } else {
            at = indexOfHash(disregardedMessages, disregardedCount, messageHash);
            if (at != -1) {
                deleted = disregardedMessages[at];
                disregardedCount = removeAt(disregardedMessages, disregardedCount, at);
            } else {
                at = indexOfHash(storedMessages, storedCount, messageHash);
                if (at != -1) {
                    deleted = storedMessages[at];
                    storedCount = removeAt(storedMessages, storedCount, at);
                    wasStored = true;
                }
            }
        }

        if (deleted == null) {
            return "No message found with the message hash: " + messageHash;
        }

        removeFromRegistry(deleted);

        String confirmation = "Message: \"" + deleted.getMessageText() + "\" successfully deleted.";
        if (wasStored && !Message.saveStoredMessages(storedAsList())) {
            confirmation += " (Warning: the JSON file could not be updated.)";
        }
        return confirmation;
    }

    /**
     * Builds a report with the full details of every stored message.
     *
     * @return the stored messages report
     */
    public String displayReport() {
        return buildReport("STORED MESSAGES REPORT", storedMessages, storedCount);
    }

    /**
     * Builds a report with the full details of every sent message.
     *
     * @return the sent messages report
     */
    public String displaySentReport() {
        return buildReport("SENT MESSAGES REPORT", sentMessages, sentCount);
    }

    private static String buildReport(String title, Message[] messages, int count) {
        StringBuilder report = new StringBuilder(title);
        if (count == 0) {
            return report.append("\nThere are no messages to display.").toString();
        }
        for (int i = 0; i < count; i++) {
            report.append("\n\n").append(messages[i].getDetails());
        }
        return report.toString();
    }

    // ---------- Array helpers ----------

    private static int indexOf(String[] values, int count, String target) {
        for (int i = 0; i < count; i++) {
            if (values[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfHash(Message[] messages, int count, String messageHash) {
        for (int i = 0; i < count; i++) {
            if (messages[i].getMessageHash().equals(messageHash)) {
                return i;
            }
        }
        return -1;
    }

    // Removes the element at the index by shifting later elements left,
    // and returns the new number of elements in use.
    private static <T> int removeAt(T[] array, int count, int index) {
        for (int i = index; i < count - 1; i++) {
            array[i] = array[i + 1];
        }
        array[count - 1] = null;
        return count - 1;
    }

    // Removes a message's hash and ID, which sit at the same position in the parallel arrays.
    private void removeFromRegistry(Message message) {
        for (int i = 0; i < registryCount; i++) {
            if (messageHashes[i].equals(message.getMessageHash())
                    && messageIDs[i].equals(message.getMessageID())) {
                removeAt(messageHashes, registryCount, i);
                registryCount = removeAt(messageIDs, registryCount, i);
                return;
            }
        }
    }

    private List<Message> storedAsList() {
        List<Message> list = new ArrayList<>();
        for (int i = 0; i < storedCount; i++) {
            list.add(storedMessages[i]);
        }
        return list;
    }
}
