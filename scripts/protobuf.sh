#!/usr/bin/env bash

set -ex
wget https://github.com/google/protobuf/archive/v3.3.1.tar.gz
tar -xzvf v3.3.1.tar.gz
cd protobuf-3.3.1 && ./autogen.sh && ./configure --prefix=/usr && make && sudo make install
