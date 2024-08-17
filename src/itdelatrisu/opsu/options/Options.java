/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
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

package itdelatrisu.opsu.options;

import clonewith.opsu.I18N;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import itdelatrisu.opsu.*;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.TimingPoint;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.skins.SkinLoader;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.ClasspathLocation;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static clonewith.opsu.I18N.t;

/**
 * Handles all user options.
 */
public class Options {
	/** Whether to use XDG directories. */
	public static final boolean USE_XDG = checkXDGFlag();

	private static final String DISABLED = t("Disabled");

	/** The config directory. */
	private static final File CONFIG_DIR = getXDGBaseDir("XDG_CONFIG_HOME", ".config");

	/** The data directory. */
	private static final File DATA_DIR = getXDGBaseDir("XDG_DATA_HOME", ".local/share");

	/** The cache directory. */
	private static final File CACHE_DIR = getXDGBaseDir("XDG_CACHE_HOME", ".cache");

	/** File for logging errors. */
	public static final File LOG_FILE = new File(CONFIG_DIR, ".opsu.log");

	/** File for storing user options. */
	private static final File OPTIONS_FILE = new File(CONFIG_DIR, ".opsu.cfg");

	/** The default beatmap directory (unless an osu! installation is detected). */
	private static final File BEATMAP_DIR = new File(DATA_DIR, "Songs/");

	/** The default skin directory (unless an osu! installation is detected). */
	private static final File SKIN_ROOT_DIR = new File(DATA_DIR, "Skins/");

	/** Cached beatmap database name. */
	public static final File BEATMAP_DB = new File(DATA_DIR, ".opsu.db");

	/** Score database name. */
	public static final File SCORE_DB = new File(DATA_DIR, ".opsu_scores.db");

	/** Directory where natives are unpacked. */
	public static final File NATIVE_DIR = new File(CACHE_DIR, "Natives/");

	/** Directory where temporary files are stored (deleted on exit). */
	public static final File TEMP_DIR = new File(CACHE_DIR, "Temp/");

	/** Main font file name. */
	public static final String FONT_MAIN = "Exo2-Regular.ttf";

	/** Bold font file name. */
	public static final String FONT_BOLD = "Exo2-Bold.ttf";

	/** CJK font file name. */
	public static final String FONT_CJK = "DroidSansFallback.ttf";

	/** Version file name. */
	public static final String VERSION_FILE = "version";

	/** The user agent to use in HTTP requests. */
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/129.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36 Edg/126.0.0.0";

	/** The beatmap directory. */
	private static File beatmapDir;

	/** The import directory. */
	private static File importDir;

	/** The screenshot directory (created when needed). */
	private static File screenshotDir;

	/** The replay directory (created when needed). */
	private static File replayDir;

	/** The root skin directory. */
	private static File skinRootDir;

	/** The custom FFmpeg location (or null for the default). */
	private static File FFmpegPath;

	/** The theme song string: {@code filename,title,artist,length(ms)} */
	private static String themeString = "theme.mp3,Rainbows,Kevin MacLeod,219350";

	/** The theme song timing point string. */
	private static String themeTimingPoint = "1120,545.454545454545,4,1,0,100,0,0";

