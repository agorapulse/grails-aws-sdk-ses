package grails.plugin.awssdk.ses
import javax.mail.*

class ReadMail {

    TestInboxConfig testInboxConfig

    Properties instantiatProperties() {
        Properties props = new Properties()
        props.setProperty("mail.imap.ssl.enable", "javax.net.ssl.SSLSocketFactory")
        props.setProperty("mail.imap.socketFactory.fallback", "false")
        props.setProperty("mail.imap.ssl.enable", "true")
        props
    }

    Store connectToStore() {
        // Connect to the POP3 server
        def props = instantiatProperties()
        Session session = Session.getDefaultInstance props, null
        def store = session.getStore testInboxConfig.provider
        store.connect testInboxConfig.host, testInboxConfig.email, testInboxConfig.password
        store
    }

    Folder openInbox(Store store) {
        // Open the folder
        Folder inbox = store.getFolder testInboxConfig.folder
        if (!inbox) {
            throw new Exception()
        }
        inbox
    }

    List<String> fetchFolderMessageSubjects() throws Exception {
        def store = connectToStore()
        def inbox = openInbox(store)

        inbox.open(Folder.READ_ONLY)

        // Get the messages from the server
        Message[] messages = inbox.getMessages()
        List<String> subjects = []
        for(int i = 0; i < messages.size();i++) {
            subjects << messages[i].subject
        }
        // Close the connection
        // but don't remove the messages from the server

        inbox.close false
        store.close()

        subjects
    }

    int deleteMessagesAtInboxWithSubject(String subject) {
        def store = connectToStore()
        def inbox = openInbox(store)

        inbox.open(Folder.READ_WRITE)

        Message[] messages = inbox.getMessages()
        int numberOfDeletedMessages = 0
        for(int i = 0; i < messages.size();i++) {
            def message = messages[i]
            if(message.subject == subject) {
                message.setFlag(Flags.Flag.DELETED, true);
                numberOfDeletedMessages++
            }
        }
        inbox.close true
        store.close()

        numberOfDeletedMessages
    }
}