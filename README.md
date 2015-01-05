README
======

## ABOUT ##

Lavagna is a small and easy to use agile issue/project tracking software. [![Build Status](https://travis-ci.org/digitalfondue/lavagna.png?branch=master)](https://travis-ci.org/digitalfondue/lavagna)

It requires: Java 7 or better, MySQL (5.1 or better) or PostgreSQL. It can be deployed in a Java servlet container or as a self contained war.


## INSTALL ##

Lavagna supports MySQL (at least 5.1) and PostgreSQL (tested on 9.1) for production use and HSQLDB for testing purposes.

It's distributed in 2 forms:

 - simple war for deploying in your preferred web container
 - self contained war with embedded jetty web server

### For testing purposes ###

If you want to test it locally, you can download the self contained war and run:

```
java -Ddatasource.driver=org.hsqldb.jdbcDriver -Ddatasource.dialect=HSQLDB -Ddatasource.url=jdbc:hsqldb:mem:lavagna -Ddatasource.username=sa -Ddatasource.password= -Dspring.profiles.active=dev -jar lavagna-jetty-console.war --headless
```

Go to http://localhost:8080 and login with "user" (password "user").

### Setup ###

Lavagna requires the following properties to be set on the JVM:

 - datasource.driver=org.hsqldb.jdbcDriver | com.mysql.jdbc.Driver | org.postgresql.Driver
 - datasource.dialect=HSQLDB | MYSQL | PGSQL
 - datasource.url= for example: jdbc:hsqldb:mem:lavagna | jdbc:mysql://localhost:3306/lavagna | jdbc:postgresql://localhost:5432/lavagna
 - datasource.username=[username]
 - datasource.password=[pwd]
 - spring.profiles.active= dev | prod

The db user must be able to create tables and others db objects.

Once the application has been started/deployed, go to

http(s)://[your deploy](:port)/setup/

There you can:

1. configure the application
2. import a Lavagna export

### Configuration steps ###

1. define the base url
2. define the initial login configuration (demo, ldap, oauth, mozilla persona)
3. define the admin user
4. confirm


## DEVELOP ##

### IDE Configuration ###

Lavagna uses project Lombok annotations, you will need to install the support in your IDE.

Use UTF-8 encoding and 120 characters as line width.


### Execute ###

Launch the Web Server:

```
mvn jetty:run
```

For launching Web Server + DB manager (HSQLDB only):

```
mvn jetty:run -DstartDBManager
```

for launching Web Server with the MySQL database (use the mysql profile):

```
mvn jetty:run -Pdev-mysql
```
```
mvn jetty:run -Pdev-pgsql
```
- go to http://localhost:8080
  if you have a 403 error, you must configure the application,
  go to http://localhost:8080/setup, select demo + insert user "user".

- enter
	username: user
	password: user

For debugging:

```
mvnDebug jetty:run
```

For running the test cases:

```
mvn test
```

For running the test cases with MySQL or PostgreSQL:

```
mvn test -Ddatasource.dialect=MYSQL
```
```
mvn test -Ddatasource.dialect=PGSQL
```

For running with jetty-runner:

```
mvn clean install
java -Ddatasource.dialect=HSQLDB -Ddatasource.driver=org.hsqldb.jdbcDriver -Ddatasource.url=jdbc:hsqldb:mem:lavagna -Ddatasource.username=sa -Ddatasource.password= -Dspring.profiles.active=dev -jar target/dependency/jetty-runner.jar --port 8080 target/*.war
```

### Vagrant ###

In order to make it easier to tests on different databases we included 3 Vagrant VMs.
Make sure that you have installed Vagrant and VirtualBox before continuing.

#### Initialization ####

Fetch the submodules:

```
git submodule update --init
```

If you are under windows you need to ensure that the pgsql submodule is not in a broken state,
double check that the file puppet\modules\postgresql\files\validate_postgresql_connection.sh is using the
unix end of line (run dos2unix).

To run the tests with Vagrant boot the VMs with:

```
vagrant up [optionally use pgsql / mysql to boot only one VM]
```

Once that the VM is up and running run the tests:

```
mvn test -Ddatasource.dialect=PGSQL / MYSQL
```


#### Connecting manually: ####

PGSQL: localhost:5432/lavagna as postgres / password

MySQL: localhost:3306/lavagna as root

Oracle: localhost:1521/XE as system / manager

## Notes about databases ##

The application uses UTF-8 at every stage and on MySQL you will need to create a database with the collation set to utf8_bin:

```
CREATE DATABASE lavagna CHARACTER SET utf8 COLLATE utf8_bin;
```


#### Oracle support ####

(THIS SECTION SHOULD BE IGNORED)

First add the vbguest plugin:

> vagrant plugin install vagrant-vbguest

Note: if you have an error while installing the vagrant-vbguest plugin, see https://github.com/WinRb/vagrant-windows/issues/193 , install before the vagrant-login plugin with

> vagrant plugin install vagrant-login


Download Oracle Database 11g Express Edition for Linux x64 from ( http://www.oracle.com/technetwork/database/database-technologies/express-edition/downloads/index.html )

Place the file oracle-xe-11.2.0-1.0.x86_64.rpm.zip in the directory puppet/modules/oracle/files of this project.

Thanks to Hilverd Reker for his GitHub repo: https://github.com/hilverd/vagrant-ubuntu-oracle-xe .



### Code Coverage ###

Jacoco plugin is used.

```
mvn install site
```

-> open target/site/jacoco/index.html with your browser


## DATABASE MIGRATION ##

Can be disabled using the following system property: datasource.disable.migration=true


## CHECK FOR UPDATED DEPENDENCIES ##

Notes:

- HSQLDB atm will not be updated to version 2.3.2 due to a bug
  (default null+unique clause has changed)
- tomcat-jdbc will not be updated to version 8.0.9 due to a strange
  class loader interaction with log4j when launching with mvn jetty:run

```
mvn versions:display-dependency-updates
```
```
mvn versions:display-plugin-updates
```
