package uk.ac.ebi.subs.arrayexpress.agent;

import org.bson.BsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.arrayexpress.model.ArrayExpressStudy;
import uk.ac.ebi.subs.arrayexpress.model.SampleDataRelationship;
import uk.ac.ebi.subs.arrayexpress.repo.ArrayExpressStudyRepository;
import uk.ac.ebi.subs.arrayexpress.repo.SampleDataRelatioshipRepository;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.SampleUse;
import uk.ac.ebi.subs.data.submittable.Assay;
import uk.ac.ebi.subs.data.submittable.AssayData;
import uk.ac.ebi.subs.data.submittable.Study;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Queues;
import uk.ac.ebi.subs.messaging.Topics;
import uk.ac.ebi.subs.processing.AgentResults;
import uk.ac.ebi.subs.processing.Certificate;
import uk.ac.ebi.subs.processing.ProcessingStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class ArrayExpressSubmissionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ArrayExpressSubmissionProcessor.class);

    String processedStatusValue = "processed";

    @Autowired
    ArrayExpressStudyRepository aeStudyRepository;
    @Autowired
    SampleDataRelatioshipRepository sampleDataRelatioshipRepository;

    RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    public ArrayExpressSubmissionProcessor(RabbitMessagingTemplate rabbitMessagingTemplate, MessageConverter messageConverter) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.rabbitMessagingTemplate.setMessageConverter(messageConverter);
    }

    @RabbitListener(queues = {Queues.AE_AGENT})
    public void handleSubmission(SubmissionEnvelope submissionEnvelope) {
        logger.info("received submission {}",
                submissionEnvelope.getSubmission().getId());

        List<Certificate> certs = processSubmission(submissionEnvelope);

        logger.info("processed submission {}",submissionEnvelope.getSubmission().getId());

        AgentResults agentResults = new AgentResults(submissionEnvelope.getSubmission().getId(),certs);

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS,Topics.EVENT_SUBMISSION_AGENT_RESULTS, agentResults);

        logger.info("sent submission {}", submissionEnvelope.getSubmission().getId());

    }

    public List<Certificate> processSubmission(SubmissionEnvelope submissionEnvelope) {

        List<Certificate> certs = new ArrayList<>();

        submissionEnvelope.getSubmission().getStudies().stream()
                .filter(s -> s.getArchive() == Archive.ArrayExpress)
                .forEach(s -> certs.addAll(processStudy(s, submissionEnvelope)));

        return certs;
    }

    public List<Certificate> processStudy(Study study, SubmissionEnvelope submissionEnvelope) {
        List<Certificate> certs = new ArrayList<>();
        Submission submission = submissionEnvelope.getSubmission();

        if (!study.isAccessioned()) {
            study.setAccession("AE-MTAB-" + UUID.randomUUID());
        }

        ArrayExpressStudy arrayExpressStudy = new ArrayExpressStudy();
        arrayExpressStudy.setAccession(study.getAccession());
        arrayExpressStudy.setStudy(study);

        certs.add(new Certificate(study,Archive.ArrayExpress, ProcessingStatus.Curation,arrayExpressStudy.getAccession()));


        submission.getAssays().stream()
                .filter(a -> a.getArchive() == Archive.ArrayExpress && a.getStudyRef().isMatch(study))
                .forEach(a -> certs.addAll(processAssay(a,submissionEnvelope,arrayExpressStudy)));

        try {
            aeStudyRepository.save(arrayExpressStudy);
            sampleDataRelatioshipRepository.save(arrayExpressStudy.getSampleDataRelationships());
        } catch (BsonSerializationException e) {
            logger.error("ArrayExpressStudy {" + arrayExpressStudy.getAccession() + "} bson document exceeds size limit:", e);
            return Collections.emptyList();
        }

        study.setStatus(processedStatusValue);
        for (SampleDataRelationship sdr : arrayExpressStudy.getSampleDataRelationships()){

            sdr.getAssay().setStatus(processedStatusValue);

            for (AssayData ad : sdr.getAssayData()){
                ad.setStatus(processedStatusValue);
            }
        }

        return certs;
    }

    public List<Certificate> processAssay(Assay assay, SubmissionEnvelope submissionEnvelope,ArrayExpressStudy arrayExpressStudy){
        List<Certificate> certs = new ArrayList<>();

        Submission submission = submissionEnvelope.getSubmission();

        SampleDataRelationship sdr = new SampleDataRelationship();
        sdr.setId(UUID.randomUUID().toString());
        sdr.setAssay(assay);

        certs.add(new Certificate(assay,Archive.ArrayExpress,ProcessingStatus.Curation));

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

        assayData.forEach(ad ->
            certs.add(new Certificate(ad,Archive.ArrayExpress,ProcessingStatus.Curation))
        );

        sdr.setAssayData(assayData);

        arrayExpressStudy.getSampleDataRelationships().add(sdr);

        return certs;
    }

}
