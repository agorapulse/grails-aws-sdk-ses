package grails.plugin.awssdk.ses

import groovy.transform.CompileStatic

@CompileStatic
class TransactionalEmailAttachment {
    String filename
    String filepath
    String mimeType
    String description = ''

    void filename(String str) {
        this.filename = str
    }

    void filepath(String str) {
        this.filepath = str
    }

    void mimeType(String str) {
        this.mimeType = str
    }

    void description(String str) {
        this.description = str
    }

    String getMimeType() {
        if(!this.mimeType && this.filepath) {
            def f = new File(this.filepath)
            if(f.exists()) {
                this.mimeType = AwsSdkSesMimeType.mimeTypeFromFilename(f.name)
            }
        }

        this.mimeType
    }

    String getFilename() {
        if(!this.filename && this.filepath) {
            def f = new File(this.filepath)
            if(f.exists()) {
                this.filename = f.name
            }
        }

        this.filename
    }
}
