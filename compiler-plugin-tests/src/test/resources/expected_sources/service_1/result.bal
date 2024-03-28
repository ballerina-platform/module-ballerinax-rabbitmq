import ballerinax/rabbitmq;

listener rabbitmq:Listener rabbitListener = new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

service "demo" on rabbitListener {
	remote function onMessage(rabbitmq:AnydataMessage message) returns rabbitmq:Error? {

	}
}
