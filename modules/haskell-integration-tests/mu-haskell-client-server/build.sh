#!/usr/bin/env bash

if [ -z "$(docker images -q cb372/mu-haskell-warm-dot-stack)" ]; then
  echo "Building an image with a warm '~/.stack' directory."
  echo "This will take ages, but we only need to do it once."
  echo
  docker build -t cb372/mu-haskell-warm-dot-stack -f Dockerfile.warm_dot_stack .
fi

docker build -t cb372/mu-scala-haskell-integration-tests .