	/**
	 * Returns whether the XDG flag in the manifest (if any) is set to "true".
	 *
	 * @return true if XDG directories are enabled, false otherwise
	 */
	private static boolean checkXDGFlag() {
		JarFile jarFile = Utils.getJarFile();
		if (jarFile == null)
			return false;
		try {
			Manifest manifest = jarFile.getManifest();
			if (manifest == null)
				return false;
			Attributes attributes = manifest.getMainAttributes();
			String value = attributes.getValue("Use-XDG");
			return (value != null && value.equalsIgnoreCase("true"));
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns the directory based on the XDG base directory specification for
	 * Unix-like operating systems, only if the "XDG" flag is enabled.
	 *
	 * @param env      the environment variable to check (XDG_*_*)
	 * @param fallback the fallback directory relative to ~home
	 * @return the XDG base directory, or the working directory if unavailable
	 */
	private static File getXDGBaseDir(String env, String fallback) {
		File workingDir = Utils.isJarRunning() ? Utils.getRunningDirectory().getParentFile()
				: Utils.getWorkingDirectory();

		if (!USE_XDG)
			return workingDir;

		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0) {
			String rootPath = System.getenv(env);
			if (rootPath == null) {
				String home = System.getProperty("user.home");
				if (home == null)
					return new File("./");
				rootPath = String.format("%s/%s", home, fallback);
			}
			File dir = new File(rootPath, "opsu");
			if (!dir.isDirectory() && !dir.mkdir())
				ErrorHandler.error(String.format("Failed to create configuration folder at '%s/opsu'.", rootPath), null,
						false);
			return dir;
		} else
			return workingDir;
	}

	/**
	 * Returns the osu! installation directory.
	 * This function only works on Windows, based on registry.
	 *
	 * @return the directory, or null if not found
	 */
	private static File getOsuInstallationDirectory() {
		if (!System.getProperty("os.name").startsWith("Win"))
			return null;

		// registry location
		final WinReg.HKEY rootKey = WinReg.HKEY_CLASSES_ROOT;
		final String regKey = "osu\\DefaultIcon";
		final String regValue = null; // default value
		final String regPathPattern = "\"(.+)\\\\[^/]+\\.exe\"";

		String value;
		try {
			value = Advapi32Util.registryGetStringValue(rootKey, regKey, regValue);
		} catch (Win32Exception e) {
			return null; // key/value not found
		}
		Pattern pattern = Pattern.compile(regPathPattern);
		Matcher m = pattern.matcher(value);
		if (!m.find())
			return null;
		File dir = new File(m.group(1));
		return (dir.isDirectory()) ? dir : null;
	}

	/** Game options. */
	public enum GameOption {
		// internal options (not displayed in-game)
		BEATMAP_DIRECTORY("BeatmapDirectory") {
			@Override
			public String write() {
				return getBeatmapDir().getAbsolutePath();
			}

			@Override
			public void read(String s) {
				beatmapDir = new File(s);
			}
		},
		IMPORT_DIRECTORY("ImportDirectory") {
			@Override
			public String write() {
				return getImportDir().getAbsolutePath();
			}

			@Override
			public void read(String s) {
				importDir = new File(s);
			}
		},
		SCREENSHOT_DIRECTORY("ScreenshotDirectory") {
			@Override
			public String write() {
				return getScreenshotDir().getAbsolutePath();
			}

			@Override
			public void read(String s) {
				screenshotDir = new File(s);
			}
		},
		REPLAY_DIRECTORY("ReplayDirectory") {
			@Override
			public String write() {
				return getReplayDir().getAbsolutePath();
			}

			@Override
			public void read(String s) {
				replayDir = new File(s);
			}
		},
		SKIN_DIRECTORY("SkinDirectory") {
			@Override
			public String write() {
				return getSkinRootDir().getAbsolutePath();
			}

			@Override
			public void read(String s) {
				skinRootDir = new File(s);
			}
		},
		FFMPEG_PATH("FFmpegPath") {
			@Override
			public String write() {
				return (FFmpegPath == null) ? "" : FFmpegPath.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				if (!s.isEmpty())
					FFmpegPath = new File(s);
			}
		},
		THEME_SONG("ThemeSong") {
			@Override
			public String write() {
				return themeString;
			}

			@Override
			public void read(String s) {
				String oldThemeString = themeString;
				themeString = s;
				Beatmap beatmap = getThemeBeatmap();
				if (beatmap == null) {
					Log.warn(String.format("The theme song \"%s\" data is invaild.", s));
				} else if (!beatmap.audioFilename.isFile()
						&& !ResourceLoader.resourceExists(beatmap.audioFilename.getName())) {
					Log.warn(String.format("Cannot find theme song [%s].", beatmap.audioFilename.getAbsolutePath()));
				}
			}
		},
		THEME_SONG_TIMINGPOINT("ThemeSongTiming") {
			@Override
			public String write() {
				return themeTimingPoint;
			}

			@Override
			public void read(String s) {
				try {
					new TimingPoint(s);
					themeTimingPoint = s;
				} catch (Exception e) {
					Log.warn(String.format("The theme song timing point \"%s\" is invaild.", s));
				}
			}
		},

		// in-game options
		SCREEN_RESOLUTION(t("Resolution"), "ScreenResolution", "") {
			private Resolution[] itemList = null;

			@Override
			public boolean isRestartRequired() {
				return true;
			}

			@Override
			public String getValueString() {
				return resolution.toString();
			}

			@Override
			public Object[] getItemList() {
				if (itemList == null) {
					int width = Display.getDesktopDisplayMode().getWidth();
					int height = Display.getDesktopDisplayMode().getHeight();
					List<Resolution> list = new ArrayList<>();
					for (Resolution res : Resolution.values()) {
						// only show resolutions that fit on the screen
						if (res == Resolution.RES_800_600 || (width >= res.getWidth() && height >= res.getHeight()))
							list.add(res);
					}
					itemList = list.toArray(new Resolution[0]);
				}
				return itemList;
			}

			@Override
			public void selectItem(int index, GameContainer container) {
				resolution = itemList[index];

				// check if fullscreen mode is possible with this resolution
				if (FULLSCREEN.getBooleanValue() && !resolution.hasFullscreenDisplayMode())
					FULLSCREEN.toggle(container);
			}

			@Override
			public void read(String s) {
				try {
					resolution = Resolution.valueOf(String.format("RES_%s", s.replace('x', '_')));
				} catch (IllegalArgumentException e) {
					Log.warn("Invalid resolution setting.", e);
				}
			}
		},
		FULLSCREEN(t("Fullscreen mode"), "Fullscreen", t("Switches to dedicated fullscreen mode."), false) {
			@Override
			public boolean isRestartRequired() {
				return true;
			}

			@Override
			public void toggle(GameContainer container) {
				// check if fullscreen mode is possible with this resolution
				if (!getBooleanValue() && !resolution.hasFullscreenDisplayMode()) {
					UI.getNotificationManager().sendBarNotification(
							String.format(t("Fullscreen mode is not available at resolution %s"), resolution.toString()));
					return;
				}

				super.toggle(container);
			}
		},
		SKIN("Skin", "Skin", "") {
			private String[] itemList = null;

			// <TODO> Can we directly load skins without restart here?

			/** Creates the list of available skins. */
			private void createSkinList() {
				File[] dirs = SkinLoader.getSkinDirectories(getSkinRootDir());
				itemList = new String[dirs.length + 1];
				itemList[0] = Skin.DEFAULT_SKIN_NAME;
				for (int i = 0; i < dirs.length; i++) {
					Skin r = SkinLoader.loadSkin(dirs[i]);
					itemList[i + 1] = r.getName();
				}
			}

			@Override
			public String getValueString() {
				return skinName;
			}

			@Override
			public Object[] getItemList() {
				if (itemList == null)
					createSkinList();
				return itemList;
			}

			@Override
			public void selectItem(int index, GameContainer container) {
				if (itemList == null) {
					createSkinList();
				}
				String OriginalName = Options.getSkin().getName();
				skinName = itemList[index];
				if (!skinName.equals(OriginalName)) {
					write();
					Utils.ChangeNewSkin();
					// TODO: If skin is changed then reload the skin and UI.
				}
			}

			@Override
			public void read(String s) {
				skinName = s;
			}
		},
		TARGET_FPS(t("Frame limiter"), "FrameSync", t("Higher values may cause high CPU usage.")) {
			private String[] itemList = null;
			private final String FPS_UN = t("Unlimited");

			@Override
			public String getValueString() {
				int fps = getTargetFPS();
				return (fps == -1) ? FPS_UN : String.format((fps == 60) ? "%dfps (vsync)" : "%dfps", fps);
			}

			@Override
			public Object[] getItemList() {
				if (itemList == null) {
					itemList = new String[targetFPS.length];
					for (int i = 0; i < targetFPS.length; i++) {
						int fps = targetFPS[i];
						itemList[i] = (fps == -1) ? FPS_UN
								: String.format((fps == 60) ? "%dfps (vsync)" : "%dfps", fps);
					}
				}
				return itemList;
			}

			@Override
			public void selectItem(int index, GameContainer container) {
				targetFPSindex = index;

				int fps = getTargetFPS();
				boolean vsync = (fps == 60);
				container.setTargetFrameRate(fps);
				if (container.isVSyncRequested() != vsync) {
					container.setVSync(vsync);
				}
			}

			@Override
			public String write() {
				return Integer.toString(targetFPS[targetFPSindex]);
			}

			@Override
			public void read(String s) {
				int i = Integer.parseInt(s);
				for (int j = 0; j < targetFPS.length; j++) {
					if (i == targetFPS[j]) {
						targetFPSindex = j;
						break;
					}
				}
			}
		},
		LANGUAGE(t("Language"), "UILang", t("The interface language.")) {
			private String[] itemList = null;
			// TODO: Display human-friendly items
			@Override
			public boolean isRestartRequired() {
				return true;
			}

			@Override
			public Object[] getItemList() {
				File[] langList = I18N.getBaseDir().listFiles();
					itemList = new String[langList.length];
				// The first item is always English
				itemList[0] = "English";
				// Add entries based on contents in res/i10n
				for (int i = 1; i < itemList.length; i++) {
					// We've recently added a messages.pot!
					int postfixLoc = langList[i].getName().indexOf('.');
					itemList[i] = langList[i].getName().substring(9, postfixLoc);
				}
				return itemList;
			}

			@Override
			public String getValueString() {
				return lang;
			}

			@Override
			public void selectItem(int index, GameContainer container) {
				lang = itemList[index];
			}

			@Override
			public String write() {
				return lang;
			}
		},
		SHOW_FPS(t("Show FPS counter"), "FpsCounter",
				t("Show a subtle FPS counter in the bottom right corner of the screen."), true) {
			@Override
			public void toggle(GameContainer container) {
				super.toggle(container);
				UI.resetFPSDisplay();
			}
		},
		SHOW_UNICODE(t("Prefer metadata in original language"), "ShowUnicode",
				t("Where available, song titles will be shown in their native language (and character-set)."), false) {
			@Override
			public void toggle(GameContainer container) {
				super.toggle(container);
				if (bool) {
					try {
						Fonts.XLARGE.loadGlyphs();
						Fonts.LARGE.loadGlyphs();
						Fonts.MEDIUM.loadGlyphs();
						Fonts.DEFAULT.loadGlyphs();
					} catch (SlickException e) {
						Log.warn("Failed to load glyphs.", e);
					}
				}
			}
		},
		SCREENSHOT_FORMAT(t("Screenshot format"), "ScreenshotFormat", t("Press F12 to take a screenshot.")) {
			private String[] itemList = null;

			@Override
			public String getValueString() {
				return screenshotFormat[screenshotFormatIndex].toUpperCase();
			}

			@Override
			public Object[] getItemList() {
				if (itemList == null) {
					itemList = new String[screenshotFormat.length];
					for (int i = 0; i < screenshotFormat.length; i++)
						itemList[i] = screenshotFormat[i].toUpperCase();
				}
				return itemList;
			}

			@Override
			public void selectItem(int index, GameContainer container) {
				screenshotFormatIndex = index;
			}

			@Override
			public String write() {
				return Integer.toString(screenshotFormatIndex);
			}

			@Override
			public void read(String s) {
				int i = Integer.parseInt(s);
				if (i >= 0 && i < screenshotFormat.length)
					screenshotFormatIndex = i;
			}
		},
		CURSOR_SIZE(t("Cursor size"), "CursorSize", t("Change the cursor scale."), 100, 50, 200) {
			@Override
			public String getValueString() {
				return String.format("%.2fx", val / 100f);
			}

			@Override
			public String write() {
				return String.format(Locale.US, "%.2f", val / 100f);
			}

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 100f);
				if (i >= getMinValue() && i <= getMaxValue())
					val = i;
			}
		},
		WARNINGARROW_TINT_WHITE(t("Tint warning arrow white"), "TintWarnArrowWhite",
				t("Tint the warning arrow appeared before the end of a break period white instead of red.\nThis overrides the skin settings."),
				true),
		BUILTIN_BACK_BUTTON(t("Always use builtin back button"), "BuiltinBackButton",
				t("Use the builtin back button style regardless of the skin."), false),
		DYNAMIC_BACKGROUND(t("Dynamic backgrounds"), "DynamicBackground",
				t("The current beatmap background will be used as the main menu background."), true),
		LOAD_VERBOSE(t("Detailed loading progress"), "LoadVerbose",
				t("Display more verbose loading progress in the splash screen."), false),
		MASTER_VOLUME(t("Master"), "VolumeUniversal", t("Global volume level."), 35, 0, 100) {
			@Override
			public void setValue(int value) {
				super.setValue(value);
				SoundStore.get().setMusicVolume(getMasterVolume() * getMusicVolume());
			}
		},
		MUSIC_VOLUME(t("Music"), "VolumeMusic", t("Music volume."), 80, 0, 100) {
			@Override
			public void setValue(int value) {
				super.setValue(value);
				SoundStore.get().setMusicVolume(getMasterVolume() * getMusicVolume());
			}
		},
		EFFECT_VOLUME(t("Effects"), "VolumeEffect", t("Menu and game sound effects volume."), 70, 0, 100),
		HITSOUND_VOLUME(t("Hit sounds"), "VolumeHitSound", t("Hit sounds volume."), 30, 0, 100),
		MUSIC_OFFSET(t("Universal offset"), "Offset", t("Adjust this value if hit objects are out of sync."), -75, -500,
				500) {
			@Override
			public String getValueString() {
				return String.format("%dms", val);
			}
		},
		DISABLE_GAMEPLAY_SOUNDS(t("Disable sound effects in gameplay"), "DisableGameplaySound",
				t("Mute all sound effects during gameplay only."), false),
		DISABLE_SOUNDS(t("Disable all sound effects"), "DisableSound",
				t("May resolve Linux sound driver issues.\nRequires a restart."), false) {
			@Override
			public boolean isRestartRequired() {
				return true;
			}
		},
		HEARTBEAT(t("Heartbeat sound (unstable)"), "Heartbeat",
				t("Play heartbeat sounds when the cursor hovers on the logo. May sounds off beat."), false),
		KEY_LEFT(t("Left game key"), "keyOsuLeft", t("Select this option to input a key.")) {
			@Override
			public String getValueString() {
				return Keyboard.getKeyName(getGameKeyLeft());
			}

			@Override
			public String write() {
				return Keyboard.getKeyName(getGameKeyLeft());
			}

			@Override
			public void read(String s) {
				setGameKeyLeft(Keyboard.getKeyIndex(s));
			}
		},
		KEY_RIGHT(t("Right game key"), "keyOsuRight", t("Select this option to input a key.")) {
			@Override
			public String getValueString() {
				return Keyboard.getKeyName(getGameKeyRight());
			}

			@Override
			public String write() {
				return Keyboard.getKeyName(getGameKeyRight());
			}

			@Override
			public void read(String s) {
				setGameKeyRight(Keyboard.getKeyIndex(s));
			}
		},
		DISABLE_MOUSE_WHEEL(t("Disable mouse wheel in play mode"), "MouseDisableWheel",
				t("Disable the functionality to  adjust the volume and pause the game with the mouse wheel during gameplay."),
				false),
		DISABLE_MOUSE_BUTTONS(t("Disable mouse buttons in play mode"), "MouseDisableButtons",
				t("Disable all mouse buttons.\nSpecifically for people who use their keyboard to click."), false),
		BACKGROUND_DIM(t("Background dim"), "DimLevel", t("Percentage to dim the background image during gameplay."), 50, 0,
				100),
		FORCE_DEFAULT_PLAYFIELD(t("Force default playfield"), "ForceDefaultPlayfield",
				t("Overrides the song background with the default playfield background."), false),
		ENABLE_VIDEOS(t("Background video"), "Video",
				t("Enables background video playback.\nIf you get a large amount of lag on beatmaps with video, try disabling this feature."),
				true),
		ENABLE_STORYBOARDS (t("Storyboards (Experimental)"), "Storyboard", t("Enables storyboard playback.\nThis is still an experimental feature."), true),
		IGNORE_BEATMAP_SKINS(t("Ignore all beatmap skins"), "IgnoreBeatmapSkins",
				t("Defaults game settings to never use skin element overrides provided by beatmaps."), false),
		FORCE_SKIN_CURSOR(t("Always use skin cursor"), "UseSkinCursor",
				t("The selected skin's cursor will override any beatmap-specific cursor modifications."), false),
		KEEP_AUTO(t("Keep the Auto mod on"), "KeepAuto", t("Keep the Auto mod on after gameplay."), false),
		PAUSE_IN_REPLAY(t("Enable pause screen in Auto play and replays"), "PauseAutoReplay",
				t("Allow players to press Esc to show the pause screen during replays and gameplay with the Auto mod on."),
				false),
		SNAKING_SLIDERS(t("Snaking sliders"), "SnakingSliders", t("Sliders gradually snake out from their starting point."),
				true),
		EXPERIMENTAL_SLIDERS(t("Use experimental sliders"), "ExperimentalSliders",
				t("Render sliders using the experimental slider style."), false),
		EXPERIMENTAL_SLIDERS_CAPS(t("Draw slider caps"), "ExperimentalSliderCaps",
				t("Draw caps (end circles) on sliders.\nOnly applies to experimental sliders."), false),
		EXPERIMENTAL_SLIDERS_SHRINK(t("Shrinking sliders"), "ExperimentalSliderShrink",
				t("Sliders shrink toward their ending point when the ball passes.\nOnly applies to experimental sliders."),
				true),
		EXPERIMENTAL_SLIDERS_MERGE(t("Merging sliders"), "ExperimentalSliderMerge",
				t("For overlapping sliders, don't draw the edges and combine the slider tracks where they cross.\nOnly applies to experimental sliders."),
				true),
		SHOW_HIT_LIGHTING(t("Hit lighting"), "HitLighting",
				t("Adds a subtle glow behind hit explosions which lights the playfield."), true),
		SHOW_COMBO_BURSTS(t("Combo bursts"), "ComboBurst",
				t("A character image bursts from the side of the screen at combo milestones."), true),
		SHOW_PERFECT_HIT(t("Perfect hits"), "PerfectHit", t("Shows perfect hit result bursts (300s, slider ticks)."), true),
		SHOW_FOLLOW_POINTS(t("Follow points"), "FollowPoints", t("Shows follow points between hit objects."), true),
		SHOW_HIT_ERROR_BAR(t("Hit error bar"), "ScoreMeter", t("Shows precisely how accurate you were with each hit."),
				false),
		ALWAYS_SHOW_KEY_OVERLAY(t("Always show key overlay"), "KeyOverlay",
				t("Show the key overlay when playing instead of only on replays."), false),
		LOAD_HD_IMAGES(t("Load HD images"), "LoadHDImages",
				String.format(t("Loads HD (%s) images when available.\nIncreases memory usage and loading times."),
						GameImage.HD_SUFFIX),
				true),
		CUSTOM_DIFFICULTY(t("Use custom difficulty"), "CustomDiff", t("Use your own difficulty settings for beatmaps!"), false),
		FIXED_CS(t("Fixed CS"), "FixedCS", t("Determines the size of circles and sliders."), 0, 0, 100) {
			@Override
			public String getValueString() {
				return (val == 0) ? DISABLED : String.format("%.1f", val / 10f);
			}

			@Override
			public String write() {
				return String.format(Locale.US, "%.1f", val / 10f);
			}

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= getMinValue() && i <= getMaxValue())
					val = i;
			}
		},
		FIXED_HP(t("Fixed HP"), "FixedHP", t("Determines the rate at which health decreases."), 0, 0, 100) {
			@Override
			public String getValueString() {
				return (val == 0) ? DISABLED : String.format("%.1f", val / 10f);
			}

			@Override
			public String write() {
				return String.format(Locale.US, "%.1f", val / 10f);
			}

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= getMinValue() && i <= getMaxValue())
					val = i;
			}
		},
		FIXED_AR(t("Fixed AR"), "FixedAR", t("Determines how long hit circles stay on the screen."), 0, 0, 100) {
			@Override
			public String getValueString() {
				return (val == 0) ? DISABLED : String.format("%.1f", val / 10f);
			}

			@Override
			public String write() {
				return String.format(Locale.US, "%.1f", val / 10f);
			}

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= getMinValue() && i <= getMaxValue())
					val = i;
			}
		},
		FIXED_OD(t("Fixed OD"), "FixedOD", t("Determines the time window for hit results."), 0, 0, 100) {
			@Override
			public String getValueString() {
				return (val == 0) ? DISABLED : String.format("%.1f", val / 10f);
			}

			@Override
			public String write() {
				return String.format(Locale.US, "%.1f", val / 10f);
			}

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= getMinValue() && i <= getMaxValue())
					val = i;
			}
		},
		FIXED_SPEED(t("Fixed speed"), "FixedSpeed", t("Determines the speed of the music."), 0, 0, 300) {
			@Override
			public String getValueString() {
				return (val == 0) ? DISABLED : String.format("%.2fx", val / 100f);
			}

			@Override
			public String write() {
				return String.format(Locale.US, "%.2f", val / 100f);
			}

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 100f);
				if (i >= getMinValue() && i <= getMaxValue())
					val = i;
			}
		},
		ALLOW_INSANE(t("Allow abnormal difficulty settings"), "AllowInsaneSetting", t("Allow difficulty settings to go beyond the normal limits.\nEnabling this might break the game."), false),
		UICOLOR_CUSTOM(t("Customize interface color"), "CustomUIColor", t("Use your own color for the interface of opsu!"),
				false),
		UICOLOR_R(t("Interface color Red"), "CustomColorR", t("Red color value."), 235, 0, 255) {
			@Override
			public String getValueString() {
				return String.format("%d", val);
			}
		},
		UICOLOR_G(t("Interface color Green"), "CustomColorG", t("Green color value."), 117, 0, 255) {
			@Override
			public String getValueString() {
				return String.format("%d", val);
			}
		},
		UICOLOR_B(t("Interface color Blue"), "CustomColorB", t("Blue color value."), 139, 0, 255) {
			@Override
			public String getValueString() {
				return String.format("%d", val);
			}
		},
		REAL_AUTO(t("Real Auto player"), "RealAuto", t("Auto also fails!"), true),
		CHECKPOINT(t("Track checkpoint"), "Checkpoint",
				t("Press Ctrl+L while playing to load a checkpoint, and Ctrl+S to set one."), 0, 0, 1800) {
			@Override
			public String getValueString() {
				return (val == 0) ? DISABLED
						: String.format("%02d:%02d",
								TimeUnit.SECONDS.toMinutes(val),
								val - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(val)));
			}
		},
		PARALLAX(t("Parallax"), "MenuParallax", t("Add a parallax effect based on the current cursor position."), true),
		ENABLE_THEME_SONG(t("Theme song"), "MenuMusic",
				OpsuConstants.PROJECT_NAME
						+ t(" will play themed music throughout the game, instead of using random beatmaps."),
				true),
		REPLAY_SEEKING(t("Replay seeking"), "ReplaySeeking",
				t("Enable a seeking bar on the left side of the screen during replays."), false),
		DISABLE_UPDATER(t("Disable automatic updates"), "DisableUpdater",
				t("Disable checking for updates when the game starts."), false),
		ENABLE_WATCH_SERVICE(t("Watch service"), "WatchService",
				t("Watch the beatmap directory for changes. Requires a restart."), false) {
			@Override
			public boolean isRestartRequired() {
				return true;
			}
		};

		/** Option name. */
		private final String name;

		/** Option name, as displayed in the configuration file. */
		private final String displayName;

		/** Option description. */
		private final String description;

		/** The boolean value for the option (if applicable). */
		protected boolean bool;

		/** The integer value for the option (if applicable). */
		protected int val;

		/** The upper and lower bounds on the integer value (if applicable). */
		private int max, min;

		/** Option types. */
		public enum OptionType {
			BOOLEAN, NUMERIC, SELECT
		}

		/** Option type. */
		private OptionType type = OptionType.SELECT;

		/**
		 * Whether this group should be visible (used for filtering in the options
		 * menu).
		 */
		private boolean visible = true;

		/**
		 * Constructor for internal options (not displayed in-game).
		 *
		 * @param displayName the option name, as displayed in the configuration file
		 */
		GameOption(String displayName) {
			this(null, displayName, null);
		}

		/**
		 * Constructor for other option types.
		 *
		 * @param name        the option name
		 * @param displayName the option name, as displayed in the configuration file
		 * @param description the option description
		 */
		GameOption(String name, String displayName, String description) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
		}

		/**
		 * Constructor for boolean options.
		 *
		 * @param name        the option name
		 * @param displayName the option name, as displayed in the configuration file
		 * @param description the option description
		 * @param value       the default boolean value
		 */
		GameOption(String name, String displayName, String description, boolean value) {
			this(name, displayName, description);
			this.bool = value;
			this.type = OptionType.BOOLEAN;
		}

		/**
		 * Constructor for numeric options.
		 *
		 * @param name        the option name
		 * @param displayName the option name, as displayed in the configuration file
		 * @param description the option description
		 * @param value       the default integer value
		 */
		GameOption(String name, String displayName, String description, int value, int min, int max) {
			this(name, displayName, description);
			this.val = value;
			this.min = min;
			this.max = max;
			this.type = OptionType.NUMERIC;
		}

		/**
		 * Returns the option name.
		 *
		 * @return the name string
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the option name, as displayed in the configuration file.
		 *
		 * @return the display name string
		 */
		public String getDisplayName() {
			return displayName;
		}

		/**
		 * Returns the option description.
		 *
		 * @return the description string
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Returns the option type.
		 *
		 * @return the type
		 */
		public OptionType getType() {
			return type;
		}

		/**
		 * Returns whether a restart is required for the option to take effect.
		 *
		 * @return true if a restart is required, false otherwise
		 */
		public boolean isRestartRequired() {
			return false;
		}

		/**
		 * Returns the boolean value for the option, if applicable.
		 *
		 * @return the boolean value
		 */
		public boolean getBooleanValue() {
			return bool;
		}

		/**
		 * Returns the integer value for the option, if applicable.
		 *
		 * @return the integer value
		 */
		public int getIntegerValue() {
			return val;
		}

		/**
		 * Returns the minimum integer value for this option, if applicable.
		 *
		 * @return the minimum integer value
		 */
		public int getMinValue() {
			return min;
		}

		/**
		 * Returns the maximum integer value for this option, if applicable.
		 *
		 * @return the maximum integer value
		 */
		public int getMaxValue() {
			return max;
		}

		/**
		 * Returns an array of values to choose from, if applicable.
		 *
		 * @return the array of values, or {@code null} if not applicable
		 */
		public Object[] getItemList() {
			return null;
		}

		/**
		 * Sets the boolean value for the option.
		 *
		 * @param value the new boolean value
		 */
		public void setValue(boolean value) {
			this.bool = value;
		}

		/**
		 * Sets the integer value for the option.
		 *
		 * @param value the new integer value
		 */
		public void setValue(int value) {
			this.val = Utils.clamp(value, min, max);
		}

		/**
		 * Toggles the boolean value for the option, if applicable.
		 *
		 * @param container the game container
		 */
		public void toggle(GameContainer container) {
			bool = !bool;
		}

		/**
		 * Selects an item with the given list index, if applicable.
		 *
		 * @param index     the selected item index (in {@link #getItemList()})
		 * @param container the game container
		 */
		public void selectItem(int index, GameContainer container) {
		}

		/**
		 * Returns the value of the option as a string (via override).
		 * <p>
		 * By default, this returns "{@code val}%" for numeric options,
		 * "Yes" or "No" based on the {@code bool} field for boolean options,
		 * and an empty string otherwise.
		 *
		 * @return the value string
		 */
		public String getValueString() {
			if (type == OptionType.NUMERIC)
				return String.format("%d%%", val);
			else if (type == OptionType.BOOLEAN)
				return (bool) ? "Yes" : "No";
			else
				return "";
		}

		/**
		 * Returns the string to write to the configuration file (via override).
		 * <p>
		 * By default, this returns "{@code val}" for numeric options,
		 * "true" or "false" based on the {@code bool} field for boolean options,
		 * and {@link #getValueString()} otherwise.
		 *
		 * @return the string to write
		 */
		public String write() {
			if (type == OptionType.NUMERIC)
				return Integer.toString(val);
			else if (type == OptionType.BOOLEAN)
				return Boolean.toString(bool);
			else
				return getValueString();
		}

		/**
		 * Reads the value of the option from the configuration file (via override).
		 * <p>
		 * By default, this sets {@code val} for numeric options only if the
		 * value is between the min and max bounds, sets {@code bool} for
		 * boolean options, and does nothing otherwise.
		 *
		 * @param s the value string read from the configuration file
		 */
		public void read(String s) {
			if (type == OptionType.NUMERIC) {
				int i = Integer.parseInt(s);
				if (i >= min && i <= max)
					val = i;
			} else if (type == OptionType.BOOLEAN)
				bool = Boolean.parseBoolean(s);
		}

		/**
		 * Checks whether the option matches a given search query.
		 *
		 * @param query the search term
		 * @return true if the option name or description matches the query
		 */
		public boolean matches(String query) {
			return !query.isEmpty() &&
					(name.toLowerCase().contains(query) || description.toLowerCase().contains(query));
		}

		/**
		 * Sets whether this option should be visible.
		 *
		 * @param visible {@code true} if visible
		 */
		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		/**
		 * Returns whether this option should be visible.
		 *
		 * @return true if visible
		 */
		public boolean isVisible() {
			return visible;
		}
	}

	/** Map of option display names to GameOptions. */
	private static HashMap<String, GameOption> optionMap;

	/** Screen resolutions. */
	private enum Resolution {
		RES_800_600(800, 600),
		RES_1024_600(1024, 600),
		RES_1024_768(1024, 768),
		RES_1280_720(1280, 720),
		RES_1280_800(1280, 800),
		RES_1280_960(1280, 960),
		RES_1280_1024(1280, 1024),
		RES_1366_768(1366, 768),
		RES_1440_900(1440, 900),
		RES_1600_900(1600, 900),
		RES_1600_1200(1600, 1200),
		RES_1680_1050(1680, 1050),
		RES_1920_1080(1920, 1080),
		RES_1920_1200(1920, 1200),
		RES_2560_1440(2560, 1440),
		RES_2560_1600(2560, 1600),
		RES_3840_2160(3840, 2160);

		/** Screen dimensions. */
		private final int width;
		private final int height;

		/**
		 * Constructor.
		 *
		 * @param width  the screen width
		 * @param height the screen height
		 */
		Resolution(int width, int height) {
			this.width = width;
			this.height = height;
		}

		/** Returns the screen width. */
		public int getWidth() {
			return width;
		}

		/** Returns the screen height. */
		public int getHeight() {
			return height;
		}

		/** Returns whether this resolution is possible to use in fullscreen mode. */
		public boolean hasFullscreenDisplayMode() {
			try {
				for (DisplayMode mode : Display.getAvailableDisplayModes()) {
					if (width == mode.getWidth() && height == mode.getHeight())
						return true;
				}
			} catch (LWJGLException e) {
				ErrorHandler.error("Failed to get available display modes.", e, true);
			}
			return false;
		}

		@Override
		public String toString() {
			return String.format("%sx%s", width, height);
		}
	}

	/** Current screen resolution. */
	private static Resolution resolution = Resolution.RES_1024_768;

	/** The name of the skin. */
	private static String skinName = "Default";

	/** The current skin. */
	private static Skin skin;

	// private static File skinDir = skin.getDirectory();

	private static String lang = "English";

	/** Frame limiters. */
	private static final int[] targetFPS = { 60, 120, 240, -1 /* Unlimited */ };

	/** Index in targetFPS[] array. */
	private static int targetFPSindex = 0;

	/** Screenshot file formats. */
	private static final String[] screenshotFormat = { "png", "jpg", "bmp" };

	/** Index in screenshotFormat[] array. */
	private static int screenshotFormatIndex = 0;

	/** Left and right game keys. */
	private static int keyLeft = Keyboard.KEY_NONE,
			keyRight = Keyboard.KEY_NONE;

	// This class should not be instantiated.
	private Options() {
	}

	/**
	 * Returns the target frame rate.
	 *
	 * @return the target FPS
	 */
	public static int getTargetFPS() {
		return targetFPS[targetFPSindex];
	}

	/**
	 * Sets the target frame rate to the next available option, and sends a
	 * bar notification about the action.
	 *
	 * @param container the game container
	 */
	public static void setNextFPS(GameContainer container) {
		int index = (targetFPSindex + 1) % targetFPS.length;
		if (index == targetFPS.length - 1)
			index = 0; // Skip "Unlimited" option
		GameOption.TARGET_FPS.selectItem(index, container);
		UI.getNotificationManager()
				.sendBarNotification(String.format(t("Frame limiter: %s"), GameOption.TARGET_FPS.getValueString()));
	}

	/**
	 * Returns the master volume level.
	 *
	 * @return the volume [0, 1]
	 */
	public static float getMasterVolume() {
		return GameOption.MASTER_VOLUME.getIntegerValue() / 100f;
	}

	/**
	 * Sets the master volume level (if within valid range).
	 *
	 * @param container the game container
	 * @param volume    the volume [0, 1]
	 */
	public static void setMasterVolume(GameContainer container, float volume) {
		if (volume >= 0f && volume <= 1f) {
			GameOption.MASTER_VOLUME.setValue((int) (volume * 100f));
			MusicController.setVolume(getMasterVolume() * getMusicVolume());
		}
	}

	/**
	 * Returns the default music volume.
	 *
	 * @return the volume [0, 1]
	 */
	public static float getMusicVolume() {
		return GameOption.MUSIC_VOLUME.getIntegerValue() / 100f;
	}

	/**
	 * Returns the default sound effect volume.
	 *
	 * @return the sound volume [0, 1]
	 */
	public static float getEffectVolume() {
		return GameOption.EFFECT_VOLUME.getIntegerValue() / 100f;
	}

	/**
	 * Returns the default hit sound volume.
	 *
	 * @return the hit sound volume [0, 1]
	 */
	public static float getHitSoundVolume() {
		return GameOption.HITSOUND_VOLUME.getIntegerValue() / 100f;
	}

	/**
	 * Returns the music offset time.
	 *
	 * @return the offset (in milliseconds)
	 */
	public static int getMusicOffset() {
		return GameOption.MUSIC_OFFSET.getIntegerValue();
	}

	/**
	 * Returns the screenshot file format.
	 *
	 * @return the file extension ("png", "jpg", "bmp")
	 */
	public static String getScreenshotFormat() {
		return screenshotFormat[screenshotFormatIndex];
	}

	/**
	 * Sets the container size and makes the window borderless if the container
	 * size is identical to the screen resolution.
	 * <p>
	 * If the configured resolution is larger than the screen size, the smallest
	 * available resolution will be used.
	 *
	 * @param app the game container
	 */
	public static void setDisplayMode(Container app) {
		int screenWidth = app.getScreenWidth();
		int screenHeight = app.getScreenHeight();
		boolean fullscreen = isFullscreen();

		// check for larger-than-screen dimensions
		if (screenWidth < resolution.getWidth() || screenHeight < resolution.getHeight())
			resolution = Resolution.RES_800_600;

		// check if fullscreen mode is possible with this resolution
		if (fullscreen && !resolution.hasFullscreenDisplayMode())
			fullscreen = false;

		try {
			app.setDisplayMode(resolution.getWidth(), resolution.getHeight(), fullscreen);
		} catch (SlickException e) {
			ErrorHandler.error("Failed to set display mode.", e, true);
		}

		// set borderless window if dimensions match screen size
		if (!fullscreen) {
			boolean borderless = (screenWidth == resolution.getWidth() && screenHeight == resolution.getHeight());
			System.setProperty("org.lwjgl.opengl.Window.undecorated", Boolean.toString(borderless));
		}
	}

	/**
	 * Returns whether fullscreen mode is enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isFullscreen() {
		return GameOption.FULLSCREEN.getBooleanValue();
	}

	/**
	 * Returns whether the FPS counter display is enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isFPSCounterEnabled() {
		return GameOption.SHOW_FPS.getBooleanValue();
	}

	/**
	 * Toggles the FPS counter display.
	 */
	public static void toggleFPSCounter() {
		GameOption.SHOW_FPS.toggle(null);
	}

	/**
	 * Returns whether hit lighting effects are enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isHitLightingEnabled() {
		return GameOption.SHOW_HIT_LIGHTING.getBooleanValue();
	}

	/**
	 * Returns whether combo burst effects are enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isComboBurstEnabled() {
		return GameOption.SHOW_COMBO_BURSTS.getBooleanValue();
	}

	/**
	 * Returns whether warningarrow is tinted white
	 *
	 * @return true if enabled
	 */
	public static boolean isWarningArrowTintWhite() {
		return GameOption.WARNINGARROW_TINT_WHITE.getBooleanValue();
	}

	/**
	 * Returns whether to always use the builtin Back button style.
	 *
	 * @return true if always
	 */
	public static boolean isBuiltinBackButton() {
		return GameOption.BUILTIN_BACK_BUTTON.getBooleanValue();
	}

	/**
	 * Returns the cursor scale.
	 *
	 * @return the scale [0.5, 2]
	 */
	public static float getCursorScale() {
		return GameOption.CURSOR_SIZE.getIntegerValue() / 100f;
	}

	/**
	 * Returns whether the main menu background should be the current beatmap
	 * background image.
	 *
	 * @return true if enabled
	 */
	public static boolean isDynamicBackgroundEnabled() {
		return GameOption.DYNAMIC_BACKGROUND.getBooleanValue();
	}

	/**
	 * Returns whether to show perfect hit result bursts.
	 *
	 * @return true if enabled
	 */
	public static boolean isPerfectHitBurstEnabled() {
		return GameOption.SHOW_PERFECT_HIT.getBooleanValue();
	}

	/**
	 * Returns whether to show follow points.
	 *
	 * @return true if enabled
	 */
	public static boolean isFollowPointEnabled() {
		return GameOption.SHOW_FOLLOW_POINTS.getBooleanValue();
	}

	/**
	 * Returns the background dim level.
	 *
	 * @return the alpha level [0, 1]
	 */
	public static float getBackgroundDim() {
		return (100 - GameOption.BACKGROUND_DIM.getIntegerValue()) / 100f;
	}

	/**
	 * Returns whether to override the beatmap background with the default playfield
	 * background.
	 *
	 * @return true if forced
	 */
	public static boolean isDefaultPlayfieldForced() {
		return GameOption.FORCE_DEFAULT_PLAYFIELD.getBooleanValue();
	}

	/**
	 * Returns whether to keep the Auto mod on after gameplay.
	 *
	 * @return true if always on
	 */
	public static boolean isAutoModAlwaysOn() {
		return GameOption.KEEP_AUTO.getBooleanValue();
	}

	/**
	 * Returns whether to show the pause screen with Auto mod on or during replay.
	 *
	 * @return true if always on
	 */
	public static boolean isPauseAlwaysEnabled() {
		return GameOption.PAUSE_IN_REPLAY.getBooleanValue();
	}

	/**
	 * Returns whether beatmap videos are enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isBeatmapVideoEnabled() {
		return GameOption.ENABLE_VIDEOS.getBooleanValue();
	}

	/**
	 * Returns whether beatmap storyboards are enabled.
	 * @return true if enabled
	 */
	public static boolean isBeatmapStoryboardEnabled() { return GameOption.ENABLE_STORYBOARDS.getBooleanValue(); }

	/**
	 * Returns whether beatmap skins are ignored.
	 *
	 * @return true if ignored
	 */
	public static boolean isBeatmapSkinIgnored() {
		return GameOption.IGNORE_BEATMAP_SKINS.getBooleanValue();
	}

	/**
	 * Returns whether to override the beatmap cursor with the current skin's
	 * cursor.
	 *
	 * @return true if forced
	 */
	public static boolean isSkinCursorForced() {
		return GameOption.FORCE_SKIN_CURSOR.getBooleanValue();
	}

	/**
	 * Returns whether sliders should snake in or just appear fully at once.
	 *
	 * @return true if sliders should snake in
	 */
	public static boolean isSliderSnaking() {
		return GameOption.SNAKING_SLIDERS.getBooleanValue();
	}

	/**
	 * Returns whether to use the experimental slider style.
	 *
	 * @return true if enabled
	 */
	public static boolean isExperimentalSliderStyle() {
		return GameOption.EXPERIMENTAL_SLIDERS.getBooleanValue();
	}

	/**
	 * Returns whether slider caps (end circles) should be drawn.
	 * Only applies to experimental sliders.
	 *
	 * @return true if slider caps should be drawn
	 */
	public static boolean isExperimentalSliderCapsDrawn() {
		return GameOption.EXPERIMENTAL_SLIDERS_CAPS.getBooleanValue();
	}

	/**
	 * Returns whether sliders should shrink toward their ending point.
	 * Only applies to experimental sliders.
	 *
	 * @return true if sliders should shrink
	 */
	public static boolean isExperimentalSliderShrinking() {
		return GameOption.EXPERIMENTAL_SLIDERS_SHRINK.getBooleanValue();
	}

	/**
	 * Returns whether to merge overlapping sliders together when drawing.
	 * Only applies to experimental sliders.
	 *
	 * @return true if sliders should be merged
	 */
	public static boolean isExperimentalSliderMerging() {
		return GameOption.EXPERIMENTAL_SLIDERS_MERGE.getBooleanValue();
	}

	/**
	 * Returns whether to use user-defined difficulty settings.
	 *
	 * @return true if using custom difficulty settings
	 */
	public static boolean isDifficultyCustom() { return GameOption.CUSTOM_DIFFICULTY.getBooleanValue(); }

	/**
	 * Returns the fixed circle size override, if any.
	 *
	 * @return the CS value (0, 10], 0f if disabled
	 */
	public static float getFixedCS() {
		return GameOption.FIXED_CS.getIntegerValue() / 10f;
	}

	/**
	 * Returns the fixed HP drain rate override, if any.
	 *
	 * @return the HP value (0, 10], 0f if disabled
	 */
	public static float getFixedHP() {
		return GameOption.FIXED_HP.getIntegerValue() / 10f;
	}

	/**
	 * Returns the fixed approach rate override, if any.
	 *
	 * @return the AR value (0, 10], 0f if disabled
	 */
	public static float getFixedAR() {
		return GameOption.FIXED_AR.getIntegerValue() / 10f;
	}

	/**
	 * Returns the fixed overall difficulty override, if any.
	 *
	 * @return the OD value (0, 10], 0f if disabled
	 */
	public static float getFixedOD() {
		return GameOption.FIXED_OD.getIntegerValue() / 10f;
	}

	/**
	 * Returns the fixed speed override, if any.
	 *
	 * @return the speed value (0, 3], 0f if disabled
	 */
	public static float getFixedSpeed() {
		return GameOption.FIXED_SPEED.getIntegerValue() / 100f;
	}

	/**
	 * Returns whether to render loading text in the splash screen.
	 *
	 * @return true if enabled
	 */
	public static boolean isLoadVerbose() {
		return GameOption.LOAD_VERBOSE.getBooleanValue();
	}

	/**
	 * Returns the track checkpoint time.
	 *
	 * @return the checkpoint time (in ms)
	 */
	public static int getCheckpoint() {
		return GameOption.CHECKPOINT.getIntegerValue() * 1000;
	}

	/**
	 * Returns whether sound effects are disabled during gameplay.
	 *
	 * @return true if disabled
	 */
	public static boolean isGameplaySoundDisabled() {
		return GameOption.DISABLE_GAMEPLAY_SOUNDS.getBooleanValue();
	}

	/**
	 * Returns whether all sound effects are disabled.
	 *
	 * @return true if disabled
	 */
	public static boolean isSoundDisabled() {
		return GameOption.DISABLE_SOUNDS.getBooleanValue();
	}

	/**
	 * Returns whether heartbeat sounds will be played.
	 *
	 * @return true if enabled
	 */
	public static boolean isHeartbeatEnabled() {
		return GameOption.HEARTBEAT.getBooleanValue();
	}

	/**
	 * Returns whether Auto is set to be real.
	 *
	 * @return real Auto player
	 */
	public static boolean isAutoReal() {
		return GameOption.REAL_AUTO.getBooleanValue();
	}

	/**
	 * Returns whether to use non-English metadata where available.
	 *
	 * @return true if Unicode preferred
	 */
	public static boolean useUnicodeMetadata() {
		return GameOption.SHOW_UNICODE.getBooleanValue();
	}

	/**
	 * Returns whether parallax is enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isParallaxEnabled() {
		return GameOption.PARALLAX.getBooleanValue();
	}

	/**
	 * Returns whether to play the theme song.
	 *
	 * @return true if enabled
	 */
	public static boolean isThemeSongEnabled() {
		return GameOption.ENABLE_THEME_SONG.getBooleanValue();
	}

	/**
	 * Returns whether replay seeking is enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isReplaySeekingEnabled() {
		return GameOption.REPLAY_SEEKING.getBooleanValue();
	}

	/**
	 * Returns whether automatic checking for updates is disabled.
	 *
	 * @return true if disabled
	 */
	public static boolean isUpdaterDisabled() {
		return GameOption.DISABLE_UPDATER.getBooleanValue();
	}

	/**
	 * Returns whether the beatmap watch service is enabled.
	 *
	 * @return true if enabled
	 */
	public static boolean isWatchServiceEnabled() {
		return GameOption.ENABLE_WATCH_SERVICE.getBooleanValue();
	}

	/**
	 * Sets the track checkpoint time, if within bounds.
	 *
	 * @param time the track position (in ms)
	 * @return true if within bounds
	 */
	public static boolean setCheckpoint(int time) {
		if (time >= 0 && time < 3600) {
			GameOption.CHECKPOINT.setValue(time);
			return true;
		}
		return false;
	}

	/**
	 * Returns whether to show the hit error bar.
	 *
	 * @return true if enabled
	 */
	public static boolean isHitErrorBarEnabled() {
		return GameOption.SHOW_HIT_ERROR_BAR.getBooleanValue();
	}

	/**
	 * Returns whether to show the key overlay on non-replay game sessions.
	 *
	 * @return true if enabled
	 */
	public static boolean alwaysShowKeyOverlay() {
		return GameOption.ALWAYS_SHOW_KEY_OVERLAY.getBooleanValue();
	}

	/**
	 * Returns whether to load HD (@2x) images.
	 *
	 * @return true if HD images are enabled, false if only SD images should be
	 *         loaded
	 */
	public static boolean loadHDImages() {
		return GameOption.LOAD_HD_IMAGES.getBooleanValue();
	}

	/**
	 * Returns whether the mouse wheel is disabled during gameplay.
	 *
	 * @return true if disabled
	 */
	public static boolean isMouseWheelDisabled() {
		return GameOption.DISABLE_MOUSE_WHEEL.getBooleanValue();
	}

	/**
	 * Returns whether the mouse buttons are disabled during gameplay.
	 *
	 * @return true if disabled
	 */
	public static boolean isMouseDisabled() {
		return GameOption.DISABLE_MOUSE_BUTTONS.getBooleanValue();
	}

	/**
	 * Toggles the mouse button enabled/disabled state during gameplay and
	 * sends a bar notification about the action.
	 */
	public static void toggleMouseDisabled() {
		GameOption.DISABLE_MOUSE_BUTTONS.toggle(null);
		UI.getNotificationManager().sendBarNotification(
				(GameOption.DISABLE_MOUSE_BUTTONS.getBooleanValue()) ? t("Mouse buttons are disabled.")
						: t("Mouse buttons are enabled."));
	}

	/**
	 * Returns whether to use custom colors for UI.
	 *
	 * @return whether to use custom colors
	 */
	public static boolean isColorCustom() {
		return GameOption.UICOLOR_CUSTOM.getBooleanValue();
	}

	/**
	 * Returns whether to allow difficulty settings beyond the normal limits.
	 *
	 * @return whether to allow
	 */
	public static boolean isInsaneSettingAllowed() { return GameOption.ALLOW_INSANE.getBooleanValue(); }

	/**
	 * Returns the accent color.
	 *
	 * @return Color, combined by RGB
	 */
	public static Color getAccentColor() {
		if (Options.isColorCustom()) {
			return new Color(GameOption.UICOLOR_R.getIntegerValue(), GameOption.UICOLOR_G.getIntegerValue(),
					GameOption.UICOLOR_B.getIntegerValue());
		} else {
			return new Color(Colors.PINK_OPTION);
		}
	}

	/**
	 * Returns the left game key.
	 *
	 * @return the left key code
	 */
	public static int getGameKeyLeft() {
		if (keyLeft == Keyboard.KEY_NONE)
			setGameKeyLeft(Input.KEY_Z);
		return keyLeft;
	}

	/**
	 * Returns the right game key.
	 *
	 * @return the right key code
	 */
	public static int getGameKeyRight() {
		if (keyRight == Keyboard.KEY_NONE)
			setGameKeyRight(Input.KEY_X);
		return keyRight;
	}

	/**
	 * Sets the left game key.
	 * This will not be set to the same key as the right game key, nor to any
	 * reserved keys (see {@link #isValidGameKey(int)}).
	 *
	 * @param key the keyboard key
	 * @return {@code true} if the key was set, {@code false} if it was rejected
	 */
	public static boolean setGameKeyLeft(int key) {
		if ((key == keyRight && key != Keyboard.KEY_NONE) || !isValidGameKey(key))
			return false;
		keyLeft = key;
		return true;
	}

	/**
	 * Sets the right game key.
	 * This will not be set to the same key as the left game key, nor to any
	 * reserved keys (see {@link #isValidGameKey(int)}).
	 *
	 * @param key the keyboard key
	 * @return {@code true} if the key was set, {@code false} if it was rejected
	 */
	public static boolean setGameKeyRight(int key) {
		if ((key == keyLeft && key != Keyboard.KEY_NONE) || !isValidGameKey(key))
			return false;
		keyRight = key;
		return true;
	}

	/**
	 * Checks if the given key is a valid game key.
	 *
	 * @param key the keyboard key
	 * @return {@code true} if valid, {@code false} otherwise
	 */
	private static boolean isValidGameKey(int key) {
		return (key != Keyboard.KEY_ESCAPE && key != Keyboard.KEY_SPACE &&
				key != Keyboard.KEY_UP && key != Keyboard.KEY_DOWN &&
				key != Keyboard.KEY_F7 && key != Keyboard.KEY_F10 && key != Keyboard.KEY_F12);
	}

	/**
	 * Returns the beatmap directory.
	 * If invalid, this will attempt to search for the directory,
	 * and if nothing found, will create one.
	 *
	 * @return the beatmap directory
	 */
	public static File getBeatmapDir() {
		if (beatmapDir != null && beatmapDir.isDirectory())
			return beatmapDir;

		// use osu! installation directory, if found
		File osuDir = getOsuInstallationDirectory();
		if (osuDir != null) {
			beatmapDir = new File(osuDir, BEATMAP_DIR.getName());
			if (beatmapDir.isDirectory())
				return beatmapDir;
		}

		// use default directory
		beatmapDir = BEATMAP_DIR;
		if (!beatmapDir.isDirectory() && !beatmapDir.mkdir())
			ErrorHandler.error(
					String.format("Failed to create beatmap directory at '%s'.", beatmapDir.getAbsolutePath()), null,
					false);
		return beatmapDir;
	}

	/**
	 * Returns the import directory (for beatmaps, skins, and replays).
	 * If invalid, this will create and return an "Import" directory.
	 *
	 * @return the import directory
	 */
	public static File getImportDir() {
		if (importDir != null && importDir.isDirectory())
			return importDir;

		importDir = new File(DATA_DIR, "Import/");
		if (!importDir.isDirectory() && !importDir.mkdir())
			ErrorHandler.error(String.format("Failed to create import directory at '%s'.", importDir.getAbsolutePath()),
					null, false);
		return importDir;
	}

	/**
	 * Returns the screenshot directory.
	 * If invalid, this will return a "Screenshot" directory.
	 *
	 * @return the screenshot directory
	 */
	public static File getScreenshotDir() {
		if (screenshotDir != null && screenshotDir.isDirectory())
			return screenshotDir;

		screenshotDir = new File(DATA_DIR, "Screenshots/");
		return screenshotDir;
	}

	/**
	 * Returns the replay directory.
	 * If invalid, this will return a "Replay" directory.
	 *
	 * @return the replay directory
	 */
	public static File getReplayDir() {
		if (replayDir != null && replayDir.isDirectory())
			return replayDir;

		replayDir = new File(DATA_DIR, "Replays/");
		return replayDir;
	}

	/**
	 * Returns the current skin directory.
	 * If invalid, this will create a "Skins" folder in the root directory.
	 *
	 * @return the skin directory
	 */
	public static File getSkinRootDir() {
		if (skinRootDir != null && skinRootDir.isDirectory())
			return skinRootDir;

		// use osu! installation directory, if found
		File osuDir = getOsuInstallationDirectory();
		if (osuDir != null) {
			skinRootDir = new File(osuDir, SKIN_ROOT_DIR.getName());
			if (skinRootDir.isDirectory())
				return skinRootDir;
		}

		// use default directory
		skinRootDir = SKIN_ROOT_DIR;
		if (!skinRootDir.isDirectory() && !skinRootDir.mkdir())
			ErrorHandler.error(
					String.format("Failed to create skins directory at '%s'.", skinRootDir.getAbsolutePath()), null,
					false);
		return skinRootDir;
	}

	/**
	 * Loads the skin given by the current skin directory.
	 * If the directory is invalid, the default skin will be loaded.
	 */
	public static void loadSkin() {
		File skinDir = getSkinDir();
		if (skinDir == null) // invalid skin name
			skinName = Skin.DEFAULT_SKIN_NAME;

		// set skin and modify resource locations
		ResourceLoader.removeAllResourceLocations();
		if (skinDir == null)
			skin = new Skin(null);
		else {
			// load the skin
			skin = SkinLoader.loadSkin(skinDir);
			// String info = skin.getName() + " (" + skin.getAuthor() + ")";
			// ErrorHandler.error(String.format("You are using the skin '%s'.", info), null,
			// false);
			// Show the real name and author in skin.ini
			ResourceLoader.addResourceLocation(new FileSystemLocation(skinDir));
		}
		ResourceLoader.addResourceLocation(new ClasspathLocation());
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File(".")));
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));
	}

	/**
	 * Returns the current skin.
	 *
	 * @return the skin, or null if no skin is loaded (see {@link #loadSkin()})
	 */
	public static Skin getSkin() {
		return skin;
	}

	/**
	 * Returns the current skin directory.
	 * <p>
	 * NOTE: This directory will differ from that of the currently loaded skin
	 * if {@link #loadSkin()} has not been called after a directory change.
	 * Use {@link Skin#getDirectory()} to get the directory of the currently
	 * loaded skin.
	 *
	 * @return the skin directory, or null for the default skin
	 */
	public static File getSkinDir() {
		File root = getSkinRootDir();
		File dir = new File(root, skinName);
		// File dir = skinDir;
		// File dir = new File(root, skinDir);
		// TODO: Need to adjust
		return (dir.isDirectory()) ? dir : null;
	}

	/**
	 * Returns the custom FFmpeg shared library location.
	 *
	 * @return the file, or {@code null} if the default location should be used
	 */
	public static File getFFmpegLocation() {
		return FFmpegPath;
	}

	public static File getThemeSong() {
		File audio = new File(BEATMAP_DIR, "theme.mp3");
		if (!audio.isFile())
			return null;
		return audio;
	}

	/**
	 * Returns a dummy Beatmap containing the theme song.
	 *
	 * @return the theme song beatmap, or {@code null} if the theme string is
	 *         malformed
	 */
	public static Beatmap getThemeBeatmap() {
		String[] tokens = themeString.split(",");
		if (tokens.length != 4)
			return null;

		Beatmap beatmap = new Beatmap(null);
		File cusau = getThemeSong();
		if (cusau != null)
			beatmap.audioFilename = cusau;
		else
			beatmap.audioFilename = new File(tokens[0]);
		beatmap.title = tokens[1];
		beatmap.artist = tokens[2];
		try {
			beatmap.endTime = Integer.parseInt(tokens[3]);
		} catch (NumberFormatException e) {
			return null;
		}
		try {
			beatmap.timingPoints = new ArrayList<>(1);
			beatmap.timingPoints.add(new TimingPoint(themeTimingPoint));
		} catch (Exception e) {
			return null;
		}

		return beatmap;
	}

	public static String getLanguage() {
		return GameOption.LANGUAGE.toString();
	}

	public static File getOptionsFile() {
		return OPTIONS_FILE;
	}

	/**
	 * Reads user options from the options file, if it exists.
	 */
	public static void parseOptions() {
		// if no config file, use default settings
		if (!OPTIONS_FILE.isFile()) {
			saveOptions();
			return;
		}

		// create option map
		if (optionMap == null) {
			optionMap = new HashMap<>();
			for (GameOption option : GameOption.values())
				optionMap.put(option.getDisplayName(), option);
		}

		// read file
		try (BufferedReader in = new BufferedReader(new FileReader(OPTIONS_FILE))) {
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
				GameOption option = optionMap.get(name);
				if (option != null) {
					try {
						String value = line.substring(index + 1).trim();
						option.read(value);
					} catch (NumberFormatException e) {
						Log.warn(String.format("Format error in options file for line: '%s'.", line), e);
					}
				}
			}
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to read file '%s'.", OPTIONS_FILE.getAbsolutePath()), e, false);
		}
	}

	/**
	 * (Over)writes user options to a file.
	 */
	public static void saveOptions() {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(OPTIONS_FILE), StandardCharsets.UTF_8))) {
			// header
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
			String date = dateFormat.format(new Date());
			writer.write(String.format(t("# %s configuration"), OpsuConstants.PROJECT_NAME));
			writer.newLine();
			writer.write(t("# last updated on "));
			writer.write(date);
			writer.newLine();
			writer.newLine();

			// options
			for (GameOption option : GameOption.values()) {
				writer.write(option.getDisplayName());
				writer.write(" = ");
				writer.write(option.write());
				writer.newLine();
			}
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to write to file '%s'.", OPTIONS_FILE.getAbsolutePath()), e,
					false);
		}
	}
}
