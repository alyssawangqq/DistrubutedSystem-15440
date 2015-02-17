all: Proxy.class Util.class test.class

%.class: %.java
	javac $<

clean:
	rm -f *.class giant small

test:
	cat /tmp/giant | LD_PRELOAD=../lib/lib440lib.so ../tools/440write giant
test_small:
	cat /tmp/small | LD_PRELOAD=../lib/lib440lib.so ../tools/440write small
test_fuck:
	cat /tmp/small | LD_PRELOAD=../lib/lib440lib.so ../tools/440write giant
