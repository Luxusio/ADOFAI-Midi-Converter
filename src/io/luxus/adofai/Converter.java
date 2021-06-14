package io.luxus.adofai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import io.luxus.api.adofai.MapData;
import io.luxus.api.adofai.TileData;
import io.luxus.api.adofai.action.SetSpeed;
import io.luxus.api.adofai.action.Twirl;
import io.luxus.api.adofai.type.EventType;
import io.luxus.api.adofai.type.TileAngle;

public class Converter {

	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;

	public static final int SET_TEMPO = 0x51;
	
	public static final double Infinity = 1.0 / 0.0;
	
	public static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
	private final double[] toneHz = { 32.7032, 34.6478, 36.7081, 38.8909, 41.2304, 43.6535, 46.2493, 48.9994, 51.913, 55.0, 58.2705, 61.7354 };
	private final Map<Integer, Double> toneDelay = new HashMap<>();
	private int toneMaxOctave = 10;
	private int toneMinOctave = -10;
	
	private int floor = 0;
	private final Map<Integer, Double> nextKeyTime = new HashMap<>();
	
	public Converter() {
		for (int i=0;i<toneHz.length;i++) {
			toneDelay.put(i, 1000000 / toneHz[i]);
		}
		for (int i=1;i<toneMaxOctave;i++) {
			int offset = i * toneHz.length;
			for (int j=0;j<toneHz.length;j++) {
				toneDelay.put(offset + j, toneDelay.get(offset + j - toneHz.length) / 2);
			}
		}
		for (int i = -1; i > toneMinOctave;i--) {
			int offset = i * toneHz.length;
			for (int j=0;j<toneHz.length;j++) {
				toneDelay.put(offset + j, toneDelay.get(offset + j + toneHz.length) * 2);
			}
		}
		
	}
	
