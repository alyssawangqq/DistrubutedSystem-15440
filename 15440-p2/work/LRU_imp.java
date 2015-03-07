public class LRU_imp implements LRU {
	Proxy proxy;
	//FileHandler fh;
	void swap(String[] addrs, int i, int j) {
		String tmp = addrs[i];
		addrs[i] = addrs[j];
		addrs[j] = tmp;
	}
	int hit(String[] addrs,int amount, int idx) {
		moveTo(addrs,idx,amount-1);
		//return amount;
		return -1;
	}
	int miss(String[] addrs,int remain_size,int amount, String s) {
		if(remain_size < 0)  {
			addrs[amount++] = s;
			return amount; // no need for LRU
		}else {
			addrs[0] = s; // should delete addrs[0] file TODO // should take size into consider
			moveTo(addrs, 0, amount-1);
			return -2; // miss return -2 means need LRU, target is the last one in array
		}
	}
	void moveTo(String[] addrs, int from, int to) {
		swap(addrs, from, to);
		for(int i = from; i < to-1; i++) {
			swap(addrs,i, i+1);
		}
	}
	public int scan(String[] addrs,int remain_size, String s,int amount) {
		//System.err.println(Proxy.path);
		for(int i = 0; i < amount; i++) {
			if(addrs[i] == s) {
				return hit(addrs,amount, i);
			}
		}
		return miss(addrs,remain_size,amount,s);
	}
}
