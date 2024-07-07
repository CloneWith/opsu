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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class I18N {
	private static ResourceBundle tlBundle;
	private static boolean isInited = false;

	public static void init() {
		if (isInited) {
			return;
		}

		try {
			// FIXME
			File i10nDir = new File("res/i10n/");
			if (!i10nDir.exists() || !i10nDir.isDirectory()) {
				throw new RuntimeException("i10n directory not found.");
			}
			URL[] urls = { i10nDir.toURI().toURL() };
			URLClassLoader loader = new URLClassLoader(urls);
			tlBundle = UResourceBundle.getBundleInstance("messages", new ULocale(Locale.getDefault().toString()),
					loader);
		} catch (Exception e) {
			e.printStackTrace();
		}

		isInited = true;
	}

	/**
	 * Returns the translated string from the .po file.
	 *
	 * @param src source string
	 * @return A translated string
	 */
	public static String t(String src) {
		if (!isInited) {
			init();
		}

		try {
			return tlBundle.getString(src);
		} catch (Exception e) {
			// String not found in .po file, return the source string
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
}
