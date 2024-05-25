package com.example.demo;

import java.util.Set;

import org.apache.pulsar.client.api.Schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.listener.PulsarRecordMessageListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DynamicContainerDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamicContainerDemoApplication.class, args);
	}

	@RestController
	static class AppController {

		@Autowired
		private GenericApplicationContext applicationContext;

		@Autowired
		private PulsarConsumerFactory<String> consumerFactory;

		@Autowired
		private PulsarTemplate<String> template;

		@GetMapping("/register/{id}")
		String registerListener(@PathVariable String id) {
			System.out.printf("***** REGISTER CONTAINER w/ id = %s%n", id);
			var containerProps = new PulsarContainerProperties();
			containerProps.setMessageListener((PulsarRecordMessageListener<?>) (consumer, msg) ->
					System.out.printf("**** LISTENER-%s-RECEIVE -> %s%n", id, msg.getValue()));
			containerProps.setSchema(Schema.STRING);
			containerProps.setTopics(Set.of("my-topic-%s".formatted(id)));
			containerProps.setSubscriptionName("my-topic-sub-%s".formatted(id));
			var container = new DefaultPulsarMessageListenerContainer<>(this.consumerFactory, containerProps);
			container.start();
			this.applicationContext.registerBean("container-" + id, DefaultPulsarMessageListenerContainer.class, () -> container);
			return "***** REGISTERED CONTAINER w/ id = %s".formatted(id);
		}

		@GetMapping("/send/{id}")
		String sendMessage(@PathVariable String id) {
			var msg = "msg-to-container-%s-%s".formatted(id, System.currentTimeMillis());
			System.out.println("***** SEND " + msg);
			this.template.send("my-topic-" + id, msg);
			return "***** SENT " + msg;
		}

	}
}
