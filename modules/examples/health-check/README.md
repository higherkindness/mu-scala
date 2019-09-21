# Health Check Service example

##Run example
Two different terminals, run (in order of appearance):

* Running the server:

```bash
sbt health-server-fs2/run
```
* Running the client:

```bash
sbt "health-client/run fs2 simple"
```


###Expected result
Client terminal shows status checking. 

##Run streaming example
Four different terminals, run (in order of appearance):

* Running the server:

```bash
sbt health-server-fs2/run
```
* Running the client (for watching "example1" service to update health status):

```bash
sbt "health-client/run fs2 watch 1"
```

* Running the client (for watching "example2" service to update health status):

```bash
sbt "health-client/run fs2 watch 2"
```
* Running the client (for updating health status for example 1):

```bash
sbt "health-client/run fs2 update 1"
```

###Expected result
Terminal two shows updated status. 

###Note
Use `monix` instead of `fs2` to run `monix` example. For example:
```bash
sbt health-server-monix/run
```
and
```bash
sbt "health-client/run monix simple"
```


[comment]: # (Start Copyright)
# Copyright

Mu is designed and developed by 47 Degrees

Copyright (C) 2017-2018 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)
