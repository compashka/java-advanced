#!/bin/bash

mkdir out

javac -d out --source-path ../java-solutions/info/kgeorgiy/ja/pleshanov --source-path ../java-solutions \
  --module-path ../../java-advanced-2023/lib:../../java-advanced-2023/artifacts \
  ../java-solutions/info/kgeorgiy/ja/pleshanov/implementor/Implementor.java

cd out || return

jar --create --file=../Implementor.jar --manifest=../META-INFO/MANIFEST.MF info/kgeorgiy/ja/pleshanov/implementor/Implementor.class module-info.class

cd ..
rm -rf out