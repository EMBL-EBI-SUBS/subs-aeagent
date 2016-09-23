package uk.ac.ebi.subs.arrayexpress.agent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.subs.ArrayExpressAgentApplication;
import uk.ac.ebi.subs.data.component.*;
import uk.ac.ebi.subs.data.submittable.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ArrayExpressAgentApplication.class)
public class AeAgentSubsProcessorTest {

    Submission sub;
    Sample sa;
    Study st;
    Assay as;
    AssayData ad;

    Study enaStudy;

    @Autowired
    ArrayExpressSubmissionProcessor processor;

    @Test
    public void test(){
        processor.processSubmission(sub);

        String processedStatus = "processed";

        assertThat("study accessioned", st.getAccession(), startsWith("AE-MTAB-"));
        assertThat("study status", st.getStatus(),equalTo(processedStatus));

        assertThat("assay accessioned", as.getAccession(), nullValue());
        assertThat("assay status", as.getStatus(),equalTo(processedStatus));

        assertThat("assay data accessioned", ad.getAccession(), nullValue());
        assertThat("assay data status", ad.getStatus(),equalTo(processedStatus));

        assertThat("ena study untouched", enaStudy.getAccession(),nullValue());
        assertThat("ena study status is null", enaStudy.getStatus(),nullValue());

    }

    @Before
    public void setUp(){
        Domain domain = new Domain();
        domain.setName("test domain");

        sa = new Sample();
        sa.setAlias("bob");
        sa.setAccession("S1");
        sa.setArchive(Archive.Usi);
        sa.setDomain(domain);

        st = new Study();
        st.setArchive(Archive.ArrayExpress);
        st.setAlias("study1");
        st.setDomain(domain);

        as = new Assay();
        as.setArchive(Archive.ArrayExpress);
        as.setAlias("exp1");
        as.setSampleRef((SampleRef) sa.asRef());
        as.setStudyRef((StudyRef)st.asRef());
        as.setDomain(domain);

        ad = new AssayData();
        ad.setAlias("run1");
        ad.setArchive(Archive.ArrayExpress);
        ad.setAssayRef((AssayRef) as.asRef());
        ad.setDomain(domain);

        enaStudy = new Study();
        enaStudy.setArchive(Archive.Ena);
        enaStudy.setAlias("not to be accessioned here");
        enaStudy.setDomain(domain);

        sub = new Submission();
        sub.setDomain(domain);
        sub.getSamples().add(sa);
        sub.getStudies().add(st);
        sub.getAssays().add(as);
        sub.getAssayData().add(ad);
    }
}
