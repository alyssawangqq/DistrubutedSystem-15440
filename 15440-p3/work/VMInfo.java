public class VMInfo {
    private static int count = 2;
    public static int getVMNumb() {
	return count;
    }

    public static void addVM() {
	count++;
    }
}
