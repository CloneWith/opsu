/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2024 CloneWith
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!. If not, see <http://www.gnu.org/licenses/>.
 */

package clonewith.opsu;

import itdelatrisu.opsu.options.Options;
import org.newdawn.slick.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class I18N {
	private static final File baseDir = new File("res/i10n");
	private static boolean isInited = false;
	private static Map<String, String> tlMap = null;

	public static void init() {
		if (isInited) {
			return;
		}

		String defString = getLangFromOptions();

		// Null or default: Use English
		if (defString == null || defString.equals("English") || defString.contains("en") || defString.equals("LANGUAGE")) {
			isInited = true;
			return;
		}

		final Locale defLocale = Locale.of(defString);

		final File poFile = PoReader.getPo(defLocale);
		if (poFile == null) {
			Log.warn(String.format("Translation for %s not found. Falling back to default.", defLocale));
			return;
		}
		try {
			tlMap = PoReader.getTranslationMap(poFile);
			if (!tlMap.isEmpty()) isInited = true;
		} catch (Exception e) {
			Log.error("Failed to generate translation map.", e);
		}
	}

	/**
	 * Read raw language setting data from the default configuration file.
	 * @return a language code string
	 */
	private static String getLangFromOptions() {
		File OptionsFile = Options.getOptionsFile();

		if (!OptionsFile.isFile()) {
			return null;
		}

		// Read the configuration file, but just for UILang.
		try (BufferedReader in = new BufferedReader(new FileReader(OptionsFile))) {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() < 2 || line.charAt(0) == '#')
					continue;
				int index = line.indexOf('=');
				if (index == -1)
					continue;

				// read option
				String name = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				if (name.equals("UILang")) {
					return value;
				}
			}
		} catch (IOException e) {
			Log.warn("Failed to read options file, locale falling to default.", e);
		}
		return null;
	}

	/**
	 * Returns the translated string from the .po file.
	 * @param src source string
	 * @return A translated string, or {@code src} if untranslated
	 */
	public static String t(String src) { return t(src, isInited);}

	public static String t(String src, boolean status) {
		if (!status) return src;
		try {
			String target = tlMap.get(src);
			return (target != null && !target.isBlank()) ? target : src;
		} catch (Exception e) {
			return src;
		}
	}

	/**
	 * Returns if I18N framework has been initialized.
	 * @return true is initialized
	 */
	public static boolean isInit() {
		return isInited;
	}

	public static String getFontmap() {
		if (tlMap == null) return "";
		return tlMap.toString();
	}

	public static File getBaseDir() {
		return baseDir;
	}
}
