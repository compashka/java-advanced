#!/bin/bash

javadoc \
  -link https://docs.oracle.com/en/java/javase/11/docs/api/ \
  -private \
  -d JAVADOC \
  -cp java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar: \
  ../java-solutions/info/kgeorgiy/ja/pleshanov/implementor/Implementor.java \
  ../../java-advanced-2023/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/Impler.java \
  ../../java-advanced-2023/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/JarImpler.java \
  ../../java-advanced-2023/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java
