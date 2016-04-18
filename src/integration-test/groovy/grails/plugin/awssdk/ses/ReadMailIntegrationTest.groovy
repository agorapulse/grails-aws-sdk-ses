package grails.plugin.awssdk.ses

import static grails.plugin.awssdk.ses.TestInboxConfig.getConfigurationMessage

trait ReadMailIntegrationTest {

    String email
    ReadMail readEmail

    def setup() {
        email = System.getenv('TEST_INBOX_EMAIL')
        assert email, configurationMessage

        String host = System.getenv('TEST_INBOX_HOST')
        assert host, configurationMessage

        String password = System.getenv('TEST_INBOX_PASSWORD')
        assert password, configurationMessage

        String folder = System.getenv('TEST_INBOX_FOLDER')
        assert folder, configurationMessage

        String provider = System.getenv('TEST_INBOX_PROVIDER')
        assert provider, configurationMessage

        def testInboxConfig = new TestInboxConfig(email: email, host: host, password: password, folder: folder, provider: provider)
        readEmail = new ReadMail(testInboxConfig: testInboxConfig)
    }
}