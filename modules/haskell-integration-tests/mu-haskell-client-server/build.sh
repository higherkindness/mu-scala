#!/usr/bin/env bash

docker pull cb372/mu-haskell-warm-dot-stack:latest

docker build -t cb372/mu-scala-haskell-integration-tests .
