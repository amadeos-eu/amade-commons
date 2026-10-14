package org.datazuul.amadeos.commons.i18n;

import javax.swing.JComboBox;
import java.util.Optional;

/**
 * The language field of an application's settings dialog: "Desktop setting (Deutsch)" followed by the
 * languages under their own names. Store the selection with
 * {@link LocaleSettings#setApplicationLanguage(Optional)} and tell the user with
 * {@code commons.language.restart} when {@link LocaleSettings#restartRequired()}.
 *
 * <pre>
 * LanguageChooser language = new LanguageChooser(settings, Messages.commons(settings.locale()));
 * ...
 * settings.setApplicationLanguage(language.selectedLanguage());
 * </pre>
 */
public final class LanguageChooser extends JComboBox<LanguageChooser.Choice> {

    /** One entry; an empty language means "follow the desktop". */
    public record Choice(Optional<Language> language, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public LanguageChooser(LocaleSettings settings, Messages commons) {
        Language desktop = settings.desktopResolution().language();
        addItem(new Choice(Optional.empty(), commons.format("language.follow-desktop", desktop.nativeName())));
        for (Language language : Language.values()) {
            addItem(new Choice(Optional.of(language), language.nativeName()));
        }
        setSelectedLanguage(settings.applicationLanguage());
        getAccessibleContext().setAccessibleName(Mnemonics.strip(commons.get("language.label")));
    }

    /** The selected language; empty for "follow the desktop". */
    public Optional<Language> selectedLanguage() {
        Choice choice = (Choice) getSelectedItem();
        return choice == null ? Optional.empty() : choice.language();
    }

    public void setSelectedLanguage(Optional<Language> language) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).language().equals(language)) {
                setSelectedIndex(i);
                return;
            }
        }
    }
}
