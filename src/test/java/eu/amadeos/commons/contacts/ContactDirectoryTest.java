package eu.amadeos.commons.contacts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContactDirectoryTest {
    @TempDir
    Path root;

    /** A clock the test moves forward by hand. */
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-23T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private Path write(String book, String file, String text) throws IOException {
        Path folder = Files.createDirectories(root.resolve(book));
        return Files.writeString(folder.resolve(file), text.replace("\n", "\r\n"));
    }

    private static String card(String uid, String... lines) {
        return "BEGIN:VCARD\nVERSION:4.0\nUID:" + uid + "\n" + String.join("\n", lines) + "\nEND:VCARD\n";
    }

    @Test
    void readsNamesAddressesBirthdaysAndGroups() throws IOException {
        write("personal", "a.vcf", card("urn:uuid:a1", "FN:Anna Berger", "N:Berger;Anna;;;",
                "EMAIL;TYPE=work:anna@firma.example", "EMAIL;TYPE=home;PREF=1:anna@example.org",
                "BDAY:19861114", "CATEGORIES:Familie,Chor"));
        write("personal", "b.vcf", card("b2", "N:Müller;Jörg;;Dr.;", "EMAIL:joerg@example.de", "BDAY:--0229",
                "CATEGORIES:Chor"));
        write("work", "c.vcf", card("c3", "ORG:Bäckerei Sonne;Verkauf", "EMAIL:info@sonne.example"));

        List<ContactCard> all = new ContactDirectory(root, new TestClock()).contacts();

        assertEquals(List.of("Anna Berger", "Bäckerei Sonne, Verkauf", "Dr. Jörg Müller"),
                all.stream().map(ContactCard::label).toList());
        ContactCard anna = all.getFirst();
        assertEquals("a1", anna.uid());
        assertEquals("personal", anna.book());
        assertEquals(List.of("anna@example.org", "anna@firma.example"),
                anna.emails().stream().map(ContactCard.Email::address).toList());
        assertEquals(new ContactCard.Birthday(MonthDay.of(11, 14), 1986), anna.birthday());
        assertEquals(List.of("Familie", "Chor"), anna.groups());
        assertEquals("work", all.get(1).book());
        assertFalse(all.get(2).birthday().hasYear());
    }

    @Test
    void readsOlderVCardVersions() {
        String v21 = """
                BEGIN:VCARD
                VERSION:2.1
                N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:Gr=C3=BCn;J=C3=BCrgen;;;
                EMAIL;INTERNET;PREF:juergen@example.de
                EMAIL;INTERNET:j.gruen@firma.example
                BDAY:1604-03-02
                END:VCARD
                BEGIN:VCARD
                VERSION:3.0
                FN:Claire O\\, Neil
                item1.EMAIL;type=INTERNET;type=pref:
                 claire@example.fr
                NOTE:Line one\\nline two
                END:VCARD
                """;
        List<ContactCard> cards = VCardReader.read(v21, "imported", "file");

        assertEquals(2, cards.size());
        assertEquals("Jürgen Grün", cards.get(0).name());
        assertEquals("file", cards.get(0).uid());
        assertEquals("juergen@example.de", cards.get(0).email().orElseThrow());
        assertEquals(new ContactCard.Birthday(MonthDay.of(3, 2), 0), cards.get(0).birthday());
        assertEquals("Claire O, Neil", cards.get(1).name());
        assertEquals("file-1", cards.get(1).uid());
        assertEquals("claire@example.fr", cards.get(1).email().orElseThrow());
    }

    @Test
    void readsChangedFilesAgainAfterTwoSeconds() throws IOException {
        Path file = write("personal", "a.vcf", card("a", "FN:Anna", "EMAIL:anna@example.org"));
        TestClock clock = new TestClock();
        ContactDirectory directory = new ContactDirectory(root, clock);
        assertEquals("Anna", directory.contacts().getFirst().name());

        Files.writeString(file, card("a", "FN:Anna Berger", "EMAIL:anna@example.org"));
        Files.setLastModifiedTime(file, FileTime.from(Instant.parse("2026-10-23T12:00:05Z")));
        write("personal", "b.vcf", card("b", "FN:Bert"));
        assertEquals("Anna", directory.contacts().getFirst().name(), "not looked at again within two seconds");

        clock.now = clock.now.plus(Duration.ofSeconds(2));
        assertEquals(List.of("Anna Berger", "Bert"), directory.contacts().stream().map(ContactCard::name).toList());

        Files.delete(file);
        directory.refresh();
        assertEquals(List.of("Bert"), directory.contacts().stream().map(ContactCard::name).toList());
    }

    @Test
    void withoutAddressBooksThereAreNoContacts() {
        assertEquals(List.of(), new ContactDirectory(root.resolve("missing"), new TestClock()).contacts());
    }

    @Test
    void readsLatin1FilesOfOldPrograms() throws IOException {
        Files.createDirectories(root.resolve("old"));
        Files.write(root.resolve("old/x.vcf"), "BEGIN:VCARD\r\nVERSION:2.1\r\nFN:Großmann\r\nEND:VCARD\r\n"
                .getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("Großmann", new ContactDirectory(root, new TestClock()).contacts().getFirst().name());
    }

    // --8<-- [start:complete-test]
    @Test
    void completesNamesWordsAddressesAndGroups() throws IOException {
        write("personal", "a.vcf", card("a", "FN:Anna Berger", "EMAIL;PREF=1:anna@example.org",
                "EMAIL:a.berger@firma.example", "CATEGORIES:Chor"));
        write("personal", "b.vcf", card("b", "FN:Jörg Müller", "NICKNAME:Mulle", "EMAIL:joerg@example.de",
                "CATEGORIES:Chor"));
        write("personal", "c.vcf", card("c", "FN:Hannah Arendt", "EMAIL:hannah@example.org"));
        write("personal", "d.vcf", card("d", "FN:Ohne Adresse"));
        ContactDirectory directory = new ContactDirectory(root, new TestClock());

        assertEquals(List.of("Anna Berger <anna@example.org>", "Anna Berger <a.berger@firma.example>"),
                texts(directory.complete("an", 8)), "the preferred address first");
        assertEquals(List.of("Hannah Arendt <hannah@example.org>"), texts(directory.complete("ar", 8)));
        assertEquals(List.of("Jörg Müller <joerg@example.de>"), texts(directory.complete("mull", 8)));
        assertEquals(List.of("Anna Berger <a.berger@firma.example>"), texts(directory.complete("firma", 8)));
        assertEquals(List.of(), directory.complete("ohne", 8), "a contact without an address is no recipient");

        Recipient choir = directory.complete("cho", 8).getFirst();
        assertTrue(choir.group());
        assertEquals("Chor", choir.name());
        assertEquals("Anna Berger <anna@example.org>, Jörg Müller <joerg@example.de>", choir.text());

        assertEquals(1, directory.complete("a", 1).size());
        assertEquals(List.of(), directory.complete("  ", 8));
    }
    // --8<-- [end:complete-test]

    private static List<String> texts(List<Recipient> recipients) {
        return recipients.stream().map(Recipient::text).toList();
    }

    @Test
    void mailboxesQuoteNamesWithSpecialCharacters() {
        assertEquals("anna@example.org", Recipient.mailbox("", "anna@example.org"));
        assertEquals("Anna Berger <anna@example.org>", Recipient.mailbox("Anna Berger", "anna@example.org"));
        assertEquals("\"Berger, Anna\" <anna@example.org>", Recipient.mailbox("Berger, Anna", "anna@example.org"));
    }

    @Test
    void birthdaysInARangeWithAgesAndLeapDays() throws IOException {
        write("personal", "a.vcf", card("a", "FN:Anna", "BDAY:1986-11-14"));
        write("personal", "b.vcf", card("b", "FN:Bert", "BDAY:--0229"));
        write("personal", "c.vcf", card("c", "FN:Carla", "BDAY:2030-01-01"));
        ContactDirectory directory = new ContactDirectory(root, new TestClock());

        List<ContactDirectory.BirthdayOn> year = directory.birthdays(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));
        assertEquals(List.of(LocalDate.of(2027, 2, 28), LocalDate.of(2027, 11, 14)),
                year.stream().map(ContactDirectory.BirthdayOn::date).toList(), "Carla is not born yet");
        assertEquals(java.util.Optional.empty(), year.get(0).age());
        assertEquals(java.util.Optional.of(41), year.get(1).age());

        assertEquals(LocalDate.of(2028, 2, 29),
                directory.birthdays(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 3, 1)).getFirst().date());
        assertEquals(2, directory.birthdays(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 3, 1)).size());
    }
}
