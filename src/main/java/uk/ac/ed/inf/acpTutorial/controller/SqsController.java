package uk.ac.ed.inf.acpTutorial.controller;

import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.ac.ed.inf.acpTutorial.service.SqsService;

import jakarta.jms.JMSException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/acp/sqs")
public class SqsController extends AbstractMessageQueueController {

    public SqsController(SqsService sqsService) {
        super(sqsService);
    }

}
