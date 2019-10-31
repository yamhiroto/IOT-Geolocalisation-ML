import java.util.Vector;

public class Demo {

	public static void main(String[] args) {
		Vecteur v = new Vecteur("1 2 3");
		Vecteur w = new Vecteur("3 4 5");
		
		// Créer un nouveau vecteur
		Vecteur x = v.add(w);
		System.out.println(x);
		
		System.out.println(v.dot(w));
	}
	
	
}
