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
