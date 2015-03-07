public interface LRU{
	//public void hit(String[] addrs, int idx);
	//public void miss(String[] addrs, String s);
	//public void moveTo(String[] addrs, int from, int to);
	public int scan(String[] addrs,int remain_size, String s,int amount);
}
