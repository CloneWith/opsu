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

package itdelatrisu.opsu.replay;

/**
 * Captures a single life frame.
 *
 * @param time   Time, in milliseconds.
 * @param health Health.
 *
 * @author smoogipooo (<a href="https://github.com/smoogipooo/osu-Replay-API/">...</a>)
 */
public record LifeFrame(int time, float health) {
	/** The sample interval, in milliseconds, when saving replays. */
	public static final int SAMPLE_INTERVAL = 2000;

	/**
	 * Constructor.
	 *
	 * @param time   the time (in ms)
	 * @param health the health [0,1]
	 */
	public LifeFrame {
	}

	/**
	 * Returns the frame time, in milliseconds.
	 */
	@Override
	public int time() {
		return time;
	}

	/**
	 * Returns the health.
	 */
	@Override
	public float health() {
		return health;
	}

	@Override
	public String toString() {
		return String.format("(%d, %.2f)", time, health);
	}
}
