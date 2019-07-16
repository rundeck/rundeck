package rundeck.interceptors

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.PackageScope
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.log4j.Logger


class WebhookInterceptor {
    private static final Logger LOG4J_LOGGER = Logger.getLogger("org.rundeck.webhooks")
    private static final ObjectMapper mapper = new ObjectMapper()

    private static final String AWS_SNS_MESSAGE_TYPE_HEADER = "x-amz-sns-message-type"
    private static final String AWS_SNS_SUBSCRIPTION_CONF_MSG = "SubscriptionConfirmation"

    int order = HIGHEST_PRECEDENCE + 60

    WebhookInterceptor() {
        match(uri: '/api/*/webhook/**')
    }

    boolean before() {
        //handle AWS SNS subscription message
        return checkAWSSNSSubscription()
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    @PackageScope
    boolean checkAWSSNSSubscription() {
        if(request.getHeader(AWS_SNS_MESSAGE_TYPE_HEADER) == AWS_SNS_SUBSCRIPTION_CONF_MSG) {
            def subConfMsg = mapper.readValue(request.inputStream.text,HashMap)
            logInfo("Auto-confirming AWS SNS Topic subscription. Subscription URL: " + subConfMsg.SubscribeURL)
            Response response = null
            try {
                OkHttpClient client = new OkHttpClient.Builder().build()
                Request rq = new Request.Builder().url(subConfMsg.SubscribeURL).get().build()
                response = client.newCall(rq).execute()
                logInfo("AWS Confirm response: " +response.body().string())
            } catch(Exception ex) {
                logError("Unable to auto confirm AWS SNS topic subscription", ex)
            } finally {
                if(response) {
                    response.close()
                }
            }
            render "ok"
            return false
        }
        return true
    }

    @PackageScope
    void logInfo(String msg) {
        LOG4J_LOGGER.info(msg)
    }

    void logError(String errorMessage, Throwable throwable) {
        LOG4J_LOGGER.error(errorMessage, throwable)
    }
}
