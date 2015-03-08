all: Proxy.class IServer.class Server.class
%.class: %.java
	javac $<

clean:
	rm -rf *.class giant small *.out

grep:
	#find /home/autograde/autolab/autograde
	#tail -n +1 /home/autograde/autolab/autograde/script/grade-ch1.sh
	#tail -n +1 /home/autograde/autolab/autograde/auto/*
	#tail -n +1 /home/autograde/autolab/autograde/tools/check_malloc.py

test:
	cat /tmp/giant | LD_PRELOAD=../lib/lib440lib.so ../tools/440write giant
test_small:
	cat /tmp/small | LD_PRELOAD=../lib/lib440lib.so ../tools/440write small
test_fuck:
	cat /tmp/small | LD_PRELOAD=../lib/lib440lib.so ../tools/440write giant
test_cache:
	rm -rf Cache/*
	rm -rf /tmp/cache/*
	LD_PRELOAD=../lib/lib440lib.so ../tools/440cat foo
test_upload:
	export proxyport15440=12345; date | LD_PRELOAD=../lib/lib440lib.so ../tools/440write foo
proxy1:
	export proxyport15440=12345;java Proxy 127.0.0.1 11122 /tmp/cache 60
proxy2:
	java Proxy 127.0.0.1 11122 /tmp/cache2 100000
test_proxy1:
	export proxyport15440=12345; LD_PRELOAD=../lib/lib440lib.so ../tools/440cat foo
test_proxy2:
	LD_PRELOAD=../lib/lib440lib.so ../tools/440cat foo
test_huge:
	rm -f /tmp/cache/huge
	export proxyport15440=12345; LD_PRELOAD=../lib/lib440lib.so ../tools/440read hugefile
test_write:
	rm -f /tmp/cache/newfile
	rm -f Serverdir/newfile
	export proxyport15440=12345; date | LD_PRELOAD=../lib/lib440lib.so ../tools/440write newfile
