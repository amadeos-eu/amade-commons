package eu.amadeos.commons.contacts;

import java.util.List;
import java.util.Objects;

/**
 * One suggestion of the address completion: a person with one of their addresses, or a group with the preferred
 * address of every member.
 *
 * @param name      the contact's or the group's name, may be empty for a contact without one
 * @param address   the e-mail address; for a group the number of members, as text
 * @param mailboxes what goes into the field: {@code Anna Berger <anna@example.org>}, one per member for a group
 * @param group     true for a group
 */
public record Recipient(String name, String address, List<String> mailboxes, boolean group) {

    public Recipient {
        name = name == null ? "" : name;
        Objects.requireNonNull(address, "address");
        mailboxes = List.copyOf(mailboxes);
    }

    /** The text for an address field: the mailboxes separated by commas. */
    public String text() {
        return String.join(", ", mailboxes);
    }

    // --8<-- [start:mailbox]
    /** "Anna Berger &lt;anna@example.org&gt;", the name in quotes if it has characters RFC 5322 does not allow bare. */
    public static String mailbox(String name, String address) {
        if (name == null || name.isBlank() || name.equalsIgnoreCase(address)) return address;
        String n = name.matches("[\\p{L}\\p{N} .'-]*") ? name : "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        return n + " <" + address + ">";
    }
    // --8<-- [end:mailbox]
}
