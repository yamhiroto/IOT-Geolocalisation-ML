import java.nio.file.Path;

public class MP3Song {

	private String auteur;
	private int length;
	private Path filename;
	//private Sound data;
	
	public MP3Song(String filename) {
		this.auteur = "<unknown>";
		//this.filename = filename;
		// On suppose qu'il existe une méthode capable d'extraire la durée d'un MP3
		//this.length = getLength(filename);
	}
	
}
