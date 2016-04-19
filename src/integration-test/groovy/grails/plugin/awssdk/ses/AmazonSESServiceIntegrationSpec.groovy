package grails.plugin.awssdk.ses

import grails.test.mixin.integration.Integration
import grails.util.Environment
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import static grails.plugin.awssdk.ses.TestInboxConfig.*

@Integration
class AmazonSESServiceIntegrationSpec extends Specification implements ReadMailIntegrationTest {

    @Autowired
    AmazonSESService amazonSESService

    void "test AmazonSESService.mail method actually delivers an email"() {
        when:
        def subjectStr = 'GRAILS AWS SDK SES Subject'
        int deliveryIndicator = amazonSESService.mail {
            to email
            subject subjectStr
            from email
        }
        then:
        deliveryIndicator == 1

        when: " Fetch emails from the server to test the email have been delivered "
        sleep(10_000) // sleep for ten senconds to ensure the email has reached the server
        def currentEnv = Environment.current
        def emailSubject = "[${currentEnv}] ${subjectStr}"
        boolean emailFound = readEmail.fetchFolderMessageSubjects().any { it == emailSubject}

        then:
        emailFound

        when:
        readEmail.deleteMessagesAtInboxWithSubject(emailSubject)
        emailFound = readEmail.fetchFolderMessageSubjects().any { it == emailSubject}

        then:
        !emailFound
    }

    void "test send attachment"() {
        when:
        def f = new File('src/integration-test/groovy/grails/plugin/awssdk/ses/groovylogo.png')

        then:
        f.exists()

        when:
        def subjectStr = 'GRAILS AWS SDK SES with Attachment'
        int deliveryIndicator = amazonSESService.mailWithAttachment {
            subject subjectStr
            htmlBody '<p>This is an example body</p>'
            to email
            from email
            attachment {
                filepath f.absolutePath
            }
        }

        then:
        deliveryIndicator == 1

        when: " Fetch emails from the server to test the email have been delivered "
        sleep(10_000) // sleep for ten senconds to ensure the email has reached the server
        def currentEnv = Environment.current
        def emailSubject = "[${currentEnv}] ${subjectStr}"
        boolean emailFound = readEmail.fetchFolderMessageSubjects().any { it == emailSubject}
        List<Map> messages = readEmail.messagesWithSubjectAtInbox(emailSubject)

        then:
        emailFound
        messages
        messages.size() >= 1

        when:
        boolean haveAttachments = readEmail.doMessagesWithSubjectAtInboxHaveAttachments(emailSubject)

        then:
        haveAttachments

        when:
        readEmail.deleteMessagesAtInboxWithSubject(emailSubject)
        emailFound = readEmail.fetchFolderMessageSubjects().any { it == emailSubject}

        then:
        !emailFound
    }


    void "test that if you try to send an unsupported attachment an exception is thrown "() {
        when:
        def subjectStr = 'GRAILS AWS SDK SES with Attachment'
        amazonSESService.mailWithAttachment {
            subject subjectStr
            htmlBody '<p>This is an example body</p>'
            to email
            from email
            attachment {
                filepath '/temp/virus.exe'
                filename 'virus.exe'
                mimeType 'application/octet-stream'
            }
        }

        then:
        thrown UnsupportedAttachmentTypeException
    }
}