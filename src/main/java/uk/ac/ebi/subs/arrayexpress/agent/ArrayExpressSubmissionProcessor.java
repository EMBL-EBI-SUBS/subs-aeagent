package uk.ac.ebi.subs.arrayexpress.agent;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.arrayexpress.model.ArrayExpressStudy;
import uk.ac.ebi.subs.arrayexpress.repo.ArrayExpressStudyRepository;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.submittable.*;
import uk.ac.ebi.subs.messaging.Channels;
import uk.ac.ebi.subs.arrayexpress.model.SampleDataRelationship;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class ArrayExpressSubmissionProcessor {

    String processedStatusValue = "processed";

    @Autowired
    ArrayExpressStudyRepository aeStudyRepository;

    RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    public ArrayExpressSubmissionProcessor(RabbitMessagingTemplate rabbitMessagingTemplate, MessageConverter messageConverter) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.rabbitMessagingTemplate.setMessageConverter(messageConverter);
    }

    @RabbitListener(queues = {Channels.AE_PROCESSING})
    public void handleSubmission(Submission submission) {

        processSubmission(submission);

        rabbitMessagingTemplate.convertAndSend(Channels.SUBMISSION_PROCESSED, submission);
    }

    public void processSubmission(Submission submission) {

        submission.getStudies().stream()
                .filter(s -> s.getArchive() == Archive.ArrayExpress)
                .forEach(s -> processStudy(s, submission));
    }

    public void processStudy(Study study, Submission submission) {

        if (!study.isAccessioned()) {
            study.setAccession("AE-MTAB-" + UUID.randomUUID());
        }

        ArrayExpressStudy arrayExpressStudy = new ArrayExpressStudy();
        arrayExpressStudy.setAccession(study.getAccession());
        arrayExpressStudy.setStudy(study);

        submission.getAssays().stream()
                .filter(a -> a.getArchive() == Archive.ArrayExpress && a.getStudyRef().isMatch(study))
                .forEach(a -> processAssay(a,submission,arrayExpressStudy));


        aeStudyRepository.save(arrayExpressStudy);


        study.setStatus(processedStatusValue);
        for (SampleDataRelationship sdr : arrayExpressStudy.getSampleDataRelationships()){

            for (Submittable s : Arrays.asList((Submittable)sdr.getAssay(),(Submittable)sdr.getAssayData())){
                s.setStatus(processedStatusValue);
            }
        }
    }

    public void processAssay(Assay assay, Submission submission,ArrayExpressStudy arrayExpressStudy){
        SampleDataRelationship sdr = new SampleDataRelationship();
        sdr.setAssay(assay);

        //find sample
        assay.getSampleRef().fillIn(submission.getSamples());

        if (assay.getSampleRef().getReferencedObject() == null){
            throw new RuntimeException("No sample found for "+assay.getSampleRef());
        }

        sdr.setSample(assay.getSampleRef().getReferencedObject());

        //find assay data
        List<AssayData> assayData = submission.getAssayData().stream()
                .filter(ad -> ad.getArchive() == Archive.ArrayExpress && ad.getAssayRef().isMatch(assay))
                .collect(Collectors.toList());

        if (assayData.size() != 1){
            throw new RuntimeException("Need 1 assay data for assay "+assay);
        }

        sdr.setAssayData(assayData.get(0));

        arrayExpressStudy.getSampleDataRelationships().add(sdr);
    }

}
