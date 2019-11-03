import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MyClass {

	public static void main(String[] args) throws IOException {
		String fileName = "input.txt";
		// *** Etape 1 ***
		// 1- Premier comptage en séquentielle pur
		Map wordMap = getWordHashMap(fileName);
		System.out.println(wordMap);
		// 2- Premier tri en séquentiel pur
		List<List<String>> sortedList = getSortedList(wordMap);
		System.out.println(sortedList);
		// Etape 2				wordList.add(word);

		
	}
	
	public static Map<String, Integer> getWordHashMap(String fileName) throws IOException {
		List<String> content = Files.readAllLines(Paths.get(fileName));
		Map<String, Integer> mapOccurences = new TreeMap<>();
		for(String line : content) {
			String[] words = line.split(" ");
			for(String word : words) {
				if(mapOccurences.containsKey(word)) {
					int tempOccurence = mapOccurences.get(word);
					mapOccurences.put(word, tempOccurence + 1);
				} else {
					mapOccurences.put(word, 1);
				}
			}
		}
		return mapOccurences;
	}

	
	public static List<List<String>> getSortedList(Map<String, Integer> wordMap) {
		List<List<String>> sortedList = new ArrayList<>();
		for(String wordMin : wordMap.keySet()) {
			if(sortedList.contains(wordMin)) {
				continue;
			}
			int nbWordsMin = wordMap.get(wordMin);
			for(String wordJ : wordMap.keySet()) {
				if(wordJ.equalsIgnoreCase(wordMin) || wordMap.containsKey(wordJ)) {
					continue;
				}
				int nbWordsJ = wordMap.get(wordJ);
				if(nbWordsMin>nbWordsJ) {
					wordMin = wordJ;
					nbWordsMin = wordMap.get(wordMin);
					continue;
				}
			}
			List<String> tempList = new ArrayList<>();
			tempList.add(wordMin);
			tempList.add(new Integer(nbWordsMin).toString());
			sortedList.add(tempList);
		}
		return sortedList;
	}
	

	
}
