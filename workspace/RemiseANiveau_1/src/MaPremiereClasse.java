import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaPremiereClasse {
	
	public static Map<String, List<Double>> _studentToGradeMap = new HashMap<>();
	public static Map<String, Double> _studentToAverageMap = new HashMap<>();
	
	public static double getMaxGrade(Map<String, Double> studentToGradeMap) {
		double bestGrade = 0;
		for(Map.Entry me : studentToGradeMap.entrySet()) {
			double grade = (double) me.getValue();
			if(bestGrade < grade) {
				bestGrade = grade;
			}
		}
		return bestGrade;
	}
	
	public static void buildGradesMap(String fileName) throws IOException {
		List<String> content = Files.readAllLines(Paths.get(fileName));
		for(String line : content) {
			String[] words = line.split(" ");
			String student = words[0];
			double grade = 0;
			try {
				grade = Double.parseDouble(words[1]);
				continue;
			} catch (NumberFormatException e) {
				System.out.println("Wrong grade number format for student: " + student);
			}
			if(_studentToGradeMap.containsKey(student)) {
				List currentGrades = _studentToGradeMap.get(student);
				currentGrades.add(grade);
				_studentToGradeMap.put(student, currentGrades);
			} else {
				List<Double> newGrades = new ArrayList<>();
				newGrades.add(grade);
				_studentToGradeMap.put(student, newGrades);
			}
		}
	}
	
	public static void computeAverageMap(Map<String, List<Double>> studentToGradeMap) {
		
		for(Map.Entry me : studentToGradeMap.entrySet()) {
			List<Double> grades = (List<Double>) me.getValue();
			Double aveTemp = 0.0;
			for(Double grade : grades) {
				aveTemp += grade; 
			}
			_studentToAverageMap.put((String) me.getKey(), aveTemp/grades.size());
			
		}
	}
	
	public static Map<String, Integer> getWordHashMap(String s) {
		String[] words = s.split(" ");
		Map<String, Integer> mapOccurences = new HashMap<>();
		for(String word : words) {
			if(mapOccurences.containsKey(word)) {
				int tempOccurence = mapOccurences.get(word);
				mapOccurences.put(word, tempOccurence + 1);
			} else {
				mapOccurences.put(word, 1);
			}
		}
		return mapOccurences;
	}
	
	
	public static void main(String[] args) throws IOException {
		/*** Exo 1 - trouver la note la plus haute ***/
		//organizeData("mon_fichier.txt");
		//System.out.println(getMaxGrade());

		/*** Exo 2 - construire une table de hachage avec le nombre d'occurences des mots d'une chaîne de caractères ***/
		//System.out.println(getWordHashMap("alpha omega theta omega omega omg alpha zeta tau rho rho"));
		
		/*** Exo 3 - Faire les moyennes de plusieurs élèves ***/
		//buildGradesMap("mon_fichier.txt");
		//computeAverageMap(_studentToGradeMap);
		//getMaxGrade(_studentToAverageMap);
		//System.out.println(getMaxGrade(_studentToAverageMap));
		
		}
}
