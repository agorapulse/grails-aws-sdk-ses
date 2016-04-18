package grails.plugin.awssdk.ses

import groovy.transform.CompileStatic

@CompileStatic
class TransactionalEmail {
    String destinationEmail
    String subject
    String htmlBody = '<html><body></body></html>'

    String sourceEmail
    String replyToEmail

    void to(String str) {
        this.destinationEmail = str
    }

    void from(String str) {
        this.sourceEmail = str
    }

    void destinationEmail(String str) {
        this.destinationEmail = str
    }

    void subject(String str) {
        this.subject = str
    }

    void htmlBody(String str) {
        this.htmlBody = str
    }

    void sourceEmail(String str) {
        this.sourceEmail = str
    }

    void replyToEmail(String str) {
        this.replyToEmail = str
    }
}
