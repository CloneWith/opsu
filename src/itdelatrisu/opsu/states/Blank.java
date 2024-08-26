package itdelatrisu.opsu.states;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

public class Blank extends BasicGameState {
	// game-related variables
	private final int state;
	private GameContainer container;
	private boolean init = false;

	public Blank(int state) {
		this.state = state;
	}

	@Override
	public int getID() {
		return state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
		throws SlickException {
		this.container = container;
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
		throws SlickException {
		g.setBackground(Color.black);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
		throws SlickException {
		if (!init) {
			init = true;
		}
	}
}
