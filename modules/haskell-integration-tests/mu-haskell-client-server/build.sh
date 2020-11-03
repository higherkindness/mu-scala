#!/usr/bin/env bash

docker pull cb372/mu-haskell-warm-dot-stack:lts-16.20

docker build -t cb372/mu-scala-haskell-integration-tests:mu-haskell-0.4.0 .
