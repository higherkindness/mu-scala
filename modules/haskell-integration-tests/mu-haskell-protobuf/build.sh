#!/usr/bin/env bash

if [ -z "$(docker images -q mu_haskell_warm_dot_stack)" ]; then
  echo "Building an image with a warm '~/.stack' directory."
  echo "This will take ages, but we only need to do it once."
  echo
  docker build -t mu_haskell_warm_dot_stack -f Dockerfile.warm_dot_stack .
fi

docker build -t cb372/mu-scala-haskell-integration-tests-protobuf .
