# JSONConf (for Java)

[![Build Status](https://travis-ci.org/detro/jsonconf.svg?branch=master)](https://travis-ci.org/detro/jsonconf)

Java utlity that to introduce JSON-based configuration to your project.

The aim is to make it intuitive to define a `default` configuration, that is then
overridden based on `user` or `environment` (command line) overrides.

## Basic concepts

Let's say you have a JSON file representing the `default` configuration of your project:

```json
{
    "database" : {
        "host" : "111.222.333.444",
        "port" : 123456,
        "username" : "dbuser",
        "password" : "supersecret"
    },
    "services" : {
        "url1" : "http://production/service/api"
    }
}
```

Usually, while working on your project, you need to override some of the configuration parameters.
Let's say you want your `user` configuration (aka "local" or "development" configuration) to look like this:

```json
{
    "database" : {
        "host" : "localhost",
        "port" : 54321,
        "username" : "dbuser",
        "password" : "supersecret"
    },
    "services" : {
        "url1" : "http://production/service/api"
    }
}
```

All is the same as `default`, except for `database.host` and `database.port`, that are set to different values.
In other words, what you wanted was to change _just those 2 fields_, applying something as simple as:

```json
{
    "database" : {
        "host" : "localhost",
        "port" : 54321
    }
}
```

Essentially, you needed an **algebraic union** of the `default` configuration with the JSON above. This would allow
to keep your configuration alteration small and to the point.

JSONConf allows to do just that. And more.

### More basic concepts

Let's say you are happy with the way you configure your project `default` and how you override your `user` configuration
with an algebraic union. Still, you discover that a new need arised.

In some instances, you need to override the configuration, but changing `user` configuration file might be "too much".
Maybe your change is based on the environment. Maybe is just a temporary need. Or you want to keep the configuration as constant
as possible for a testing environment, but still the machines that are running your code require small differences in configuration.

In Java, usually, this is the place for `SystemProperties`: you just pass a set of `-Dkey=value` to your executable and read it
in your code.

This would require you have to programmatically read those parameters and apply them to your configuration.
For example, on a specific machine, the `database.port` value would be the only difference. But, we have already said, you don't want
to create a specific configuration file.

What if you could run your application like:

```bash
java -jar your_app.jar -Djson[0]=database.port=9999 -Djson[1]=services.url1="http://qa/service/api"
```

This woud mean that your configuration parameter `database.port` is overridden to the value `9999`,
and you are using a [JSON-path](https://github.com/jayway/JsonPath) to override it.

## What is it

_Hopefully you have read the section above. If not, **read it** so I can focus on the code here._

JSONConf consist of just 2 classes:

* `JSONConf`, that will represent the configuration of your application
* `JSONConfBuilder`, that handles creating the JSONConf for you

you don't need anything else.

### Example use

Create 2 files for your project (the actual name of the file is up to you):

* `my-default-config.json`
* `my-user-or-environment-config.json` (optional)

Then load the configuration in your project with something like:

```java
JSONConf c = new JSONConfBuilder("my-default-config.json")
                .withUserConfFilePath("my-user-or-environment-config.json")
                .build();
```

That's it! Start consuming your configuration.

### Advanced use: change name of command line override array

By default JSONConf allows to override the configuration at runtime via a command line parameter called `json`.
This parameter is interpreted as an array and values are expected in the form `-Djson[0]=... -Djson[1]=... -Djson[2]=...`.
An example use:

```bash
java -jar my.jar -Djson[0]=json.path.to.key=value -Djson[1]=json.path.to.another.key=value
```

It's possible to change the name of the array when using the `JSONConfBuilder` though:

```java
JSONConf c = new JSONConfBuilder("my-default-config.json")
                .withUserConfFilePath("my-user-or-environment-config.json")
                .withCLIPropsArray("myjson")
                .build();
```

and so your command line will look like:

```bash
java -jar my.jar -Dmyjson[0]=json.path.to.key=value -Dmyjson[1]=json.path.to.another.key=value
```

### Advanced use: inject alternative `SystemProperties`

By default the `JSONConfBuilder` will pick the command line parameters from `System.getProperties()`.
You can change that doing the following:

```java
Properties sysProps = new Properties();
String cliPropsArrayName = "myarray";

sysProps.setProperty(cliPropsArrayName + "[0]", "name=\"cli config\"");
sysProps.setProperty(cliPropsArrayName + "[1]", "shared.shared_field_num=3");
sysProps.setProperty(cliPropsArrayName + "[2]", "['annoyingly long string'].browsers=\"firefox\"");
sysProps.setProperty(cliPropsArrayName + "[3]", "array_of_nums=[1, 2, 3]");
sysProps.setProperty(cliPropsArrayName + "[4]", "array_of_strings=[\"string1\", \"string2\", \"string3\"]");

JSONConf c = new JSONConfBuilder("default-config.json")
        .withSystemProperties(sysProps)
        .withCLIPropsArray(cliPropsArrayName)
        .build();
```

## Documentation

Please check out the [JavaDoc](https://cdn.rawgit.com/detro/jsonconf/master/docs/javadoc/index.html)
or the [source](https://github.com/detro/jsonconf/blob/master/src/main/java/com/github/detro/jsonconf/JSONConfBuilder.java) itself
to see the different options offered by the `JSONConfBuilder` to help you tailor the configuration to your needs.

## Dependencies

JSONConf builds upon:

```groovy
compile "com.google.code.gson:gson:$gsonVersion"
compile "com.jayway.jsonpath:json-path:$jsonPathVersion"
```

## Include in your project (via Maven Central)

### Maven
```xml
<dependency>
    <groupId>com.github.detro</groupId>
    <artifactId>jsonconf</artifactId>
    <version>0.0.4</version>
</dependency>
```

### Grails / Gradle
```grails
compile 'com.github.detro:jsonconf:0.0.4'
```

### Others
See [search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22jsonconf%22).

## License (BSD)

See [LICENSE.BSD](./LICENSE.BSD) located in the project source root.

## Setup in Intellij in Windows

After cloning the project, enter the directory and change the permission of gradle.bat.
```bash
chmod +x ./gradlew.bat
```
Use gradle to generate an Intellij project.
```bash
./gradlew.bat idea
```
