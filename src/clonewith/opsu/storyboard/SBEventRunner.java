package clonewith.opsu.storyboard;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Storyboard Event Runner
 *
 * @author fluddokt
 *
 */
class SBEventRunner {
	ArrayList<SBEvent> events = new ArrayList<>();
	int eventsIndex = 0;
	SBEventRunnerListener listener = () -> {};
	public void ready() {
		events.sort(Comparator.comparingInt(SBEvent::getTime));
	}
	public void update(int trackPosition) {
		if (eventsIndex-1 >= 0 && events.get(eventsIndex-1).getTime() > trackPosition)
			reset();

		while (eventsIndex<events.size() && events.get(eventsIndex).getTime() <= trackPosition) {
			events.get(eventsIndex).execute();
			eventsIndex++;
		}
	}
	public void reset() {
		eventsIndex = 0;
		listener.reseted();
	}
	public void add(SBEvent e) {
		events.add(e);
	}
	public void setListener(SBEventRunnerListener listenener) {
		this.listener = listenener;
	}
}
