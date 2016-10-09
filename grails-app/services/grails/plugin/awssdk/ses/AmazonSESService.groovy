package grails.plugin.awssdk.ses

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Region
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.model.*
import grails.core.GrailsApplication
import grails.plugin.awssdk.AwsClientUtil
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.BodyPart
import javax.mail.Session
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import java.nio.ByteBuffer

import static grails.plugin.awssdk.ses.AwsSdkSesEmailDeliveryStatus.*

@Slf4j
class AmazonSESService implements InitializingBean {

    static SERVICE_NAME = AmazonSimpleEmailService.ENDPOINT_PREFIX

    GrailsApplication grailsApplication
    AmazonSimpleEmailServiceClient client

    void afterPropertiesSet() throws Exception {
        // Set region
        Region region = AwsClientUtil.buildRegion(config, serviceConfig)
        assert region?.isServiceSupported(SERVICE_NAME)

        // Create client
        def credentials = AwsClientUtil.buildCredentials(config, serviceConfig)
        ClientConfiguration configuration = AwsClientUtil.buildClientConfiguration(config, serviceConfig)
        client = new AmazonSimpleEmailServiceClient(credentials, configuration)
                .withRegion(region)
    }
    /**
     * @return 1 if successful, 0 if not sent, -1 if blacklisted
     */
    int mail(@DelegatesTo(TransactionalEmail) Closure composer) throws Exception {

        def transactionalEmail = transactionalEmailWithClosure(composer)

        send(transactionalEmail.destinationEmail,
                transactionalEmail.subject,
                transactionalEmail.htmlBody,
                transactionalEmail.sourceEmail,
                transactionalEmail.replyToEmail)
    }

    /**
     *
     * @param destinationEmail
     * @param subject
     * @param htmlBody
     * @param sourceEmail
     * @param replyToEmail
     * @return 1 if successful, 0 if not sent, -1 if blacklisted
     */
    int send(String destinationEmail,
             String subject,
             String htmlBody,
             String sourceEmail = '',
             String replyToEmail = '') {
        int statusId = STATUS_NOT_DELIVERED
        if (!destinationEmail) {
            return statusId
        }
        if (!sourceEmail) {
            assert serviceConfig.sourceEmail, "Default sourceEmail must be set in config"
            sourceEmail = serviceConfig.sourceEmail
        }

        subject = preffixSubject(subject)
        Destination destination = new Destination([destinationEmail])
        Content messageSubject = new Content(subject)
        Body messageBody = new Body().withHtml(new Content(htmlBody))
        Message message = new Message(messageSubject, messageBody)
        try {
            SendEmailRequest sendEmailRequest = new SendEmailRequest(sourceEmail, destination, message)
            if (replyToEmail) {
                sendEmailRequest.replyToAddresses = [replyToEmail]
            }
            client.sendEmail(sendEmailRequest)
            statusId = STATUS_DELIVERED
        } catch (AmazonServiceException exception) {
            if (exception.message.find("Address blacklisted")) {
                log.debug "Address blacklisted destinationEmail=$destinationEmail"
                statusId = STATUS_BLACKLISTED
            } else if (exception.message.find("Missing final")) {
                log.warn "Invalid parameter value: destinationEmail=$destinationEmail, sourceEmail=$sourceEmail, replyToEmail=$replyToEmail, subject=$subject"
            } else {
                log.warn exception
            }
        } catch (AmazonClientException exception) {
            log.warn exception
        }
        statusId
    }

    private String preffixSubject(String subject) {
        // Prefix email subject for DEV and BETA environment
        if (Environment.current != Environment.PRODUCTION) {
            subject = "[${Environment.current}] $subject"
        } else if (serviceConfig?.subjectPrefix) {
            subject = "${serviceConfig.subjectPrefix} $subject"
        }
        subject
    }

    // PRIVATE

    def getConfig() {
        grailsApplication.config.grails?.plugin?.awssdk ?: grailsApplication.config.grails?.plugins?.awssdk
    }

    def getServiceConfig() {
        config['ses']
    }


    int mailWithAttachment(@DelegatesTo(TransactionalEmail) Closure composer) throws UnsupportedAttachmentTypeException {
        def transactionalEmail = transactionalEmailWithClosure(composer)
        sendEmailWithAttachment(transactionalEmail)
    }

    TransactionalEmail transactionalEmailWithClosure(@DelegatesTo(TransactionalEmail) Closure composer) {

        Closure cl = composer.clone()
        TransactionalEmail transactionalEmail = new TransactionalEmail()
        cl.delegate = transactionalEmail
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        transactionalEmail
    }

    int sendEmailWithAttachment(TransactionalEmail transactionalEmail) throws UnsupportedAttachmentTypeException {
        int statusId = STATUS_NOT_DELIVERED

        Session session = Session.getInstance(new Properties())
        MimeMessage mimeMessage = new MimeMessage(session)
        def subject = preffixSubject(transactionalEmail.subject)
        mimeMessage.setSubject(subject)
        MimeMultipart mimeMultipart = new MimeMultipart()

        BodyPart p = new MimeBodyPart()
        p.setContent(transactionalEmail.htmlBody, "text/html")
        mimeMultipart.addBodyPart(p)

        for(TransactionalEmailAttachment attachment : transactionalEmail.attachments) {

            if(!AwsSdkSesMimeType.isMimeTypeSupported(attachment.mimeType)) {
                throw new UnsupportedAttachmentTypeException()
            }

            MimeBodyPart mimeBodyPart = new MimeBodyPart()
            mimeBodyPart.setFileName(attachment.filename)
            mimeBodyPart.setDescription(attachment.description, "UTF-8")
            DataSource ds = new ByteArrayDataSource(new FileInputStream(new File(attachment.filepath)), attachment.mimeType)
            mimeBodyPart.setDataHandler(new DataHandler(ds))
            mimeMultipart.addBodyPart(mimeBodyPart);
        }
        mimeMessage.content = mimeMultipart

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        mimeMessage.writeTo(outputStream)
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()))

        SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)

        rawEmailRequest.setDestinations(transactionalEmail.recipients)
        rawEmailRequest.setSource(transactionalEmail.sourceEmail)

        try {
            client.sendRawEmail(rawEmailRequest);
            statusId = STATUS_DELIVERED

        } catch (AmazonServiceException exception) {
            if (exception.message.find("Address blacklisted")) {

                log.debug "Address blacklisted destinationEmail=${transactionalEmail.recipients.toString()}"
                statusId = STATUS_BLACKLISTED
            } else if (exception.message.find("Missing final")) {
                log.warn "Invalid parameter value: destinationEmail=${transactionalEmail.recipients.toString()}, sourceEmail=${transactionalEmail.sourceEmail}, replyToEmail=${transactionalEmail.replyToEmail}, subject=${subject}"
            } else {
                log.warn exception
            }
        } catch (AmazonClientException exception) {
            log.warn exception

        }
        statusId
    }
}
