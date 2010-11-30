include ../../Makefile.inc

DIRS = man1 man5

.PHONY: all clean

all : $(DIRS)
	$(MAKE) -C $<

clean : $(DIRS)
	$(MAKE) -C $< clean
