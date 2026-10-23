package eu.amadeos.commons.contacts;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What other programs read of a contact in the address books of amade-contacts: the name, the e-mail addresses,
 * the birthday and the groups. Read-only; amade-contacts is the program that changes contacts.
 *
 * @param uid          the vCard {@code UID} (or the file name, if the card has none)
 * @param book         the folder of the address book the contact is in
 * @param name         the name to show: {@code FN}, else the name from {@code N}, else organization or nickname;
 *                     may be empty
 * @param nickname     the first {@code NICKNAME}, may be empty
 * @param organization {@code ORG}, may be empty
 * @param emails       preferred address first
 * @param birthday     null if unknown
 * @param groups       the {@code CATEGORIES}, which amade-contacts shows as groups
 */
public record ContactCard(String uid, String book, String name, String nickname, String organization,
                          List<Email> emails, Birthday birthday, List<String> groups) {

    public ContactCard {
        Objects.requireNonNull(uid, "uid");
        book = book == null ? "" : book;
        name = name == null ? "" : name.strip();
        nickname = nickname == null ? "" : nickname.strip();
        organization = organization == null ? "" : organization.strip();
        emails = emails == null ? List.of() : List.copyOf(emails);
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    /**
     * An e-mail address.
     *
     * @param preference 1 (most preferred) to 100 (vCard {@code PREF}; 100 if the card says nothing)
     * @param types      such as {@code home} or {@code work}, in lower case
     */
    public record Email(String address, int preference, List<String> types) {
        public Email {
            Objects.requireNonNull(address, "address");
            types = types == null ? List.of() : List.copyOf(types);
        }
    }

    /**
     * A birthday, with or without the year.
     *
     * @param year 0 if unknown
     */
    public record Birthday(MonthDay day, int year) {
        public Birthday {
            Objects.requireNonNull(day, "day");
        }

        public boolean hasYear() {
            return year != 0;
        }

        /** The day it is celebrated in {@code inYear}; 29 February is on 28 February in other years. */
        public LocalDate on(int inYear) {
            return day.isValidYear(inYear) ? day.atYear(inYear) : LocalDate.of(inYear, 2, 28);
        }

        /** The age reached on the birthday in {@code inYear}, if the year of birth is known and not later. */
        public Optional<Integer> ageIn(int inYear) {
            return hasYear() && inYear >= year ? Optional.of(inYear - year) : Optional.empty();
        }
    }

    /** The name, or the first address for a contact without one. */
    public String label() {
        if (!name.isEmpty()) return name;
        return emails.isEmpty() ? "" : emails.getFirst().address();
    }

    /** The preferred e-mail address. */
    public Optional<String> email() {
        return emails.isEmpty() ? Optional.empty() : Optional.of(emails.getFirst().address());
    }
}
