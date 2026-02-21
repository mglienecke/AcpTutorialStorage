package uk.ac.ed.inf.acpTutorial.controller;

import jakarta.jms.JMSException;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.acpTutorial.service.RabbitMqService;

@RestController
@RequestMapping("/api/v1/acp/rabbitmq")
public class RabbitMqController extends AbstractMessageQueueController {

    public RabbitMqController(RabbitMqService rabbitMqService) {
        super(rabbitMqService);
    }

}
