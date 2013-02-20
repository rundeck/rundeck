include Makefile.inc

DIRS = en jp
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
	for i in $^ ; do \
	$(MAKE) VERSION=$(VERSION) -C $$i ; \
	done ;

clean : $(DIRS)
	for i in $^ ; do \
	$(MAKE) -C $$i clean ; \
	done ;
