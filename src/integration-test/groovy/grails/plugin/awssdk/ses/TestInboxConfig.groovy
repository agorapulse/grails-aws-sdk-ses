package grails.plugin.awssdk.ses

import groovy.transform.CompileStatic

@CompileStatic
class TestInboxConfig {
    String email
    String host
    String password
    String folder
    String provider
}
