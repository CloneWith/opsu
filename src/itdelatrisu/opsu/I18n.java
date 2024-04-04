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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Simple I18n implementation.
 *
 * @see https://github.com/sba1/simpledance
 */
public class I18n {
	static private Properties prop;
	static private Boolean locap = false;

	static private void LocaleInit() {
		/*
		Locale loc = Locale.getDefault();
		String lang = loc.getLanguage();
		String co = loc.getCountry();
		String fname = "i18n/" + lang + "_" + co + ".properties";
		prop = new Properties();
		try{
			InputStream inputStream = I18n.class.getClassLoader().getResourceAsStream(fname);
			prop.load(inputStream);
		}
		catch (IOException e) {} */
		locap = true;
	};

	/**
	 * Returns a translated string.
	 *
	 * @param str string key
	 * @return The translated string by `gettext`
	 */
	static final public String t(String str) {
		if (!locap)
			LocaleInit();
		return str;
		// if (prop == null)
		// 	return str;
		// return prop.getProperty(str);
		// return locres.getString(str);
		// return GettextResource.gettext(locres, str);
	}
}
