package grails.plugin.awssdk.ses

import spock.lang.Specification

class AwsSesMailerSpec extends Specification {

    void "test transactionalEmailWithClosure"() {
        when:
        TransactionalEmail transactionalEmail = AwsSesMailer.transactionalEmailWithClosure {
            subject 'Hi Paul'
            htmlBody '<p>This is an example body</p>'
            to 'me@sergiodelamo.com'
            from 'subscribe@groovycalamari.com'
            attachment {
                filename 'test.pdf'
                filepath '/tmp/test.pdf'
                mimeType 'application/pdf'
                description 'An example pdf'
            }
        }

        then:
        transactionalEmail
        transactionalEmail.subject == 'Hi Paul'
        transactionalEmail.htmlBody == '<p>This is an example body</p>'
        transactionalEmail.sourceEmail == 'subscribe@groovycalamari.com'
        transactionalEmail.recipients == ['me@sergiodelamo.com']
        transactionalEmail.destinationEmail == 'me@sergiodelamo.com'
        transactionalEmail.attachments.size() == 1
        transactionalEmail.attachments.first().filename == 'test.pdf'
        transactionalEmail.attachments.first().filepath == '/tmp/test.pdf'
        transactionalEmail.attachments.first().mimeType == 'application/pdf'
        transactionalEmail.attachments.first().description == 'An example pdf'


        when:
        def f = new File('src/integration-test/groovy/grails/plugin/awssdk/ses/groovylogo.png')

        then:
        f.exists()


        when:
        transactionalEmail = AwsSesMailer.transactionalEmailWithClosure {
            subject 'Hi Paul'
            htmlBody '<p>This is an example body</p>'
            to 'me@sergiodelamo.com'
            from 'subscribe@groovycalamari.com'
            attachment {
                filepath f.absolutePath
            }
        }

        then:
        transactionalEmail
        transactionalEmail.subject == 'Hi Paul'
        transactionalEmail.htmlBody == '<p>This is an example body</p>'
        transactionalEmail.sourceEmail == 'subscribe@groovycalamari.com'
        transactionalEmail.recipients == ['me@sergiodelamo.com']
        transactionalEmail.destinationEmail == 'me@sergiodelamo.com'
        transactionalEmail.attachments.size() == 1
        transactionalEmail.attachments.first().filename == 'groovylogo.png'
        transactionalEmail.attachments.first().filepath == f.absolutePath
        transactionalEmail.attachments.first().mimeType == 'image/png'
        transactionalEmail.attachments.first().description == ''


    }
}
