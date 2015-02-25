import java.io.*;
public class test{

	public static void main (String[] args) {
		//System.err.println(method(args[0]));
		System.err.println(args[0]);
	}

	public static int method(String s) {
		try{
			//File file = new File(s);
			//try {
			//	if(file.delete()) {
			//		return 1;
			//	}else {
			//		if(!file.exists()) return -1;
			//		if(file.isDirectory() && file.list().length > 0) return -2;
			//		else 
			//			return -3;
			//	}
			//}
			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(s));
			byte[] b = "haha";
			output.write(b,0,b.length);
			output.write(b,0,b.length);
			output.write(b,0,b.length);
			output.flush();
			output.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			return -100;
		}
		return 1;
	}
}
