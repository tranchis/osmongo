osmongo
=======

Tool that allows to convert OSM XML files into MongoDB databases, in Clojure. Invokable as a Java library.

To compile:

```bash
lein uberjar
```

To execute:

```bash
java -jar target/osmongo-0.1.0-SNAPSHOT-standalone.jar "path/to/file.osm" "http://username:password@mongoserver:port/database"
```

