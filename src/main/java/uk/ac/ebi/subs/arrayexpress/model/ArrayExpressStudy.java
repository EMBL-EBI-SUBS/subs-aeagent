package uk.ac.ebi.subs.arrayexpress.model;


import org.springframework.data.annotation.Id;
import uk.ac.ebi.subs.data.submittable.Study;

import java.util.ArrayList;
import java.util.List;

public class ArrayExpressStudy {
    @Id String accession;

    Study study;

    List<SampleDataRelationship> sampleDataRelationships = new ArrayList<>();

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public List<SampleDataRelationship> getSampleDataRelationships() {
        return sampleDataRelationships;
    }

    public void setSampleDataRelationships(List<SampleDataRelationship> sampleDataRelationships) {
        this.sampleDataRelationships = sampleDataRelationships;
    }
}
