package grails.plugin.awssdk

import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import grails.config.Config

class AwsClientUtil {

    static final String DEFAULT_REGION = 'us-east-1'

    static intValueForConfigName(Config co, String configName, final String serviceConfig,  final String defaultConfig, int defaultValue = 0) {
        def propertykey = "${serviceConfig}.${configName}" as String
        if ( co.getProperty(propertykey, Integer, null) ) {
            return co.getProperty(propertykey, Integer, null).intValue()
        }
        propertykey = "${defaultConfig}.${configName}" as String
        if (co.getProperty(propertykey, Integer, null) ) {
            return  co.getProperty(propertykey, Integer, null).intValue()
        }
        defaultValue
    }

    static stringValueForConfig(Config co,  String configName,  final String serviceConfig, final String defaultConfig, String defaultValue = '') {
        def propertyKey = "${serviceConfig}.${configName}" as String
        if ( co.getProperty(propertyKey, String, null) ) {
            return co.getProperty(propertyKey, String, null)
        }
        propertyKey = "${defaultConfig}.${configName}" as String
        if (co.getProperty(propertyKey, String, null) ) {
            return co.getProperty(propertyKey, String, null)
        }
        defaultValue
    }

    static ClientConfiguration clientConfigurationWithConfig(Config co, final String defaultConfig, final String serviceConfig) {
        int connectionTimeout = intValueForConfigName(co, 'connectionTimeout', serviceConfig, defaultConfig)
        int maxConnections = intValueForConfigName(co, 'maxConnections', serviceConfig,  defaultConfig)
        int maxErrorRetry = intValueForConfigName(co, 'maxErrorRetry', serviceConfig, defaultConfig)
        String protocol = stringValueForConfig(co, 'protocol', serviceConfig, defaultConfig)
        int socketTimeout = intValueForConfigName(co, 'socketTimeout', serviceConfig, defaultConfig)
        String userAgent = stringValueForConfig(co, 'userAgent', serviceConfig, defaultConfig)
        String proxyDomain = stringValueForConfig(co, 'proxyDomain', serviceConfig, defaultConfig)
        String proxyHost = stringValueForConfig(co, 'proxyHost', serviceConfig, defaultConfig)
        String proxyPassword = stringValueForConfig(co, 'proxyPassword', serviceConfig, defaultConfig)
        int proxyPort = intValueForConfigName(co, 'proxyPort', serviceConfig, defaultConfig)
        String proxyUsername = stringValueForConfig(co, 'proxyUsername', serviceConfig, defaultConfig)
        String proxyWorkstation = stringValueForConfig(co, 'proxyWorkstation', serviceConfig, defaultConfig)

        ClientConfiguration clientConfiguration = new ClientConfiguration()
        if (connectionTimeout){
            clientConfiguration.connectionTimeout = connectionTimeout
        }
        if (maxConnections) {
            clientConfiguration.maxConnections = maxConnections
        }
        if (maxErrorRetry) {
            clientConfiguration.maxErrorRetry = maxErrorRetry
        }
        if (protocol) {
            if ( protocol.toUpperCase() == 'HTTP' ) {
                clientConfiguration.protocol = Protocol.HTTP
            } else {
                clientConfiguration.protocol = Protocol.HTTPS
            }
        }
        if (socketTimeout) clientConfiguration.socketTimeout = socketTimeout
        if (userAgent) clientConfiguration.userAgent = userAgent
        if (proxyDomain) clientConfiguration.proxyDomain = proxyDomain
        if (proxyHost) clientConfiguration.proxyHost = proxyHost
        if (proxyPassword) clientConfiguration.proxyPassword = proxyPassword
        if (proxyPort) clientConfiguration.proxyPort = proxyPort
        if (proxyUsername) clientConfiguration.proxyUsername = proxyUsername
        if (proxyWorkstation) clientConfiguration.proxyWorkstation = proxyWorkstation
        clientConfiguration
    }



}
