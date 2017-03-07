package uk.ac.ebi.subs.arrayexpress.agent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.subs.ArrayExpressAgentApplication;

import uk.ac.ebi.subs.data.FullSubmission;
import uk.ac.ebi.subs.processing.ProcessingCertificate;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.data.component.*;
import uk.ac.ebi.subs.data.submittable.*;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ArrayExpressAgentApplication.class)
public class AeAgentSubsProcessorTest {

    SubmissionEnvelope subEnv;
    FullSubmission sub;
    Sample sa;
    Study st;
    Assay as;
    AssayData ad;

    Study enaStudy;

    @Autowired
    ArrayExpressSubmissionProcessor processor;

    @Test
    public void test(){
        List<ProcessingCertificate> certs = processor.processSubmission(subEnv);

        String processedStatus = ProcessingStatusEnum.Completed.name();

        assertThat("study accessioned", st.getAccession(), startsWith("AE-MTAB-"));


        assertThat("assay accessioned", as.getAccession(), nullValue());


        assertThat("assay data accessioned", ad.getAccession(), nullValue());


        assertThat("ena study untouched", enaStudy.getAccession(),nullValue());


       assertThat("correct certs",
                certs,
                containsInAnyOrder(
                        new ProcessingCertificate(st,Archive.ArrayExpress, ProcessingStatusEnum.Curation, st.getAccession()),
                        new ProcessingCertificate(as,Archive.ArrayExpress, ProcessingStatusEnum.Curation),
                        new ProcessingCertificate(ad,Archive.ArrayExpress, ProcessingStatusEnum.Curation)
                )

                );

    }

    @Before
    public void setUp(){
        Team team = new Team();
        team.setName("test team");

        sa = new Sample();
        sa.setAlias("bob");
        sa.setAccession("S1");
        sa.setArchive(Archive.BioSamples);
        sa.setTeam(team);

        st = new Study();
        st.setArchive(Archive.ArrayExpress);
        st.setAlias("study1");
        st.setTeam(team);

        as = new Assay();
        as.setArchive(Archive.ArrayExpress);
        as.setAlias("exp1");

        as.getSampleUses().add(new SampleUse((SampleRef) sa.asRef()));
        as.setStudyRef((StudyRef)st.asRef());
        as.setTeam(team);

        ad = new AssayData();
        ad.setAlias("run1");
        ad.setArchive(Archive.ArrayExpress);
        ad.setAssayRef((AssayRef) as.asRef());
        ad.setTeam(team);

        enaStudy = new Study();
        enaStudy.setArchive(Archive.Ena);
        enaStudy.setAlias("not to be accessioned here");
        enaStudy.setTeam(team);

        sub = new FullSubmission();
        sub.setTeam(team);
        sub.getSamples().add(sa);
        sub.getStudies().add(st);
        sub.getAssays().add(as);
        sub.getAssayData().add(ad);

        subEnv = new SubmissionEnvelope(sub);
    }
}