	public List<Melody> midiToMelodyList(Sequence sequence, boolean[] disable) {

		List<Melody> melodyList = new ArrayList<>();
		Melody currMelody = new Melody(0, 0);

		Track[] tracks = sequence.getTracks();
		int[] eventIndices = new int[tracks.length];
		
		int resolution = sequence.getResolution();
		double tempo = 500000.0;
		double tickMultiply = tempo / resolution;
		long currTimeUs = 0;
		long lastTick = 0;
		
		while (true) {

			long minTick = 2147483647;
			int minIdx = -1;
			MidiEvent event = null;

			for (int i = 0; i < tracks.length; i++) {
				if (eventIndices[i] < tracks[i].size()) {
					MidiEvent temp = tracks[i].get(eventIndices[i]);
					if (temp.getTick() < minTick) {
						minIdx = i;
						minTick = temp.getTick();
						event = temp;
					}
				}
			}

			if (event == null)
				break;
			
			eventIndices[minIdx]++;

			MidiMessage message = event.getMessage();
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage) message;
				
				if (disable[minIdx]) continue;
				
				if (sm.getCommand() == NOTE_ON || sm.getCommand() == NOTE_OFF) {
					int key = sm.getData1();
					int velocity = sm.getData2();
					
					if (event.getTick() != currMelody.getTick()) {
						melodyList.add(currMelody);
						currTimeUs += (event.getTick() - lastTick) * tickMultiply;
						lastTick = event.getTick();
						currMelody = new Melody(currTimeUs, event.getTick(), currMelody);
					}

					if (velocity > 0 && sm.getCommand() == NOTE_ON) {
						// on
						currMelody.getKeys().add(key);
					} else {
						// off
						currMelody.getKeys().remove(key);
					}
				}
			} else if (message instanceof MetaMessage) {
				MetaMessage mm = (MetaMessage) message;
				
				if (mm.getType() == SET_TEMPO) {
					currTimeUs += (event.getTick() - lastTick) * tickMultiply;
					lastTick = event.getTick();
					
					tempo = ((mm.getData()[0] < 0 ? -mm.getData()[0] : mm.getData()[0]) << 16) |
								((mm.getData()[1] < 0 ? -mm.getData()[1] : mm.getData()[1]) << 8) | 
								((mm.getData()[2] < 0 ? -mm.getData()[2] : mm.getData()[2]));
					tickMultiply = tempo / resolution;
					System.out.println(event.getTick() + " tempo " + tickMultiply + "us / tick");
				}
			}

		}
		
		melodyList.add(currMelody);
		
		return melodyList;
	}
	

	public List<Long> melodyListToUsDelayList(List<Melody> melodyList, int octaveOffset) {

		// us
		long currTime = 0;
		nextKeyTime.clear();
		
		List<Long> usDelayList = new LinkedList<Long>();
		
		for (int i=1;i<melodyList.size();i++) {
			final Melody curr = melodyList.get(i-1);
			final Melody next = melodyList.get(i);
			
			if (curr.getKeys().isEmpty()) {
				// todo : delay
				long diffTime = next.getUs() - currTime;
				if (diffTime == 0) continue;
				
				usDelayList.add(diffTime);
				currTime = next.getUs();
				
			} else {
				long minTime, prevTime = currTime;
				Set<Integer> minTimeKeys = new HashSet<>();
				
				do {
					// get minimum time, time keys
					minTime = 9223372036854775807L;
					minTimeKeys.clear();
					for (Integer key : curr.getKeys()) {
						key += octaveOffset * 12;
						
						long nextTime = getNextTime(currTime, key);
						
						if (nextTime == minTime) minTimeKeys.add(key);
						else if (nextTime < minTime) {
							minTimeKeys.clear();
							minTimeKeys.add(key);
							minTime = nextTime;
						}
					}
					
					if (minTime >= next.getUs()) break;
					
					for (Integer key : minTimeKeys) 
						addNextTime(key);
					
					// calculate bpm
					long diffTime = minTime - prevTime;
					if (diffTime == 0) continue;

					usDelayList.add(diffTime);
					
					prevTime = minTime;
				} while (true);
				currTime = prevTime;
			}
		}
		
		return usDelayList;
	}
	
	private long getNextTime(long timeFrom, int key) {
		Double nextTime = nextKeyTime.get(key);
		if (nextTime == null) {
			nextTime = 0.0;
		}
		
		if (nextTime <= timeFrom) {
			double delayTime = toneDelay.get(key);
			nextTime = delayTime * (long) (timeFrom / delayTime);
			if (nextTime <= timeFrom) nextTime += delayTime;
			if (nextTime <= timeFrom) {
				System.err.println("nextTime is smaller than timeFrom! : " + timeFrom + ", " + key + "(" + nextTime + ")");
			}
		}
		nextKeyTime.put(key, nextTime);
		return (long) (double) nextTime;
	}
	
	private void addNextTime(int key) {
		nextKeyTime.put(key, nextKeyTime.get(key) + toneDelay.get(key));
	}
	
	public MapData usDelayListToMapData(List<Long> usDelayList) {
		MapData mapData = new MapData();
		List<TileData> tileDataList = mapData.getTileDataList();
		floor = 0;
		tileDataList.add(getTileData());
		
		for (Long usDelay : usDelayList) {
			double toBPM = 60.0 * 1000 * 1000 / usDelay / 12.0;
			
			TileData tileData = getTileData();
			tileData.getActionList(EventType.SET_SPEED).add(new SetSpeed("Bpm", toBPM, 1.0));
			tileDataList.add(tileData);
		}
		
		return mapData;
	}
	
	private TileData getTileData() {
		TileData tileData = new TileData(floor,
				floor == 0 ? TileAngle.NONE : floor % 2 == 0 ? TileAngle._165 : TileAngle._0);
		if (floor > 1) 	tileData.getActionList(EventType.TWIRL).add(new Twirl());
		floor++;
		return tileData;
	}
	

}
