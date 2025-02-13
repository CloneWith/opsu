package clonewith.opsu.storyboard;

public enum TriggerEvent {
	Passing, Failing, HitSoundFinish, HitSoundWhistle, HitSoundClap, None;

	public static TriggerEvent toTriggerEvent(String s) {
		return switch (s) {
			case "Passing" -> Passing;
			case "Failing" -> Failing;
			case "HitSoundFinish" -> HitSoundFinish;
			case "HitSoundWhistle" -> HitSoundWhistle;
			case "HitSoundClap" -> HitSoundClap;
			default -> None;
		};
	}
}
