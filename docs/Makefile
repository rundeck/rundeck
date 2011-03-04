include Makefile.inc

DIRS = en
VERSION=$(shell grep version.number= ../version.properties | cut -d= -f 2)

.PHONY: all clean

all : $(DIRS)
	$(MAKE) VERSION=$(VERSION) -C $<

clean : $(DIRS)
	$(MAKE) -C $< clean
