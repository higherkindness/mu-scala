---
layout: docs
title: SSL/TLS
permalink: /docs/rpc/ssl-tls
---

# SSL/TLS Encryption

> [gRPC](https://grpc.io/docs/guides/auth.html) has SSL/TLS integration and promotes the use of SSL/TLS to authenticate the server, and encrypt all the data exchanged between the client and the server. Optional mechanisms are available for clients to provide certificates for mutual authentication.

[frees-rpc] allows you to encrypt the connection between the server and the client through SSL/TLS. The main goal of using SSL is to protect your sensitive information and keeps your data secure between servers and clients.

As we mentioned in the [Quickstart](/docs/rpc/quickstart) section, we can choose and configure our client with `OkHttp` or `Netty` but if we want to encrypt our service, it's mandatory to use `Netty`.

[frees-rpc] only supports encryptation over *Netty*.

## Requirements 

On the server and client side, we will need two files to configure the `SslContext` in `gRPC`:

* Server certificate file: Small data files that digitally bind a cryptographic key to an organization’s details. This file could be generated or obtained from a third company.

* Server private key file: The private key is a separate file that is used in the encryption of data sent between your server and the clients. All SSL certificates require a private key to work.

## Usage

The first step to do, in order to start to secure our service is use `frees-rpc-netty-ssl` and `frees-rpc-client-netty`.

