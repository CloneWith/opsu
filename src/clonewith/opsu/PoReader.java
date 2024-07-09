package clonewith.opsu;

import org.newdawn.slick.util.Log;

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
		final File localedFile = new File(i10nDir + poPrefix + "_" + target + ".po");
		if (!localedFile.exists()) {
			Log.error("Not found:" + localedFile);
		}

		return localedFile.exists() ? localedFile : null;
	}

	/**
	 * Returns a map of translated strings from a specific .po file.
	 * @param src source .po file
	 * @return a map of String to String
	 */
	public static Map<String, String> getTranslationMap(File src) throws Exception {
        final Map<String, String> translations = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            String line;
            StringBuilder original = null;
            StringBuilder translation = null;

            while ((line = br.readLine()) != null) {
				if (line.startsWith("\"")) {
					if (original != null && translation == null) {
						original.append(line, 1, line.length() - 1);
					} else if (translation != null) {
						translation.append(line, 1, line.length() - 1);
					}
				} else if (line.startsWith("msgstr \"")) {
					translation = new StringBuilder(line.substring(8, line.length() - 1));
				} else if (line.startsWith("msgid \"")) {
					if (original != null && translation != null) {
						translations.put(original.toString(), translation.toString());
						translation = null;
					}
					original = new StringBuilder(line.substring(7, line.length() - 1));
				}
            }
        } catch (IOException e) {
			Log.error(e);
        }
		// System.err.println(translations);
        return translations;
    }
}
