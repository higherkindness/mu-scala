---
layout: docs
title: Generating sources from IDLs stored in Compendium
permalink: /generate-sources-from-compendium
---

# Generating sources from IDLs stored in [Compendium](https://higherkindness.io/compendium/)

compendium is a standalone solution, implemented as an HTTP service, that provides storage, conversion and client generation for your schemas in a format-agnostic fashion. `sbt-mu-srcgen` provides a feature that enables the user to store and get IDL definitions from compendium.

This section will provide instructions about how to configure the sbt settings to use compendium. We are assuming you have a compendium instance running. If not, please check the [compendium microsite](https://higherkindness.io/compendium/).
Also, the configuration related to the IDL type could be checked on:

* [Protobuf section](generate-sources-from-proto)
* [Avro section](generate-sources-from-avro)
* [OpenAPI section](generate-sources-from-openapi)

## Configure sbt plugin

Settings related to compendium interaction are:

| Setting                                 | Type                      | Description                                                                                                                                                                                             | Default value            |
|:----------------------------------------|:--------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------------------------|
| `muSrcGenExecutionMode`                 | ExecutionMode             | Execution mode of the plugin. If `Compendium, it's required a compendium instance where IDL files are saved. | `Local`                                                                                  |                          |
| `muSrcGenCompendiumServerUrl`           | _String_                  | Compendium server url                                                                                                                                                                                   | `http://localhost:8080` |
| `muSrcGenCompendiumProtocolIdentifiers` | _Seq[ProtocolAndVersion]_ | Protocol identifiers to retrieve from compendium. `ProtocolAndVersion` provides two values: `name` (mandatory) that corresponds with the identifier used to store the protocol and `version` (optional) | `Nil`                    |

So in order to use compendium, you'll need to add those settings in your `build.sbt`:

```scala
muSrcGenCompendiumProtocolIdentifiers := List(ProtocolAndVersion("example1", None))
muSrcGenExecutionMode := Compendium
```

If needed, you can also point out to your compendium server if it's not on the default value, `http://localhost:8080`:

```scala
muSrcGenCompendiumServerUrl := "http://localhost:47047" 
muSrcGenCompendiumProtocolIdentifiers := List(ProtocolAndVersion("example2", Some(2)))
muSrcGenExecutionMode := Compendium
```


[compendium]: https://higherkindness.io/compendium/