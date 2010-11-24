include Makefile.inc

DIRS = en

all : 
	-for d in $(DIRS); do ( $(ECHO) d=$$d; cd $$d; $(MAKE) ); done

clean :
	$(ECHO) cleaning up in .
	-for d in $(DIRS); do (cd $$d; $(MAKE) clean ); done
