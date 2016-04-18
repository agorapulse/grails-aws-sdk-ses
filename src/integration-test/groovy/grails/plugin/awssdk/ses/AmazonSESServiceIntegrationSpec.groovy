package grails.plugin.awssdk.ses

import grails.test.mixin.integration.Integration
import grails.util.Environment
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
class AmazonSESServiceIntegrationSpec extends Specification {

    @Autowired
    AmazonSESService amazonSESService

    void "test something"() {
        when:
        def configurationMessage = """
            This test connects to a server to ensure that the email has been really delivered.
            You have to set the next environment variables:
               TEST_INBOX_EMAIL=your.email@gmx.es
               TEST_INBOX_HOST=pop.gmx.com
               TEST_INBOX_PASSWORD=yourpassword
               TEST_INBOX_FOLDER=INBOX
               TEST_INBOX_PROVIDER=pop3
If you are running this test in IntellJ you can configure environment variables for all your tests
with 'Edit Configuration -> Deafults -> Junit -> Environment Variables
"""

        String email = System.getenv('TEST_INBOX_EMAIL')
        String host = System.getenv('TEST_INBOX_HOST')
        String password = System.getenv('TEST_INBOX_PASSWORD')
        String folder = System.getenv('TEST_INBOX_FOLDER')
        String provider = System.getenv('TEST_INBOX_PROVIDER')

        then:
        assert email, configurationMessage
        assert host, configurationMessage
        assert password, configurationMessage
        assert folder, configurationMessage
        assert provider, configurationMessage

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


        then:
        def currentEnv = Environment.current
        def emailSubject = "[${currentEnv}] ${subjectStr}"

        def testInboxConfig = new TestInboxConfig(email: email, host: host, password: password, folder: folder, provider: provider)
        def readEmail = new ReadMail(testInboxConfig: testInboxConfig)
        boolean emailFound = readEmail.fetchFolderMessageSubjects().any { it == emailSubject}

        then:
        emailFound

        when:
        readEmail.deleteMessagesAtInboxWithSubject(emailSubject)
        emailFound = readEmail.fetchFolderMessageSubjects().any { it == emailSubject}

        then:
        !emailFound
    }
}