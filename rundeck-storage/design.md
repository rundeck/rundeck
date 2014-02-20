# lightweight resource storage

## client API

Client API Components:

* Tree - root or subtree which can store content
* Path - address in a tree
* PathItem - addressable location in the content tree
* Directory - path which contains other directories or resources
* Resource - content item stored in the tree

## SPI

Storage SPI

* ContentFilter - can intercept reads/writes to the tree and modify content

## Configuration API

Configure a Tree by composing storage SPI implementations

* TreeConfiguration - defines a configuration of TreeStorage and ContentFilter
* TreeBuilder - builds a configuration into a Tree

## Configuration
