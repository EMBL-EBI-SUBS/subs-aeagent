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
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.SampleUse;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.Assay;
import uk.ac.ebi.subs.data.submittable.AssayData;
import uk.ac.ebi.subs.data.submittable.Sample;
import uk.ac.ebi.subs.data.submittable.Study;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Queues;
import uk.ac.ebi.subs.messaging.Topics;
import uk.ac.ebi.subs.processing.ProcessingCertificate;
import uk.ac.ebi.subs.processing.ProcessingCertificateEnvelope;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.processing.UpdatedSamplesEnvelope;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class ArrayExpressSubmissionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ArrayExpressSubmissionProcessor.class);

    ProcessingStatusEnum processedStatusValue = ProcessingStatusEnum.Completed;

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

    @RabbitListener(queues = Queues.AE_SAMPLES_UPDATED)
    public void handleSampleUpdate(UpdatedSamplesEnvelope updatedSamplesEnvelope){
        logger.info("received updated samples for submission {}",updatedSamplesEnvelope.getSubmissionId());

        Map<String,Sample> samplesByAccession = new HashMap<>();
        updatedSamplesEnvelope.getUpdatedSamples().forEach(s -> samplesByAccession.put(s.getAccession(),s));

        String[] updatedSampleAccessions = new String[0];
        updatedSampleAccessions = (String[])samplesByAccession.keySet().toArray(updatedSampleAccessions);

        List<SampleDataRelationship> sdrs = sampleDataRelatioshipRepository.findBySampleAccessions(updatedSampleAccessions);

        logger.debug("found {} sdrs for sample update for submission {}",sdrs.size(),updatedSamplesEnvelope.getSubmissionId());

        for(SampleDataRelationship sdr : sdrs){
            ListIterator<Sample> sampleListIterator = sdr.getSamples().listIterator();

            while (sampleListIterator.hasNext()){
                Sample s = sampleListIterator.next();

                if (samplesByAccession.containsKey(s.getAccession())){
                    sampleListIterator.remove();
                    sampleListIterator.add(samplesByAccession.get(s.getAccession()));
                    logger.debug("update sample {} in sdr {} ",s.getAccession(),sdr.getId());
                }
            }

        }

        sampleDataRelatioshipRepository.save(sdrs);


        logger.info("finished updating samples for submission {}", updatedSamplesEnvelope.getSubmissionId());
    }

    @RabbitListener(queues = Queues.AE_AGENT)
    public void handleSubmission(SubmissionEnvelope submissionEnvelope) {
        logger.info("received submission {}",
                submissionEnvelope.getSubmission().getId());

        List<ProcessingCertificate> certs = processSubmission(submissionEnvelope);

        logger.info("processed submission {}",submissionEnvelope.getSubmission().getId());

        ProcessingCertificateEnvelope processingCertificateEnvelope = new ProcessingCertificateEnvelope(submissionEnvelope.getSubmission().getId(),certs);

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS,Topics.EVENT_SUBMISSION_AGENT_RESULTS, processingCertificateEnvelope);

        logger.info("sent submission {}", submissionEnvelope.getSubmission().getId());

    }

    public List<ProcessingCertificate> processSubmission(SubmissionEnvelope submissionEnvelope) {

        List<ProcessingCertificate> certs = new ArrayList<>();

        submissionEnvelope.getStudies().stream()
                .filter(s -> s.getArchive() == Archive.ArrayExpress)
                .forEach(s -> certs.addAll(processStudy(s, submissionEnvelope)));

        return certs;
    }

    public List<ProcessingCertificate> processStudy(Study study, SubmissionEnvelope submissionEnvelope) {
        List<ProcessingCertificate> certs = new ArrayList<>();

        if (!study.isAccessioned()) {
            study.setAccession("AE-MTAB-" + UUID.randomUUID());
        }

        ArrayExpressStudy arrayExpressStudy = new ArrayExpressStudy();
        arrayExpressStudy.setAccession(study.getAccession());
        arrayExpressStudy.setStudy(study);

        certs.add(
                new ProcessingCertificate(study,Archive.ArrayExpress, ProcessingStatusEnum.Curation,arrayExpressStudy.getAccession()));


        submissionEnvelope.getAssays().stream()
                .filter(a -> a.getArchive() == Archive.ArrayExpress && a.getStudyRef().isMatch(study))
                .forEach(a -> certs.addAll(processAssay(a,submissionEnvelope,arrayExpressStudy)));

        try {
            aeStudyRepository.save(arrayExpressStudy);
            sampleDataRelatioshipRepository.save(arrayExpressStudy.getSampleDataRelationships());
        } catch (BsonSerializationException e) {
            logger.error("ArrayExpressStudy {" + arrayExpressStudy.getAccession() + "} bson document exceeds size limit:", e);
            return Collections.emptyList();
        }

        return certs;
    }

    public List<ProcessingCertificate> processAssay(Assay assay, SubmissionEnvelope submissionEnvelope, ArrayExpressStudy arrayExpressStudy){
        List<ProcessingCertificate> certs = new ArrayList<>();

        SampleDataRelationship sdr = new SampleDataRelationship();
        sdr.setId(UUID.randomUUID().toString());
        sdr.setAssay(assay);

        certs.add(new ProcessingCertificate(assay,Archive.ArrayExpress, ProcessingStatusEnum.Curation));

        //find sample
        for (SampleUse su : assay.getSampleUses()){
            Sample s = su.getSampleRef().fillIn(submissionEnvelope.getSamples(),submissionEnvelope.getSupportingSamples());

            if (s == null){
                throw new RuntimeException("No sample found for "+su.getSampleRef());
            }
            sdr.getSamples().add(s);
        }
        //TODO change sdr to take a list?


        sdr.setSampleUses(assay.getSampleUses());

        //find assay data
        List<AssayData> assayData = submissionEnvelope.getAssayData().stream()
                .filter(ad -> ad.getArchive() == Archive.ArrayExpress && ad.getAssayRef().isMatch(assay))
                .collect(Collectors.toList());

        assayData.forEach(ad ->
            certs.add(new ProcessingCertificate(ad,Archive.ArrayExpress, ProcessingStatusEnum.Curation))
        );

        sdr.setAssayData(assayData);

        arrayExpressStudy.getSampleDataRelationships().add(sdr);

        return certs;
    }

}
