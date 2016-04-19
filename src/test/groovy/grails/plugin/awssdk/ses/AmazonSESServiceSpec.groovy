package grails.plugin.awssdk.ses

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import grails.test.mixin.TestFor
import spock.lang.Specification
import static grails.plugin.awssdk.ses.AwsSdkSesEmailDeliveryStatus.*


@TestFor(AmazonSESService)
class AmazonSESServiceSpec extends Specification {

    void setup() {
        // Mock collaborator
        service.client = Mock(AmazonSimpleEmailServiceClient)
    }

    void setUpSesInteractions(Map parameters = [:]) {
        if (parameters.serviceException) {
            1 * service.client.sendEmail(_) >> {
                throw new AmazonServiceException(parameters.serviceException)
            }
        } else if (parameters.clientException) {
            1 * service.client.sendEmail(_) >> {
                throw new AmazonClientException(parameters.clientException)
            }
        } else {
            1 * service.client.sendEmail(_) >> [messageId: 'someMessageId']
        }
    }

    /**
     * Tests for sendEmail(String destinationEmail, String subject, String htmlBody, String sourceEmail = '', String replyToEmail = '')
     */
    void "Send email"() {
        setup:
        setUpSesInteractions()

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com')

        then:
        statusId == STATUS_DELIVERED
    }

    void "Send email with source email"() {
        setup:
        setUpSesInteractions()

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com')

        then:
        statusId == STATUS_DELIVERED
    }

    void "Send email with reply email"() {
        setup:
        setUpSesInteractions()

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com', 'reply@email.com')

        then:
        statusId == STATUS_DELIVERED
    }

    void "Send email with blacklisted service exception"() {
        setup:
        setUpSesInteractions(serviceException: "Address blacklisted")

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com')

        then:
        statusId == STATUS_BLACKLISTED
    }

    void "Send email with unverified email service exception"() {
        setup:
        setUpSesInteractions(serviceException: "Email address is not verified")

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com')

        then:
        statusId == STATUS_NOT_DELIVERED
    }

    void "Send email with unknown service exception"() {
        setup:
        setUpSesInteractions(serviceException: "Unknown")

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com')

        then:
        statusId == STATUS_NOT_DELIVERED
    }

    void "Send email with unknown client exception"() {
        setup:
        setUpSesInteractions(clientException: "Unknown")

        when:
        int statusId = service.send('destination@email.com', 'Some subject', 'Some html body', 'source@email.com')

        then:
        statusId == STATUS_NOT_DELIVERED
    }


    void "test transactionalEmailWithClosure"() {
        when:
        TransactionalEmail transactionalEmail = service.transactionalEmailWithClosure {
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
        transactionalEmail = service.transactionalEmailWithClosure {
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
