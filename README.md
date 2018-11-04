# depends

The missing linker for the JVM.

## Problem statement

Have you ever gotten an error like this:

```
java.lang.NoClassDefFoundError: com/google/gson/JsonParseException

    at io.kubernetes.client.ApiClient.<init>(ApiClient.java:85)
    at io.kubernetes.client.util.ClientBuilder.build(ClientBuilder.java:202)
    at io.kubernetes.client.util.Config.defaultClient(Config.java:104)
Caused by: java.lang.ClassNotFoundException: com.google.gson.JsonParseException
    at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
    at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:349)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
    ... 26 more
```

Then spent hours trying different versions of dependencies until it magically goes away?

## Solution

Just run: 

```
java -jar depends.jar com.mycompany.app:my-app:1.0-SNAPSHOT --filter=JsonParseException
```

And get a helpful report of what's broken:

```
broken apis:
         com.fasterxml.jackson.core.JsonParseException.getMessage()Ljava/lang/String; is only present in 
        [com.fasterxml.jackson.core:jackson-core [2.9.3, 2.9.5, 2.9.4, 2.9.6]]

        com.fasterxml.jackson.core.JsonParseException.<init>(Lcom/fasterxml/jackson/core/JsonParser;Ljava/lang/String;Ljava/lang/Throwable;)V is only present in 
        [com.fasterxml.jackson.core:jackson-core [2.9.3, 2.9.5, 2.9.4, 2.7.8, 2.9.6]]

        com.fasterxml.jackson.core.JsonParseException.<init>(Lcom/fasterxml/jackson/core/JsonParser;Ljava/lang/String;)V is only present in 
        [com.fasterxml.jackson.core:jackson-core [2.9.3, 2.9.5, 2.9.4, 2.7.8, 2.9.6]]

        com.fasterxml.jackson.core.JsonParseException.<init>(Lcom/fasterxml/jackson/core/JsonParser;Ljava/lang/String;Lcom/fasterxml/jackson/core/JsonLocation;)V is only present in 
        [com.fasterxml.jackson.core:jackson-core [2.9.3, 2.9.5, 2.9.4, 2.7.8, 2.9.6]]

```

Then pick a version from the list!

## How it works

1. It uses [ShrinkWrap Resolver](http://arquillian.org/modules/resolver-shrinkwrap/) to build a dependency graph of your project.
1. It scans through the classes in each jar and uses [ASM4](https://asm.ow2.io/) to find function invocations.
1. It builds a map of all cross-module function calls.
1. It assumes any function called from an external module is a public API.
1. It compares all the versions of each module to see which methods are not present in all versions.
1. It filters these by a given search term.
1. Finally, it prints a report to help you figure out which version to use when a dependency conflict arrises.

## Roadmap

1. Count all the broken calls, and recommend the version of each library most likely to not have missing method exceptions at run time.
1. Produce a report to name-and-shame [semver](https://semver.org/) violations.
1. Convert this project into a [Gradle ResolutionStrategy](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.ResolutionStrategy.html) so it Just Works.

   