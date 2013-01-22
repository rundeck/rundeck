include Makefile.inc

DIRS = en
ifndef VERSION
VERSION=$(shell grep version.number= ../version.properties | cut -d= -f 2)
ifndef TAG
TAG=$(shell grep version.tag= ../version.properties | cut -d= -f 2)
endif
ifneq ($(TAG),GA)
VERSION=$(VERSION)-$(TAG)
endif
endif

.PHONY: all clean

all : $(DIRS)
	$(MAKE) VERSION=$(VERSION) -C $<

clean : $(DIRS)
	$(MAKE) -C $< clean
