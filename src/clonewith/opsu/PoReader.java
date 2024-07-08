package clonewith.opsu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PoReader {
	/** The default i10n directory. */
	private static final String i10nDir = "res/i10n/";

	/** The .po file prefix. */
	private static final String poPrefix = "messages";

	/**
	 * Returns a File object pointing to a .po file.
	 * @param target a specific locale
	 * @return a .po File, or null if not found
	 */
	public static File getPo(Locale target) {
		String locale = Locale.getDefault().toString();
		File localedFile = new File(i10nDir + poPrefix + "_" + locale + ".po");
		if (!localedFile.exists()) {
			System.err.println("Not found:" + localedFile);
		}

		return localedFile.exists() ? localedFile : null;
	}

	/**
	 * Returns a map of translated strings from a specific .po file.
	 * @param src source .po file
	 * @return a map of String to String
	 */
	public static Map<String, String> getTranslationMap(File src) {
        Map<String, String> translations = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            String line;
            String original = null;
            String translation = null;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("msgid \"")) {
                    original = line.substring(7, line.length() - 1);
                } else if (line.startsWith("msgstr \"")) {
                    translation = line.substring(8, line.length() - 1);
                    if (original != null && !translation.isEmpty()) {
                        translations.put(original, translation);
                        original = null;
                        translation = null;
                    }
                } else if (line.startsWith("\"")) {
					if (translation == null) {
						original += line.substring(1, line.length() - 1);
					} else {
						translation += line.substring(1, line.length());
					}
				}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		// System.err.println(translations);
        return translations;
    }
}
