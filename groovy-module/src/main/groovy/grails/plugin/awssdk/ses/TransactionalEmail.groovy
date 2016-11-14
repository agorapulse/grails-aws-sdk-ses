package grails.plugin.awssdk.ses

import groovy.transform.CompileStatic

@CompileStatic
class TransactionalEmail {
    String sourceEmail
    String subject
    String htmlBody = '<html><body></body></html>'

    String replyToEmail

    List<String> recipients = []

    List<TransactionalEmailAttachment> attachments = []

    void attachment(Closure attachment) {

        Closure cl = (Closure) attachment.clone()
        TransactionalEmailAttachment att = new TransactionalEmailAttachment()
        cl.delegate = att
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        attachments << att

    }

    void recipients(List<String> recipients) {
        this.recipients = recipients
    }

    void to(String str) {
        this.recipients = [str]
    }

    void from(String str) {
        this.sourceEmail = str
    }
    void sourceEmail(String str) {
        this.sourceEmail = str
    }

    void destinationEmail(String str) {
        this.recipients = [str]
    }

    String getDestinationEmail() {
        return (!this.recipients.isEmpty()) ? this.recipients.first() : null
    }

    void subject(String str) {
        this.subject = str
    }

    void htmlBody(String str) {
        this.htmlBody = str
    }



    void replyToEmail(String str) {
        this.replyToEmail = str
    }
}
