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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.options.Options.GameOption;
import org.newdawn.slick.Image;

import static clonewith.opsu.I18N.t;

/**
 * Option category and related options.
 */
public class OptionGroup {
	/** All option groups. */
	public static final OptionGroup[] ALL_OPTIONS = new OptionGroup[] {
		new OptionGroup(t("General"), GameImage.MENU_NAV_GENERAL),
		new OptionGroup(t("LANGUAGE"), new GameOption[] {
			GameOption.LANGUAGE,
			GameOption.SHOW_UNICODE,
		}),
		new OptionGroup(t("UPDATES"), new GameOption[] {
			GameOption.DISABLE_UPDATER,
		}),
		new OptionGroup(t("Graphics"), GameImage.MENU_NAV_GRAPHICS),
		new OptionGroup(t("LAYOUT"), new GameOption[] {
			GameOption.SCREEN_RESOLUTION,
			GameOption.FULLSCREEN,
		}),
		new OptionGroup(t("RENDERER"), new GameOption[] {
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
		}),
		new OptionGroup(t("DETAIL SETTINGS"), new GameOption[] {
			GameOption.SNAKING_SLIDERS,
			GameOption.ENABLE_VIDEOS,
			GameOption.ENABLE_STORYBOARDS,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SCREENSHOT_FORMAT,
		}),
		new OptionGroup(t("EXPERIMENTAL SLIDERS"), new GameOption[] {
			GameOption.EXPERIMENTAL_SLIDERS,
			GameOption.EXPERIMENTAL_SLIDERS_MERGE,
			GameOption.EXPERIMENTAL_SLIDERS_SHRINK,
			GameOption.EXPERIMENTAL_SLIDERS_CAPS,
		}),
		new OptionGroup(t("MAIN MENU"), new GameOption[] {
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.PARALLAX,
			GameOption.ENABLE_THEME_SONG,
		}),
		new OptionGroup(t("SONG SELECT"), new GameOption[] {
			GameOption.DEFAULT_BACKGROUND_FOR_SONG_SELECT,
		}),
		new OptionGroup(t("Gameplay"), GameImage.MENU_NAV_GAMEPLAY),
		new OptionGroup(t("GENERAL"), new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.SHOW_HIT_ERROR_BAR,
			GameOption.ALWAYS_SHOW_KEY_OVERLAY,
		}),
		new OptionGroup(t("MODS"), new GameOption[] {
			GameOption.KEEP_AUTO,
			GameOption.PAUSE_IN_REPLAY,
			GameOption.REAL_AUTO,
		}),
		new OptionGroup(t("Audio"), GameImage.MENU_NAV_AUDIO),
		new OptionGroup(t("VOLUME"), new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.DISABLE_GAMEPLAY_SOUNDS,
			GameOption.DISABLE_SOUNDS,
			GameOption.HEARTBEAT,
		}),
		new OptionGroup(t("OFFSET ADJUSTMENT"), new GameOption[] {
			GameOption.MUSIC_OFFSET,
		}),
		new OptionGroup(t("Skin"), GameImage.MENU_NAV_SKIN),
		new OptionGroup(t("SKIN"), new GameOption[]{
			GameOption.SKIN,
			GameOption.LOAD_HD_IMAGES,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.FORCE_SKIN_CURSOR,
			GameOption.CURSOR_SIZE,
			GameOption.WARNINGARROW_TINT_WHITE,
			GameOption.BUILTIN_BACK_BUTTON,
		}),
		new OptionGroup(t("Input"), GameImage.MENU_NAV_INPUT),
		new OptionGroup(t("MOUSE"), new GameOption[] {
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
		}),
		new OptionGroup(t("KEYBOARD"), new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
		}),
		new OptionGroup(t("Custom"), GameImage.MENU_NAV_CUSTOM),
		new OptionGroup(t("DIFFICULTY"), new GameOption[] {
			GameOption.CUSTOM_DIFFICULTY,
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.FIXED_SPEED,
			GameOption.ALLOW_INSANE,
		}),
		new OptionGroup(t("COLOR"), new GameOption[] {
			GameOption.UICOLOR_CUSTOM,
			GameOption.UICOLOR_R,
			GameOption.UICOLOR_G,
			GameOption.UICOLOR_B,
		}),
		new OptionGroup(t("SEEKING"), new GameOption[] {
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
		}),
		new OptionGroup(t("MISCELLANEOUS"), new GameOption[] {
			GameOption.ENABLE_WATCH_SERVICE,
			GameOption.LOAD_VERBOSE,
		}),
	};

	/** The category name. */
	private final String category;

	/** The icon, if this is a section header. */
	private final GameImage icon;

	/** The game options. */
	private final GameOption[] options;

	/** Whether this group should be visible (used for filtering in the options menu). */
	private boolean visible = true;

	/**
	 * Creates an option group with the given category name and options.
	 * @param category the category name
	 * @param options the related options
	 */
	public OptionGroup(String category, GameOption[] options) {
		this.category = category;
		this.options = options;
		this.icon = null;
	}

	/**
	 * Creates an option group with the given category name and icon, effectively being a section.
	 * @param category the category name
	 * @param icon the icon to be used for this section
	 */
	public OptionGroup(String category, GameImage icon) {
		this.category = category;
		this.options = null;
		this.icon = icon;
	}


	/** Returns the category name. */
	public String getName() { return category; }

	/** Returns the related options. */
	public GameOption[] getOptions() { return options; }

	/** Returns the related icon. */
	public Image getIcon() { return icon.getImage(); }

	/** Returns the option at the given index. */
	public GameOption getOption(int i) { return options[i]; }

	/** Sets whether this group should be visible. */
	public void setVisible(boolean visible) { this.visible = visible; }

	/** Returns whether this group should be visible. */
	public boolean isVisible() { return visible; }
}
