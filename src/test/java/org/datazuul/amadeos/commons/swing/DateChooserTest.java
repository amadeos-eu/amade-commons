package org.datazuul.amadeos.commons.swing;

import org.datazuul.amadeos.commons.i18n.Messages;
import org.datazuul.amadeos.commons.i18n.TranslationCheck;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class DateChooserTest {
    @Test
    void gridCellsFollowTheFirstDayOfTheWeek() {
        YearMonth october = YearMonth.of(2026, 10); // 1 October 2026 is a Thursday
        assertNull(DateChooser.MonthGrid.dayAt(october, DayOfWeek.MONDAY, 2));
        assertEquals(LocalDate.of(2026, 10, 1), DateChooser.MonthGrid.dayAt(october, DayOfWeek.MONDAY, 3));
        assertEquals(LocalDate.of(2026, 10, 1), DateChooser.MonthGrid.dayAt(october, DayOfWeek.SUNDAY, 4));
        assertEquals(LocalDate.of(2026, 10, 31), DateChooser.MonthGrid.dayAt(october, DayOfWeek.MONDAY, 33));
        assertNull(DateChooser.MonthGrid.dayAt(october, DayOfWeek.MONDAY, 34));
    }

    @Test
    void textsExistInEveryLanguage() {
        assertEquals(Map.of(), TranslationCheck.problems(Messages.class, "org.datazuul.amadeos.commons.i18n.commons"));
        assertEquals("Datum wählen", Messages.commons(Locale.GERMANY).get("date.choose"));
    }

    @Test
    void selectionNavigationAndResult() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            DateChooser chooser = new DateChooser(null, LocalDate.of(2026, 10, 18), Locale.GERMANY);
            try {
                assertEquals(YearMonth.of(2026, 10), chooser.grid().month());
                chooser.grid().showMonth(YearMonth.of(2026, 2));
                assertEquals(LocalDate.of(2026, 2, 18), chooser.grid().selected(), "the day is kept");
                chooser.grid().setSelected(LocalDate.of(2026, 1, 31));
                chooser.grid().showMonth(YearMonth.of(2026, 2));
                assertEquals(LocalDate.of(2026, 2, 28), chooser.grid().selected(), "clamped to the month");
                assertEquals(Optional.empty(), chooser.result());
                chooser.accept(chooser.grid().selected());
                assertEquals(Optional.of(LocalDate.of(2026, 2, 28)), chooser.result());
            } finally {
                chooser.dispose();
            }
        });
    }
}
