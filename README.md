Grails AWS SDK SES Plugin
=========================

[![Build Status](https://travis-ci.org/agorapulse/grails-aws-sdk-ses.svg?token=BpxbA1UyYnNoUwrDNXtN&branch=master)](https://travis-ci.org/agorapulse/grails-aws-sdk-ses)

# Introduction

The AWS SDK Plugins allow your [Grails](http://grails.org) application to use the [Amazon Web Services](http://aws.amazon.com/) infrastructure services.
The aim is to provide lightweight utility Grails service wrappers around the official [AWS SDK for Java](http://aws.amazon.com/sdkforjava/).

The following services are currently supported:

* [AWS SDK CloudSearch Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-cloudsearch)
* [AWS SDK DynamoDB Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-dynamodb)
* [AWS SDK Kinesis Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-kinesis)
* [AWS SDK S3 Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-s3)
* [AWS SDK SES Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-ses)
* [AWS SDK SQS Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-sqs)

This plugin encapsulates **Amazon SES** related logic.


# Installation

Add plugin dependency to your `build.gradle`:

```groovy
dependencies {
  ...
  compile 'org.grails.plugins:aws-sdk-ses:2.0.0-beta1'
  ...
```

If not provided (for example with `rest-api` profile), you might have to had GSP plugin dependency.

```groovy
...
apply plugin:"org.grails.grails-gsp"
...

dependencies {
    ...
    compile "org.grails:grails-plugin-gsp"
    ...
}
```

# Config

Create an AWS account [Amazon Web Services](http://aws.amazon.com/), in order to get your own credentials accessKey and secretKey.


## AWS SDK for Java version

You can override the default AWS SDK for Java version by setting it in your _gradle.properties_:

```
awsJavaSdkVersion=1.10.66
```

## Credentials

Add your AWS credentials parameters to your _grails-app/conf/application.yml_:

```yml
grails:
    plugin:
        awssdk:
            accessKey: {ACCESS_KEY}
            secretKey: {SECRET_KEY}
```

If you do not provide credentials, a credentials provider chain will be used that searches for credentials in this order:

* Environment Variables - `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY`
* Java System Properties - `aws.accessKeyId and `aws.secretKey`
* Instance profile credentials delivered through the Amazon EC2 metadata service (IAM role)

## Region

The default region used is **us-east-1**. You might override it in your config:

```yml
grails:
    plugin:
        awssdk:
            region: eu-west-1
```

If you're using multiple AWS SDK Grails plugins, you can define specific settings for each services.

```yml
grails:
    plugin:
        awssdk:
            accessKey: {ACCESS_KEY} # Global default setting
            secretKey: {SECRET_KEY} # Global default setting
            region: us-east-1       # Global default setting
            ses:
                accessKey: {ACCESS_KEY} # Service setting (optional)
                secretKey: {SECRET_KEY} # Service setting (optional)
                region: eu-west-1       # Service setting (optional)
                notificationEmail: notification@foo.com
                subjectPrefix: [BETA]
            
```

**notificationEmail** allows you define global from/source email.
**subjectPrefix** allows you to automatically prefix all your email subjects (for example, to get a specific env).

By default, in environments other than PROD, subject are prefixed by "[ENV_NAME] ..." (ex: "[DEVELOPMENT] Some test subject")

# Usage

The plugin provides the following Grails artefacts:

* **AmazonSESService**
* **AmazonSESTemplateService**

And the following interface:

* **MailRecipient**

To send a custom HTML email.
The send method returns:
 
* 1 if successful, 
* 0 if not sent, 
* -1 if blacklisted

```groovy
String htmlBody = '''
<html>
<body>
The email content
</body>
</html>
'''

int statusId = amazonSESService.send(
    'ben@foo.com',
    'Some subject',
    htmlBody,
    'notification@foo.com',  	// Optional, default to notificationEmail from config
    'reply@foo.com'             // Optional, reply to email
)
```

You can use a method which takes a closure as an argument

```groovy
int statusId = amazonSESService.mail {
    to 'recipient@foo.com',
    subject 'Some subject'
    from 'sender@foo.com'
}
```

## Send Emails with Attachments

You can send an email with an attachment as illustrated below:

```groovy
int statusId = amazonSESService.mailWithAttachment {
    to 'recipient@foo.com',
    subject 'Some subject'
    from 'sender@foo.com'
    htmlBody '<p>Find your ticket attached</p>'
    attachment {
        filepath '/tmp/ticket.pdf'
    }
}
```

If you want to have more control about the attachment filename, mime type .. you can use:

```groovy
int statusId = amazonSESService.mailWithAttachment {
    to 'recipient@foo.com',
    subject 'Some subject'
    from 'sender@foo.com'
    htmlBody '<p>Find your ticket attached</p>'
    attachment {
        filename 'ticket-2344.pdf'
        filepath '/tmp/ticket.pdf'
        mimeType 'application/pdf'
        description 'Your concert ticket'
    }
}
```


**Note**. AWS SES has a list of [unsupported attachment types](http://docs.aws.amazon.com/ses/latest/DeveloperGuide/mime-types.html)

## Send Emails with Templates

To send an email from an GSP template with i18n support.

```groovy
int statusId = amazonSESTemplateService.sendTemplate(
    'ben@foo.com',
    'email.test.subject',
    [],             // Subject variables, if required
    [
        foo: 'Some value to use in the template',
        bar: 'Another value'
    ],
    'test'          // GSP located in '/views/template/emails/_test.gsp'
)
```

Example template _test.gsp:

```html
<html>
<body>
The template content with some foo=${foo} and bar=${bar}<br/>
Sent date: ${sentDate}
</body>
</html>
```

You can send a template using a method which takes a closure as illustrated here:

```groovy
def m = [foo: 'Some value to use in the template',bar: 'Another value']
def statusId = amazonSESTemplateService.mailTemplate {
    to 'ben@foo.com',
    subjectCode 'email.test.subject' // defined in mail.properties
    model m
    templateName 'test' // GSP located in '/views/template/emails/_test.gsp'
}
```

To send an email to a recipient (with a class that implements [MailRecipient](/src/main/groovy/grails/plugins/awssdk/ses/MailRecipient.groovy) interface).

```groovy
int statusId = amazonSESTemplateService.sendTemplateToRecipient(
    recipient,
    'email.test.subject',
    [],             // Subject variables, if required
    [
        foo: 'Some value to use in the template',
        bar: 'Another value'
    ],
    'test'          // GSP located in '/views/template/emails/_test.gsp'
)
```

If required, you can also directly use **AmazonSESClient** instance available at **amazonSESService.client**.

For more info, AWS SDK for Java documentation is located here:

* [AWS SDK for Java](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html)


# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/grails-aws-sdk-ses/issues) section on GitHub.

Feedback and pull requests are welcome!