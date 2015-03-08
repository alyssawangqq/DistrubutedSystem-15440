import java.io.*;
public class test{

	public static int num = 100;
	public static void main (String[] args) {
		//System.err.println(method(args[0]));
		//System.err.println(args[0]);
		//testArr();
		//num = decr(num);
		//num = decr(num);
		//System.err.println(num);

		File f = new File("a/b/c");
		//if(f.exists()) System.err.println("exist");
		String s = f.getParent();
		//System.err.println(s);
		//String a = "/a/b/c";
		//String b = "/a/b";
		//System.err.println(a.replace(b, ""));
	}

	public static void testArr() {
		String[] a = new String[10000];
		System.err.println(a.length);
		a[a.length] = "a";
		System.err.println(a.length);
	}

	public static int decr(int i) {
		i -= 10;
		return i;
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
			//BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(s));
			//byte[] b = "haha";
			//output.write(b,0,b.length);
			//output.write(b,0,b.length);
			//output.write(b,0,b.length);
			//output.flush();
			//output.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			return -100;
		}
		return 1;
	}
}
