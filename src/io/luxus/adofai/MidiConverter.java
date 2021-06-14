package io.luxus.adofai;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import io.luxus.api.adofai.MapData;

public class MidiConverter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		try {
			program(scanner);
			System.out.println("계속하시려면 엔터키를 눌러주세요.");
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			scanner.close();
		}
	}

	private static void program(Scanner scanner) throws IOException, IllegalArgumentException, IllegalAccessException, InvalidMidiDataException {
		System.out.println("A Dance of Fire and Ice Midi Converter");
		System.out.println("ver 1.0.0");
		System.out.println("개발자 : Luxus io");
		System.out.println("YouTube : https://www.youtube.com/c/Luxusio");
		System.out.println("Github : https://github.com/Luxusio/ADOFAI-Midi-Converter");
		System.out.println();

		System.out.print("파일경로(.mid포함) : ");
		String path = scanner.nextLine();
		
		File file = new File(path);
		if (!file.exists()) {
			System.err.println("E> File not exists: " + path);
			return;
		}
		
		Sequence sequence = MidiSystem.getSequence(file);
		
		
		boolean[] disable = new boolean[sequence.getTracks().length];
		
		int trackNumber = 0;
		for (Track track : sequence.getTracks()) {
			disable[trackNumber] = false;
			System.out.println("Track " + trackNumber++ + ": size = " + track.size());
		}
		
		int disableIn = 0;
		do {
			System.out.print("비활성화 할 트랙(-1: 나가기): ");
			disableIn = scanner.nextInt();
			if (0 <= disableIn && disableIn < disable.length) {
				disable[disableIn] = true;
			}
			else if (disableIn != -1) {
				System.out.print("0이상 혹은 " + disable.length + "미만의 값을 입력 해 주세요");
			}
		} while (disableIn != -1);
		
		
		System.out.println();
		
		int octaveOffset = 0;
		System.out.print("옥타브 오프셋(보통-4~-2):");
		octaveOffset = scanner.nextInt();
		
		System.out.println();
		System.out.println("start");
		
		Converter converter = new Converter();
		
		List<Melody> melodyList = converter.midiToMelodyList(sequence, disable);
		List<Long> usDelayList = converter.melodyListToUsDelayList(melodyList, octaveOffset);
		MapData mapData = converter.usDelayListToMapData(usDelayList);
		
		String outPath;
		int idx = path.lastIndexOf('.');
		if (idx != -1) {
			outPath = path.substring(0, idx) + ".adofai";
		} else {
			outPath = path + ".adofai";
		}
		mapData.save(outPath);

		System.out.println("outPath:" + outPath);
		
	}
	
}
