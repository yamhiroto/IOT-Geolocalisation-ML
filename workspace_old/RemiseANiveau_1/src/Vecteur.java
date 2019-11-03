import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Vecteur {
	
	private ArrayList<Double> _components;
	
	public Vecteur(String s) {
		this._components = new ArrayList<>();
		String[] sTab = s.split(" ");
		for(String el : sTab) {
			this._components.add(Double.parseDouble(el));
		}
	}
	
	public Vecteur(ArrayList<Double> l) {
		this._components = l;
	}
	
	public Vecteur add(Vecteur b) {
		List<Double> l = new ArrayList<>();
		for(int i=0; i<this._components.size(); i++) {
			l.add(this._components.get(i) + b._components.get(i));
		}
		return new Vecteur(l.stream().map(Object::toString)
                .collect(Collectors.joining(" ")));
	}
	
	public double dot(Vecteur b) {
		double res = 0.0;
		for(int i=0; i<this._components.size(); i++) {
			res += this._components.get(i) * b._components.get(i);
		}
		return res;
	}
	
	public String toString() {
		String res = "";
		for(double comp : this._components) {
			res += "," + comp;
		}
		return "[" + res.substring(1) + "]";
	}

}
