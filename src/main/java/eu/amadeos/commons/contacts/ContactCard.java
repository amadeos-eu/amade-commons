package eu.amadeos.commons.contacts;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What other programs read of a contact in the address books of amade-contacts: the name, the e-mail addresses,
 * the birthday, the groups, and for labels and letters the parts of the name, the job title, the phone numbers and
 * the postal addresses. Read-only; amade-contacts is the program that changes contacts.
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
 * @param parts        the parts of the name from {@code N}; all empty if the card has none
 * @param title        the job title ({@code TITLE}), may be empty
 * @param phones       the phone numbers, preferred number first
 * @param addresses    the postal addresses ({@code ADR}), preferred address first
 */
public record ContactCard(String uid, String book, String name, String nickname, String organization,
                          List<Email> emails, Birthday birthday, List<String> groups, NameParts parts,
                          String title, List<Phone> phones, List<Address> addresses) {

    /** A card with only the name, e-mail addresses, birthday and groups. */
    public ContactCard(String uid, String book, String name, String nickname, String organization,
                       List<Email> emails, Birthday birthday, List<String> groups) {
        this(uid, book, name, nickname, organization, emails, birthday, groups, null, "", List.of(), List.of());
    }

    public ContactCard {
        Objects.requireNonNull(uid, "uid");
        book = book == null ? "" : book;
        name = name == null ? "" : name.strip();
        nickname = nickname == null ? "" : nickname.strip();
        organization = organization == null ? "" : organization.strip();
        emails = emails == null ? List.of() : List.copyOf(emails);
        groups = groups == null ? List.of() : List.copyOf(groups);
        parts = parts == null ? NameParts.EMPTY : parts;
        title = title == null ? "" : title.strip();
        phones = phones == null ? List.of() : List.copyOf(phones);
        addresses = addresses == null ? List.of() : List.copyOf(addresses);
    }

    /**
     * The parts of a name, as in the vCard {@code N} property; each may be empty, never null.
     *
     * @param prefix     such as {@code Dr.}
     * @param given      the first name
     * @param additional middle names
     * @param family     the last name
     * @param suffix     such as {@code jun.}
     */
    public record NameParts(String prefix, String given, String additional, String family, String suffix) {
        public static final NameParts EMPTY = new NameParts("", "", "", "", "");

        public NameParts {
            prefix = clean(prefix);
            given = clean(given);
            additional = clean(additional);
            family = clean(family);
            suffix = clean(suffix);
        }

        public boolean isEmpty() {
            return prefix.isEmpty() && given.isEmpty() && additional.isEmpty() && family.isEmpty() && suffix.isEmpty();
        }
    }

    /**
     * A phone number.
     *
     * @param number     as written in the card
     * @param preference 1 (most preferred) to 100
     * @param types      such as {@code cell}, {@code home}, {@code work} or {@code fax}, in lower case
     */
    public record Phone(String number, int preference, List<String> types) {
        public Phone {
            Objects.requireNonNull(number, "number");
            types = types == null ? List.of() : List.copyOf(types);
        }
    }

    /**
     * A postal address, as in the vCard {@code ADR} property; each part may be empty, never null.
     *
     * @param poBox      post office box
     * @param extended   such as an apartment or "c/o"
     * @param street     street and house number
     * @param locality   the city
     * @param region     state or province
     * @param postalCode the postcode
     * @param country    the country name
     * @param preference 1 (most preferred) to 100
     * @param types      such as {@code home} or {@code work}, in lower case
     */
    public record Address(String poBox, String extended, String street, String locality, String region,
                          String postalCode, String country, int preference, List<String> types) {
        public Address {
            poBox = clean(poBox);
            extended = clean(extended);
            street = clean(street);
            locality = clean(locality);
            region = clean(region);
            postalCode = clean(postalCode);
            country = clean(country);
            types = types == null ? List.of() : List.copyOf(types);
        }

        public boolean isEmpty() {
            return poBox.isEmpty() && extended.isEmpty() && street.isEmpty() && locality.isEmpty() && region.isEmpty()
                    && postalCode.isEmpty() && country.isEmpty();
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.strip();
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

    /** The preferred postal address. */
    public Optional<Address> address() {
        return addresses.isEmpty() ? Optional.empty() : Optional.of(addresses.getFirst());
    }

    /** The preferred phone number. */
    public Optional<String> phone() {
        return phones.isEmpty() ? Optional.empty() : Optional.of(phones.getFirst().number());
    }

    /** The preferred e-mail address. */
    public Optional<String> email() {
        return emails.isEmpty() ? Optional.empty() : Optional.of(emails.getFirst().address());
    }
}
