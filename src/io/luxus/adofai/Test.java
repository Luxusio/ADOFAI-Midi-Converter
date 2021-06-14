 package io.luxus.adofai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import io.luxus.api.adofai.MapData;

public class Test {

	public static void main(String[] args) throws Exception {
		testAllMakeMap();
		//testMakeMap();
		//testConverter();
		//readMidiSequence();
		
	}

	public static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
	
	private static void testAllMakeMap() throws Exception {
		boolean[] disable = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
		String fileName = "iL";
		fileName = "FreedomDive";
		fileName = "megalovania";
		
		Sequence sequence = MidiSystem.getSequence(new File(fileName + ".mid"));
		List<Melody> melodyList = new Converter().midiToMelodyList(sequence, disable);
		List<Long> usDelayList = new Converter().melodyListToUsDelayList(melodyList, -4);
		MapData mapData = new Converter().usDelayListToMapData(usDelayList);
		System.out.println(mapData.getTileDataList().size() + "tiles");
		mapData.save(fileName + ".adofai");
	}

	public static final int C = 0;
	public static final int C_ = 1;
	public static final int D = 2;
	public static final int D_ = 3;
	public static final int E = 4;
	public static final int F = 5;
	public static final int F_ = 6;
	public static final int G = 7;
	public static final int G_ = 8;
	public static final int A = 9;
	public static final int A_ = 10;
	public static final int B = 11;
	
	private static void testMakeMap() throws Exception {
		
		List<Melody> melodyList = new ArrayList<>();
		melodyList.add(getMelody(0, C, E, G));
		melodyList.add(getMelody(1000, D, E, G));
		melodyList.add(getMelody(2000, E, G, B));
		melodyList.add(getMelody(3000, F));
		melodyList.add(getMelody(4000, G));
		melodyList.add(getMelody(5000, A));
		melodyList.add(getMelody(6000, B));
		new Converter().usDelayListToMapData(
			new Converter().melodyListToUsDelayList(melodyList, -5))
		.save("test.adofai");
	}
	
	private static Melody getMelody(long ms, int... keys) {
		Melody melody = new Melody(ms * 1000, 0);
		for (int key : keys) melody.getKeys().add(12 * 5 + key);
		return melody;
	}
	
	private static void testConverter() throws InvalidMidiDataException, IOException {
		boolean[] disable = {false, false, false, true};
		Sequence sequence = MidiSystem.getSequence(new File("iL.mid"));

		List<Melody> melodyList = new Converter().midiToMelodyList(sequence, disable);
		
		for (Melody melody : melodyList) {

			System.out.print("@" + (melody.getUs() / 1000000) + "s " + (melody.getUs() % 1000000) + "us");
			
			for (Integer key : melody.getKeys()) {
				int octave = (key / 12) - 1;
				int note = key % 12;
				String noteName = NOTE_NAMES[note];

				System.out.print(" " + noteName + octave);
			}
			System.out.println();

		}

	}

	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	public static final int CC = 0xB0; // Continuous controller

	public static final int SET_TEMPO = 0x51;
	public static final int TIME_SIGNATURE = 0x58;
	public static final int KEY_SIGNATURE = 0x59;
	
	
	private static void readMidiSequence() throws InvalidMidiDataException, IOException {
		Sequence sequence = MidiSystem.getSequence(new File("iL.mid"));
		
		int resolution = sequence.getResolution();
		System.out.println("resolution: " + resolution);
		int trackNumber = 0;
		for (Track track : sequence.getTracks()) {
			trackNumber++;
			System.out.println("Track " + trackNumber + ": size = " + track.size());
			System.out.println();
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				if (event.getTick() < 0 || event.getTick() > 25000)
					continue;
				System.out.print("@" + event.getTick() + " ");
				MidiMessage message = event.getMessage();
				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					System.out.print("Channel: " + sm.getChannel() + " ");
					if (sm.getCommand() == NOTE_ON) {
						int key = sm.getData1();
						int octave = (key / 12) - 1;
						int note = key % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
						System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
					} else if (sm.getCommand() == NOTE_OFF) {
						int key = sm.getData1();
						int octave = (key / 12) - 1;
						int note = key % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
						System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
					} else {
						System.out.println("Command:" + sm.getCommand() + ", " + sm.getData1() + ", " + sm.getData2());
					}
				} else if (message instanceof MetaMessage) {
					MetaMessage mm = (MetaMessage) message;
					
					if (mm.getType() == SET_TEMPO) {
						int tempo = ((mm.getData()[0] < 0 ? -mm.getData()[0] : mm.getData()[0]) << 16) |
									((mm.getData()[1] < 0 ? -mm.getData()[1] : mm.getData()[1]) << 8) | 
									((mm.getData()[2] < 0 ? -mm.getData()[2] : mm.getData()[2]));

						System.out.println("metaMessage setTempo " + (tempo / 1000.0 / resolution) + "ms / tick" );
						
					}
					else if (mm.getType() == TIME_SIGNATURE) {
						System.out.println("metaMessage TimeSignature");
						
					}
					else if (mm.getType() == KEY_SIGNATURE) {
						System.out.println("metaMessage KeySignature");
						
					}
					else {
						System.out.println("metaMessage " + mm.getType());
						
					}
					
					
				} else {
					System.out.println("Other message: " + message.getClass());
				}
			}

			System.out.println();
		}
	}

}