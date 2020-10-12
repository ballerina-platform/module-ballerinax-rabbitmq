module io.ballerina.stdlib.rabbitmq {
    requires com.rabbitmq.client;
    requires io.ballerina.jvm;
    requires org.slf4j;
    requires java.transaction.xa;
    exports org.ballerinalang.messaging.rabbitmq.util;
    exports org.ballerinalang.messaging.rabbitmq;
}