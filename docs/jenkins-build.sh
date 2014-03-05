#!/bin/bash

# script for jenkins

make -C docs clean
make javadoc
make -C docs
