/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.plugins.mongodb

import com.gmongo.GMongo

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.CallableWithArgs

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
final class MongodbConnector implements MongodbProvider {
    private bootstrap

    private static final Logger LOG = LoggerFactory.getLogger(MongodbConnector)

    Object withMongodb(String serverName = 'default', Closure closure) {
        MongodbServerHolder.instance.withMongodb(serverName, closure)
    }

    public <T> T withMongodb(String serverName = 'default', CallableWithArgs<T> callable) {
        return MongodbServerHolder.instance.withMongodb(serverName, callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        def serverClass = app.class.classLoader.loadClass('MongodbConfig')
        new ConfigSlurper(Environment.current.name).parse(serverClass)
    }

    private ConfigObject narrowConfig(ConfigObject config, String serverName) {
        return serverName == 'default' ? config.server : config.servers[serverName]
    }

    GMongo connect(GriffonApplication app, ConfigObject config, String serverName = 'default') {
        if (MongodbServerHolder.instance.isServerConnected(serverName)) {
            return MongodbServerHolder.instance.getServer(serverName)
        }

        config = narrowConfig(config, serverName)
        app.event('MongodbConnectStart', [config, serverName])
        GMongo server = startMongodb(config)
        MongodbServerHolder.instance.setServer(serverName, server)
        bootstrap = app.class.classLoader.loadClass('BootstrapMongodb').newInstance()
        bootstrap.metaClass.app = app
        bootstrap.init(serverName, server)
        app.event('MongodbConnectEnd', [serverName, server])
        server
    }

    void disconnect(GriffonApplication app, ConfigObject config, String serverName = 'default') {
        if (MongodbServerHolder.instance.isServerConnected(serverName)) {
            config = narrowConfig(config, serverName)
            GMongo server = MongodbServerHolder.instance.getServer(serverName)
            app.event('MongodbDisconnectStart', [config, serverName, server])
            bootstrap.destroy(serverName, server)
            stopMongodb(config, server)
            app.event('MongodbDisconnectEnd', [config, serverName])
            MongodbServerHolder.instance.disconnectServer(serverName)
        }
    }

    private GMongo startMongodb(ConfigObject config) {
        String host  = config.host ?: 'localhost'
        int port     = config.port ?: 27017i
        
        new GMongo(host, port)
    }

    private void stopMongodb(ConfigObject config, GMongo server) {
        server.close()
    }
}
