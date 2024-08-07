/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
 * Copyright (C) 2023-2024 CloneWith
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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Project-specific constants.
 */
public class OpsuConstants {
	/** Project name. */
	public static final String PROJECT_NAME = "opsu!";

	/** Project author. */
	public static final String PROJECT_AUTHOR = "@itdelatrisu + @CloneWith";

	/** Website address. */
	public static final URI WEBSITE_URI = URI.create("https://clonewith.github.io/opsu/");

	/** Repository address. */
	public static final URI REPOSITORY_URI = URI.create("https://github.com/clonewith/opsu");

	/** Credits address. */
	public static final URI CREDITS_URI = URI.create("https://github.com/clonewith/opsu/blob/master/CREDITS.md");

	/** Issue reporting address. */
	public static final String ISSUES_URL = "https://github.com/CloneWith/opsu/discussions/new?category=bug-report&title=%s&body=%s";

	/** Address containing the latest version file. */
	public static final String VERSION_REMOTE = "https://raw.githubusercontent.com/clonewith/opsu/gh-pages/version";

	/** Changelog address. */
	private static final String CHANGELOG_URL = "https://github.com/clonewith/opsu/releases/tag/%s";

	/** Returns the changelog URI for the given version. */
	public static URI getChangelogURI(String version) {
		return URI.create(String.format(CHANGELOG_URL, URLEncoder.encode(version, StandardCharsets.UTF_8)));
	}

	// This class should not be instantiated.
	private OpsuConstants() {}
}
