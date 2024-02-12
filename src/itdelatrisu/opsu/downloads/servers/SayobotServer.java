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

package itdelatrisu.opsu.downloads.servers;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.newdawn.slick.util.Log;

/**
 * Download server: https://osu.sayobot.cn/home
 */
public class SayobotServer extends DownloadServer {
	/** Server name. */
	private static final String SERVER_NAME = "Sayobot CN";

	/** Formatted download URL: {@code beatmapSetID} */
	private static final String DOWNLOAD_URL = "https://txy1.sayobot.cn/beatmaps/download/full/%d";

	/**
	* Format:
	* {@params}:
	* 0=%d: Page size
	* 1=%d: Page index from 0
	* 2=%d: Beatmap list type:
	* *
	* *
	* * 4: search
	*
	* 3=%s: Query
	* 5=%d: Modes -> 1
	* 6=%d: Ranked state
	*/
	private static final String SEARCH_URL = "https://api.sayobot.cn/beatmaplist?0=%d&1=%d&2=%d&3=%s&5=%d&6=";

	/** TODO: Preview audio URL, for future use */
	private static final String PREVIEW_URL = "https://cdnx.sayobot.cn:25225/preview/%d";

	/** Maximum beatmaps displayed per page. */
	private static final int PAGE_LIMIT = 20;

	/** Total result count from the last query. */
	private int totalResults = -1;

	/** Constructor. */
	public SayobotServer() {}

	@Override
	public String getName() {
		return SERVER_NAME;
	}

	@Override
	public String getDownloadURL(int beatmapSetID) {
		return String.format(DOWNLOAD_URL, beatmapSetID);
	}

	@Override
	public DownloadNode[] resultList(String query, int page, boolean rankedOnly) throws IOException {
		DownloadNode[] nodes = null;
		try {
			// read JSON
			String search = String.format(SEARCH_URL,
				PAGE_LIMIT,
				page - 1,
				4,
				(URLEncoder.encode(query, "UTF-8")).replace("+", "%20"),
				1
			);
			if (rankedOnly) search += "1";
			String s = Utils.readDataFromUrl(new URL(search));
			JSONObject body = new JSONObject(s);
			int stat = body.getInt("status");
			if (stat != 0) {
				this.totalResults = -1;
				return null;
			}
			int endid = body.getInt("endid");
			this.totalResults = endid * PAGE_LIMIT;
			// this.totalResults = body.getInt("results");
			JSONArray arr = body.getJSONArray("data");

			nodes = new DownloadNode[arr.length()];
			for (int i = 0; i < nodes.length; i++) {
				JSONObject item = arr.getJSONObject(i);
				nodes[i] = new DownloadNode(
						item.getInt("sid"), formatDate(item.getLong("lastupdate")), // "date"
						item.getString("title"), item.isNull("titleU") ? null : item.getString("titleU"), // "titleUnicode"
						item.getString("artist"), item.isNull("artistU") ? null : item.getString("artistU"), // "artistUnicode"
						item.getString("creator"));
			}
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			ErrorHandler.error(String.format("Problem loading result list for query '%s'.", query), e, true);
		} catch (JSONException e) {
			Log.error(e);
		}
		return nodes;
	}

	@Override
	public int minQueryLength() {
		return 0;
	}

	@Override
	public int totalResults() {
		return totalResults;
	}

	@Override
	public boolean isDownloadInBrowser() {
		return false;
	}

	/**
	 * Returns a formatted date string from a raw date.
	 *
	 * @param s the raw date string (e.g. "2015-09-30 09:39:04.536")
	 * @return the formatted date, or the raw string if it could not be parsed
	 */
	private String formatDate(Long s) {
		try {
			DateFormat fmt = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
			Date date = new Date(s * 1000); // Unix timestamp
			return fmt.format(date);
		} catch (StringIndexOutOfBoundsException e) {
			return "";
		}
	}
}
