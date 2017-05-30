import java.io.IOException;


public class Main {
	public static void main(String args[]){
		Apriori a  = new Apriori();
		try {
			a.runAlgorithm(.5, "vote.arff", "a.txt", 30);
		} catch (IOException e) {
			e.printStackTrace();
		}
		a.printStats();
	}

}
