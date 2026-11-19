package chatapp;

import java.util.Scanner;

/**
 * Main.java
 *
 * Console entry point for the PROG5121 PoE - QuickChat.
 *
 * The user registers and logs in (Part 1), then uses QuickChat to send,
 * disregard and store messages (Part 2). Every message is kept in arrays,
 * and the Stored Messages menu lets the user search, delete and report on
 * them (Part 3).
 */
public class Main {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Login login = new Login();

        registerNewUser(scanner, login);

        if (!logIn(scanner, login)) {
            System.out.println("Too many failed login attempts. Goodbye.");
            scanner.close();
            return;
        }

        String senderName = login.getFirstName() + " " + login.getLastName();
        runQuickChat(scanner, senderName);
        scanner.close();
    }

    // ---------- Part 1: registration and login ----------

    private static void registerNewUser(Scanner scanner, Login login) {
        System.out.println("=== Registration ===");

        System.out.print("Enter your first name: ");
        String firstName = scanner.nextLine();

        System.out.print("Enter your last name: ");
        String lastName = scanner.nextLine();

        String username;
        do {
            System.out.print("Enter a username (must contain an underscore and be no more than 5 characters): ");
            username = scanner.nextLine();
            if (!login.checkUserName(username)) {
                System.out.println("Username is not correctly formatted; please ensure that your "
                        + "username contains an underscore and is no more than five characters in length.");
            }
        } while (!login.checkUserName(username));

        String password;
        do {
            System.out.print("Enter a password (min 8 characters, 1 capital letter, 1 number, 1 special character): ");
            password = scanner.nextLine();
            if (!login.checkPasswordComplexity(password)) {
                System.out.println("Password is not correctly formatted; please ensure that the "
                        + "password contains at least eight characters, a capital letter, a number, "
                        + "and a special character.");
            }
        } while (!login.checkPasswordComplexity(password));

        String cellphoneNumber;
        do {
            System.out.print("Enter your cell phone number (with international code, e.g. +27838968976): ");
            cellphoneNumber = scanner.nextLine();
            if (!login.checkCellPhoneNumber(cellphoneNumber)) {
                System.out.println("Cell number is incorrectly formatted or does not contain "
                        + "international code; please correct the number and try again.");
            }
        } while (!login.checkCellPhoneNumber(cellphoneNumber));

        System.out.println(login.registerUser(firstName, lastName, username, password, cellphoneNumber));
    }

    // Gives the user a limited number of attempts to log in.
    private static boolean logIn(Scanner scanner, Login login) {
        System.out.println("\n=== Please log in ===");
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            boolean loggedIn = login.loginUser(username, password);
            System.out.println(login.returnLoginStatus(loggedIn));
            if (loggedIn) {
                return true;
            }
        }
        return false;
    }

    // ---------- Part 2: QuickChat ----------

    private static void runQuickChat(Scanner scanner, String senderName) {
        System.out.println("\nWelcome to QuickChat.");

        int numMessages = readNumber(scanner, "How many messages do you wish to enter? ", 1, 1000);

        // The arrays are sized from the number of messages the user chose, and
        // the stored messages already in the JSON file are read into them.
        MessageManager manager = new MessageManager(numMessages);
        int alreadyStored = manager.getStoredMessageTexts().length;
        if (alreadyStored > 0) {
            System.out.println(alreadyStored + " previously stored message(s) loaded from the JSON file.");
        }

        int messagesEntered = 0;
        boolean running = true;
        while (running) {
            System.out.println("\n1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");
            int choice = readNumber(scanner, "Choose an option: ", 1, 4);

            switch (choice) {
                case 1:
                    if (messagesEntered >= numMessages) {
                        System.out.println("You have already entered all " + numMessages + " message(s).");
                    } else {
                        messagesEntered = sendMessages(scanner, numMessages, messagesEntered, manager);
                        System.out.println("\nTotal number of messages sent: " + Message.returnTotalMessages());
                    }
                    break;
                case 2:
                    System.out.println("Coming Soon.");
                    break;
                case 4:
                    runStoredMessagesMenu(scanner, manager, senderName);
                    break;
                default:
                    System.out.println("Goodbye.");
                    running = false;
                    break;
            }
        }
    }

    // Lets the user enter the messages they said they wanted to send. The for
    // loop counter is the message number used in the message hash.
    private static int sendMessages(Scanner scanner, int numMessages, int startIndex,
                                    MessageManager manager) {
        for (int i = startIndex; i < numMessages; i++) {
            System.out.println("\n--- Message " + (i + 1) + " of " + numMessages + " ---");

            String recipient = readValidRecipient(scanner);
            String text = readValidMessage(scanner);

            Message message = new Message(i, recipient, text);
            System.out.println(message.messageIDGeneratedMessage());

            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            int option = readNumber(scanner, "Choose an option: ", 1, 3);

            System.out.println(message.sentMessage(option));

            if (option == Message.DISREGARD_MESSAGE) {
                confirmDeletion(scanner);
            } else if (option == Message.SEND_MESSAGE) {
                System.out.println("\n" + message.getDetails());
            }

            // Part 3: the message also goes into the matching array.
            manager.addMessage(message, option);
        }
        return numMessages;
    }

    private static String readValidRecipient(Scanner scanner) {
        while (true) {
            System.out.print("Enter the recipient's cell number (with international code): ");
            String recipient = scanner.nextLine().trim();
            String result = Message.checkRecipientCell(recipient);
            System.out.println(result);
            if (result.equals("Cell phone number successfully captured.")) {
                return recipient;
            }
        }
    }

    private static String readValidMessage(Scanner scanner) {
        while (true) {
            System.out.print("Enter your message (max 250 characters): ");
            String text = scanner.nextLine();
            if (text.trim().isEmpty()) {
                System.out.println("A message cannot be empty.");
                continue;
            }
            String result = Message.checkMessageLength(text);
            if (result.equals("Message ready to send.")) {
                System.out.println(result);
                return text;
            }
            System.out.println("Please enter a message of less than 250 characters.");
            System.out.println(result);
        }
    }

    // The user has to enter 0 to confirm that the message must be deleted.
    private static void confirmDeletion(Scanner scanner) {
        String input;
        do {
            System.out.print("> ");
            input = scanner.nextLine().trim();
        } while (!input.equals("0"));
        System.out.println("Message deleted.");
    }

    // ---------- Part 3: Stored Messages ----------

    private static void runStoredMessagesMenu(Scanner scanner, MessageManager manager,
                                              String senderName) {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== Stored Messages ===");
            System.out.println("1) Display the sender and recipient of all stored messages");
            System.out.println("2) Display the longest stored message");
            System.out.println("3) Search for a message ID");
            System.out.println("4) Search for all messages sent or stored for a recipient");
            System.out.println("5) Delete a message using its message hash");
            System.out.println("6) Display a report of all stored messages");
            System.out.println("7) Display a report of all sent messages");
            System.out.println("0) Back to the main menu");
            int choice = readNumber(scanner, "Choose an option: ", 0, 7);

            switch (choice) {
                case 1:
                    System.out.println(manager.displaySenderAndRecipient(senderName));
                    break;
                case 2:
                    showLongestStoredMessage(manager);
                    break;
                case 3:
                    searchForMessageID(scanner, manager);
                    break;
                case 4:
                    searchForRecipient(scanner, manager);
                    break;
                case 5:
                    System.out.print("Enter the message hash: ");
                    String hash = scanner.nextLine().trim();
                    System.out.println(manager.deleteMessageByHash(hash));
                    break;
                case 6:
                    System.out.println(manager.displayReport());
                    break;
                case 7:
                    System.out.println(manager.displaySentReport());
                    break;
                default:
                    back = true;
                    break;
            }
        }
    }

    private static void showLongestStoredMessage(MessageManager manager) {
        Message longest = manager.longestStoredMessage();
        if (longest == null) {
            System.out.println("There are no stored messages.");
        } else {
            System.out.println("Longest stored message: " + longest.getMessageText());
        }
    }

    private static void searchForMessageID(Scanner scanner, MessageManager manager) {
        System.out.print("Enter the message ID: ");
        String messageID = scanner.nextLine().trim();
        Message found = manager.searchByMessageID(messageID);
        if (found == null) {
            System.out.println("No message found with the message ID: " + messageID);
        } else {
            System.out.println("Recipient: " + found.getRecipient());
            System.out.println("Message: " + found.getMessageText());
        }
    }

    private static void searchForRecipient(Scanner scanner, MessageManager manager) {
        System.out.print("Enter the recipient's cell number: ");
        String recipient = scanner.nextLine().trim();
        Message[] found = manager.searchByRecipient(recipient);
        if (found.length == 0) {
            System.out.println("No sent or stored messages found for " + recipient);
        } else {
            for (int i = 0; i < found.length; i++) {
                System.out.println("Message " + (i + 1) + ": " + found[i].getMessageText());
            }
        }
    }

    // Keeps asking until the user enters a whole number between min and max.
    private static int readNumber(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Not a number - fall through and ask again.
            }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }
}
