## Firebird

1. Download the driver package from here:
   http://firebirdsql.org/en/jdbc-driver/
   (I used Jaybird-2.2.10-JDK_1.8.zip)

2. Explode the zip file to a temp dir, e.g., ~/tmp/jaybird
   Note: Unlike most distributions, this zip does not include the
         root 'jaybird' folder.

3. Edit (probably new file)  ~/.lein/profiles.clj
   Add this line:
{:user {:plugins [[lein-localrepo "0.5.3"]]}}

4. Run these commands:

     cd ~/asi/src/tao2/winglue-well
     lein localrepo install ~/tmp/jaybird/jaybird-full-2.2.10.jar org/firebirdsql/jdbc 2.2.10

5. In project.clj, review the dependencies, it should include:

    [org/firebirdsql/jdbc "2.2.10"]

6. See resources/tao2-config.clj for examples of configuring data source url

---
## Oracle

1. you need ojdbc7.jar from an oracle install (instant client, whatever)

2. lein localrepo install ojdbc7.jar oracle.jdbc/oracledriver 12.1.0.2.0

---
## Protobufs

Lein is a pain in chasing unreferenced dependencies, so we'll just stuff
them in the project so they come along

1. need protobuf.jar (version 2.6.1)

2. lein localrepo install protobuf-java-2.6.1.jar com/google/protobuf 2.6

https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/2.6.1/protobuf-java-2.6.1.jar

 lein localrepo install $PUNE/glcalcs/*.jar com/ibm/icu/icu4j 56.1

---
# Winglue Well

## glcalcs dependency
1. See top of tao2/CMakeLists.txt for Fedora package deps.
2. mkdir build && cd build && cmake .. && make

## tao2web dependency
1. Checkout tao2web 000-jdt-docker-WORK
2. Follow README.md there to build-min

## Build and run winglue-well
1. Copy -r tao2web/reagenty/resources/public/* into winglue-well/resources/public
2. Copy resources/tao2.clj into this directory and edit.
3. lein ring server-headless

## Deploy
1. lein ring uberjar
2. Follow instructions in deploy/README.md

---
## API Docs

API Docs were made using [Grav Flat-File CMS](https://getgrav.org/).

API Docs can be ran locally with PHP's built in server as described [here](http://linoxide.com/tools/setup-grav-flat-file-based-cms-server/).

TL;DR
```
# In the apidocs directory run
php -S localhost:8000
```

Or can be read off the markdown documents in apidocs/user/pages

Or can be hosted in a server capable of running PHP.
