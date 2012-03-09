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

import com.mongodb.DB
import com.gmongo.GMongo

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import griffon.util.CallableWithArgs
import static griffon.util.GriffonNameUtils.isBlank

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
class MongodbServerHolder implements MongodbProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MongodbServerHolder)
    private static final Object[] LOCK = new Object[0]
    private final Map<String, GMongo> servers = [:]
  
    String[] getServerNames() {
        List<String> serverNames = new ArrayList().addAll(servers.keySet())
        serverNames.toArray(new String[serverNames.size()])
    }

    GMongo getServer(String serverName = 'default') {
        if(isBlank(serverName)) serverName = 'default'
        retrieveServer(serverName)
    }

    void setServer(String serverName = 'default', GMongo server) {
        if(isBlank(serverName)) serverName = 'default'
        storeServer(serverName, server)
    }

    Object withMongodb(String serverName = 'default', Closure closure) {
        GMongo server = fetchServer(serverName)
        if(LOG.debugEnabled) LOG.debug("Executing statement on server '$serverName'")
        return closure(serverName, server)
    }

    public <T> T withMongodb(String serverName = 'default', CallableWithArgs<T> callable) {
        GMongo server = fetchServer(serverName)
        if(LOG.debugEnabled) LOG.debug("Executing statement on server '$serverName'")
        callable.args = [serverName, server] as Object[]
        return callable.call()
    }
    
    boolean isServerConnected(String serverName) {
        if(isBlank(serverName)) serverName = 'default'
        retrieveServer(serverName) != null
    }
    
    void disconnectServer(String serverName) {
        if(isBlank(serverName)) serverName = 'default'
        storeServer(serverName, null)
    }

    private GMongo fetchServer(String serverName) {
        if(isBlank(serverName)) serverName = 'default'
        GMongo server = retrieveServer(serverName)
        if(server == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = MongodbConnector.instance.createConfig(app)
            server = MongodbConnector.instance.connect(app, config, serverName)
        }
        
        if(server == null) {
            throw new IllegalArgumentException("No such mongodb server configuration for name $serverName")
        }
        server
    }

    private GMongo retrieveServer(String serverName) {
        synchronized(LOCK) {
            servers[serverName]
        }
    }

    private void storeServer(String serverName, GMongo server) {
        synchronized(LOCK) {
            servers[serverName] = server
        }
    }
}
