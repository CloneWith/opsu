package itdelatrisu.opsu;

import gnu.gettext.GettextResource;

import java.util.ResourceBundle;

/**
 * Simple I18n implementation.
 * Source: https://github.com/sba1/simpledance/blob
 */
public class I18n {
	static private ResourceBundle locres;
	static private Boolean locap = false;

	static final private void LocaleInit() {
		try {
			locres = GettextResource.getBundle("Opsu");
		} catch (Exception e) {};
		locap = true;
	};

	/**
	 * Returns a translated string.
	 * @param str string key
	 * @return The translated string by `gettext`
	 */
	static final public String t(String str) {
		if (!locap)
			LocaleInit();
		if (locres == null)
			return str;
		return GettextResource.gettext(locres, str);
	}
}
