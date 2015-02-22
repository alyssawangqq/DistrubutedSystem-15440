interface IF {
	public int foo();
}

class IF_IMP implements IF {
	public int foo() {
		return 5;
	}
}

public class testInterface {
	public static void main (String [] args) {
		IF_IMP if123= new IF_IMP();
		System.err.println(if123.foo());
	}
}
