package io.luxus.adofai;

import java.util.HashSet;
import java.util.Set;

public class Melody {

	private final long us;
	private final long tick;
	
	private final Set<Integer> keys;
	
	public Melody(long us, long tick) {
		this.us = us;
		this.tick = tick;
		this.keys = new HashSet<>();
	}
	
	public Melody(long us, long tick, Melody melody) {
		this(us, tick);
		this.keys.addAll(melody.keys);
	}
	
	public long getUs() {
		return us;
	}
	
	public long getTick() {
		return tick;
	}
	
	public Set<Integer> getKeys() {
		return keys;
	}
	
}
