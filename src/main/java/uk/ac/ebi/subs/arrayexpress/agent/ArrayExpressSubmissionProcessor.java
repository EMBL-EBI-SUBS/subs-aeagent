package uk.ac.ebi.subs.arrayexpress.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.arrayexpress.model.ArrayExpressStudy;
import uk.ac.ebi.subs.arrayexpress.repo.ArrayExpressStudyRepository;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.SubmissionEnvelope;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.SampleUse;
import uk.ac.ebi.subs.data.submittable.*;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Queues;
import uk.ac.ebi.subs.messaging.Topics;
import uk.ac.ebi.subs.arrayexpress.model.SampleDataRelationship;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class ArrayExpressSubmissionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ArrayExpressSubmissionProcessor.class);

    String processedStatusValue = "processed";

    @Autowired
    ArrayExpressStudyRepository aeStudyRepository;

    RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    public ArrayExpressSubmissionProcessor(RabbitMessagingTemplate rabbitMessagingTemplate, MessageConverter messageConverter) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.rabbitMessagingTemplate.setMessageConverter(messageConverter);
    }

    @RabbitListener(queues = {Queues.AE_AGENT})
    public void handleSubmission(SubmissionEnvelope submissionEnvelope) {

        logger.info("received submission {}, most recent handler was ",
                submissionEnvelope.getSubmission().getId(),
                submissionEnvelope.mostRecentHandler());

        processSubmission(submissionEnvelope);

        submissionEnvelope.addHandler(this.getClass());
        logger.info("processed submission {}",submissionEnvelope.getSubmission().getId());

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS,Topics.EVENT_SUBMISSION_PROCESSED, submissionEnvelope);

        logger.info("sent submission {}", submissionEnvelope.getSubmission().getId());

    }

    public void processSubmission(SubmissionEnvelope submissionEnvelope) {

        submissionEnvelope.getSubmission().getStudies().stream()
                .filter(s -> s.getArchive() == Archive.ArrayExpress)
                .forEach(s -> processStudy(s, submissionEnvelope));
    }

    public void processStudy(Study study, SubmissionEnvelope submissionEnvelope) {
        Submission submission = submissionEnvelope.getSubmission();

        if (!study.isAccessioned()) {
            study.setAccession("AE-MTAB-" + UUID.randomUUID());
        }

        ArrayExpressStudy arrayExpressStudy = new ArrayExpressStudy();
        arrayExpressStudy.setAccession(study.getAccession());
        arrayExpressStudy.setStudy(study);

        submission.getAssays().stream()
                .filter(a -> a.getArchive() == Archive.ArrayExpress && a.getStudyRef().isMatch(study))
                .forEach(a -> processAssay(a,submissionEnvelope,arrayExpressStudy));


        aeStudyRepository.save(arrayExpressStudy);


        study.setStatus(processedStatusValue);
        for (SampleDataRelationship sdr : arrayExpressStudy.getSampleDataRelationships()){

            sdr.getAssay().setStatus(processedStatusValue);

            for (AssayData ad : sdr.getAssayData()){
                ad.setStatus(processedStatusValue);
            }
        }
    }

    public void processAssay(Assay assay, SubmissionEnvelope submissionEnvelope,ArrayExpressStudy arrayExpressStudy){
        Submission submission = submissionEnvelope.getSubmission();

        SampleDataRelationship sdr = new SampleDataRelationship();
        sdr.setAssay(assay);

        //find sample
        for (SampleUse su : assay.getSampleUses()){
            su.getSampleRef().fillIn(submission.getSamples(),submissionEnvelope.getSupportingSamples());

            if (su.getSampleRef().getReferencedObject() == null){
                throw new RuntimeException("No sample found for "+su.getSampleRef());
            }
        }
        //TODO change sdr to take a list?


        sdr.setSampleUses(assay.getSampleUses());

        //find assay data
        List<AssayData> assayData = submission.getAssayData().stream()
                .filter(ad -> ad.getArchive() == Archive.ArrayExpress && ad.getAssayRef().isMatch(assay))
                .collect(Collectors.toList());

        sdr.setAssayData(assayData);

        arrayExpressStudy.getSampleDataRelationships().add(sdr);
    }

}
