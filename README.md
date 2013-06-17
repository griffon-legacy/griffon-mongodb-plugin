
Mongodb support
---------------

Plugin page: [http://artifacts.griffon-framework.org/plugin/mongodb](http://artifacts.griffon-framework.org/plugin/mongodb)


The Mongodb plugin enables lightweight access to [Mongodb][1] databases.
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * MongodbConfig.groovy - contains the database definitions.
 * BootstrapMongodb.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

A new dynamic method named `withMongodb` will be injected into all controllers,
giving you access to a `com.gmongodb.GMongo` object, with which you'll be able
to make calls to the database. Remember to make all database calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.

This method is aware of multiple databases. If no databaseName is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withMongodb { databaseName, database -> ... }
            withMongodb('internal') { databaseName, database -> ... }
        }
    }

This method is also accessible to any component through the singleton `griffon.plugins.mongodb.MongodbConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`MongodbEnhancer.enhance(metaClassInstance, mongodbProviderInstance)`.

Configuration
-------------
### Dynamic method injection

The `withMongodb()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.mongodb.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * MongodbConnectStart[config, databaseName] - triggered before connecting to the database
 * MongodbConnectEnd[databaseName, database] - triggered after connecting to the database
 * MongodbDisconnectStart[config, databaseName, database] - triggered before disconnecting from the database
 * MongodbDisconnectEnd[config, databaseName] - triggered after disconnecting from the database

### Multiple Stores

The config file `MongodbConfig.groovy` defines a default database block. As the name
implies this is the database used by default, however you can configure named databases
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    databases {
        internal {
            host = 'server.acme.com'
        }
    }

This block can be used inside the `environments()` block in the same way as the
default database block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/mongodb][2]

Testing
-------
The `withMongodb()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `MongodbEnhancer.enhance(metaClassInstance, mongodbProviderInstance)` where 
`mongodbProviderInstance` is of type `griffon.plugins.mongodb.MongodbProvider`. The contract for this interface looks like this

    public interface MongodbProvider {
        Object withMongodb(Closure closure);
        Object withMongodb(String serverName, Closure closure);
        <T> T withMongodb(CallableWithArgs<T> callable);
        <T> T withMongodb(String serverName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyMongodbProvider implements MongodbProvider {
        Object withMongodb(String serverName = 'default', Closure closure) { null }
        public <T> T withMongodb(String serverName = 'default', CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            MongodbEnhancer.enhance(service.metaClass, new MyMongodbProvider())
            // exercise service methods
        }
    }


[1]: http://www.mongodb.org
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/mongodb

