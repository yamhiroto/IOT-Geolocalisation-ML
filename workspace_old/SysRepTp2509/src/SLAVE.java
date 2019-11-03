
public class SLAVE {

	public static void main(String[] args) {
		int a = 3;
		int b = 6;
		try {
			Thread.sleep(10000);
			System.err.print(a+b);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
