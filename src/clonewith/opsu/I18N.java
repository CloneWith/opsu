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

import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.newdawn.slick.util.Log;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.options.Options;

public class I18N {
	private static File baseDir = new File("res/i10n");
	private static boolean isInited = false;
	private static Map<String, String> tlMap = null;

	public static void init() {
		if (isInited) {
			return;
		}

		Locale defLocale = Options.getLanguage();

		if (defLocale == Locale.ENGLISH) return;

		File poFile = PoReader.getPo(defLocale);
		if (poFile == null) {
			Log.warn(String.format("Translation for %s not found. Falling back to default.", defLocale.toString()));
			return;
		}
		tlMap = PoReader.getTranslationMap(poFile);
		isInited = true;
	}

	/**
	 * Returns the translated string from the .po file.
	 *
	 * @param src source string
	 * @return A translated string
	 */
	public static String t(String src) {
		if (!isInited) return src;
		try {
			String target = tlMap.get(src);
			return target != null ? target : src;
		} catch (Exception e) {
			ErrorHandler.error("Failed to read translations.", e, true);
			return src;
		}
	}

	/**
	 * Returns if I18N framework has been initialized.
	 *
	 * @return true is initialized
	 */
	public static boolean getInitStatus() {
		return isInited;
	}

	public static String getFontmap() {
		return tlMap.toString();
	}

	public static File getBaseDir() {
		return baseDir;
	}
}
