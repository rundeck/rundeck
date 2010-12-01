include ../../Makefile.inc

DIRS = man1 man5

.PHONY: clean subdirs $(DIRS)

subdirs: $(DIRS)

$(DIRS) : 
	$(MAKE) -C $@

clean : $(DIRS)
	$(MAKE) -C $< clean
