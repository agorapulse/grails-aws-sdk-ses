package grails.plugin.awssdk.ses

import grails.gsp.PageRenderer
import org.joda.time.LocalDateTime
import org.springframework.context.MessageSource

class AmazonSESTemplateService extends AmazonSESService {

    MessageSource messageSource
    PageRenderer groovyPageRenderer

    /**
     * Global method to send email to anybody
     *
     * @param destinationEmail
     * @param subjectKey
     * @param subjectVariables
     * @param model
     * @param templateName
     * @param locale
     * @param timeZoneGmt
     * @param replyToEmail
     * @return 1 if successful, 0 if not sent, -1 if blacklisted
     */
    int sendTemplate(String destinationEmail,
                     String subjectKey,
                     List subjectVariables,
                     Map model,
                     String templateName,
                     Locale locale = Locale.ENGLISH,
                     int timeZoneGmt = 0,
                     String replyToEmail = '') {
        int statusId = 0
        if (destinationEmail != '') {
            String subject = messageSource.getMessage(subjectKey, subjectVariables as Object[], locale)
            // Render body
            String htmlBody = groovyPageRenderer.render(
                    model: model + [
                            locale  : locale,
                            notificationEmail: destinationEmail,
                            sentDate: new LocalDateTime().plusMinutes((timeZoneGmt * 60).toInteger()).toDate()
                    ],
                    template: "/templates/email/${templateName}"
            )
            // Send email
            statusId = send(destinationEmail, subject, htmlBody, '', replyToEmail)
        }
        statusId
    }

    /**
     * Global method to send email to a recipient (must implement MailRecipient interface)
     *
     * @param recipient
     * @param subjectKey
     * @param subjectVariables
     * @param model
     * @param templateName
     * @param locale
     * @param destinationEmail
     * @param replyToEmail
     * @return 1 if successful, 0 if not sent, -1 if blacklisted
     */
    int sendTemplateToRecipient(MailRecipient recipient,
                                String subjectKey,
                                List subjectVariables,
                                Map model,
                                String templateName,
                                String destinationEmail = '',
                                String replyToEmail = '') {
        int statusId = 0
        if (!destinationEmail && recipient.emailValidated) {
            destinationEmail = recipient.email
        }
        if (destinationEmail != '') {
            statusId = sendTemplate(
                    destinationEmail,
                    subjectKey,
                    subjectVariables,
                    model,
                    templateName,
                    recipient.locale,
                    recipient.timeZoneGmt,
                    replyToEmail
            )
            if (statusId == -1 && recipient.email == destinationEmail) {
                // Email has been blacklisted
                recipient.emailBlacklistedCount++
                recipient.emailValidated = false
                recipient.save()
            }
        }
        statusId
    }

}

