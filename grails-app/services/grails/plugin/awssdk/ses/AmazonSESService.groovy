package grails.plugin.awssdk.ses

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Region
import com.amazonaws.regions.ServiceAbbreviations
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.model.*
import grails.core.GrailsApplication
import grails.plugin.awssdk.AwsClientUtil
import grails.util.Environment
import org.springframework.beans.factory.InitializingBean

class AmazonSESService implements InitializingBean {
    public static final int STATUS_DELIVERED = 1
    public static final int STATUS_BLACKLISTED = -1
    public static final int STATUS_NOT_DELIVERED = 0

    static SERVICE_NAME = ServiceAbbreviations.Email

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
            assert serviceConfig.notificationEmail, "Default notificationEmail must be set in config"
            sourceEmail = serviceConfig.notificationEmail
        }
        // Prefix email subject for DEV and BETA environment
        if (Environment.current != Environment.PRODUCTION) {
            subject = "[${Environment.current}] $subject"
        } else if (serviceConfig?.subjectPrefix) {
            subject = "${serviceConfig.subjectPrefix} $subject"
        }
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

    // PRIVATE

    def getConfig() {
        grailsApplication.config.grails?.plugin?.awssdk ?: grailsApplication.config.grails?.plugins?.awssdk
    }

    def getServiceConfig() {
        config['ses']
    }

}
