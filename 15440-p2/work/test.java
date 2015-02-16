import java.io.*;

public class test{

	public static void main (String[] args) {
		System.err.println(method(args[0]));
	}

	public static int method(String s) {
		File file = new File(s);
		try {
			if(file.delete()) {
				return 1;
			}else {
				if(!file.exists()) return -1;
				if(file.isDirectory() && file.list().length > 0) return -2;
				else 
					return -3;
			}
		}catch(Exception e) {
			e.printStackTrace();
			return -100;
		}
	}
}
