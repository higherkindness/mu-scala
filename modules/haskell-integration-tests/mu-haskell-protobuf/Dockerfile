FROM fpco/stack-build:lts-14.27 as build
RUN mkdir /opt/build
RUN mkdir /opt/build/bin
COPY . /opt/build
RUN cd /opt/build && stack build --system-ghc --copy-bins --local-bin-path /opt/build/bin
FROM ubuntu:19.04
RUN mkdir -p /opt/mu-haskell-protobuf
WORKDIR /opt/mu-haskell-protobuf
RUN apt-get update && apt-get install -y \
  ca-certificates \
  libgmp-dev
COPY --from=build /opt/build/bin .
RUN chmod a+x /opt/mu-haskell-protobuf/server
EXPOSE 9123
