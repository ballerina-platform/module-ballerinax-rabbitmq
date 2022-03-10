import ballerinax/rabbitmq;

listener rabbitmq:Listener rabbitListener = new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

service "demo" on rabbitListener {
}
