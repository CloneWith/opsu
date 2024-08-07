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

import java.io.File;
import java.util.Locale;
import java.util.Map;

public class I18N {
	private static final File baseDir = new File("res/i10n");
	private static boolean isInited = false;
	private static Map<String, String> tlMap = null;

	public static void init() { init(true); }

	public static void init(boolean status) {
		String defString = null;
		if (isInited) {
			return;
		}

		// !status -> Initial -> Use system default temporarily
		defString = (!status) ? Locale.getDefault().toString() : Options.getLanguage();

		if (defString.equals("English") || defString.contains("en")) {
			isInited = true;
			return;
		}
		if (defString.equals("LANGUAGE") || defString == null) return;

		// TODO: Another way to build Locales
		final Locale defLocale = new Locale(defString);

		final File poFile = PoReader.getPo(defLocale);
		if (poFile == null) {
			Log.warn(String.format("Translation for %s not found. Falling back to default.", defLocale));
			return;
		}
		try {
			tlMap = PoReader.getTranslationMap(poFile);
			if (!defString.equals("LANGUAGE")) isInited = true;
		} catch (Exception e) {
			Log.error("Failed to generate translation map.", e);
		}
	}

	/**
	 * Returns the translated string from the .po file.
	 *
	 * @param src source string
	 * @return A translated string, or source if untranslated
	 */
	public static String t(String src) { return t(src, isInited);}

	public static String t(String src, boolean status) {
		// if (src == "LANGUAGE" && !isInited) init(false);
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
	 *
	 * @return true is initialized
	 */
	public static boolean isInit() {
		return isInited;
	}

	public static String getFontmap() {
		return tlMap.toString();
	}

	public static File getBaseDir() {
		return baseDir;
	}
}
