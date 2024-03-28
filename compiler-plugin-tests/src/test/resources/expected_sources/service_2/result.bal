import ballerinax/rabbitmq;

listener rabbitmq:Listener rabbitListener = new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

service "demo" on rabbitListener {
    int x = 5;
    string hello = "hello";
	remote function onMessage(rabbitmq:AnydataMessage message) returns rabbitmq:Error? {

	}
}
