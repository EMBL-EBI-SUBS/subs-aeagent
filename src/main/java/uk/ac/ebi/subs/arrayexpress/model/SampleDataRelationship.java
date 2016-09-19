package uk.ac.ebi.subs.arrayexpress.model;


import uk.ac.ebi.subs.data.submittable.Assay;
import uk.ac.ebi.subs.data.submittable.AssayData;
import uk.ac.ebi.subs.data.submittable.Sample;

import java.util.List;

public class SampleDataRelationship {

    Assay assay;
    Sample sample;
    List<AssayData> assayData;

    public Assay getAssay() {
        return assay;
    }

    public void setAssay(Assay assay) {
        this.assay = assay;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public List<AssayData> getAssayData() {
        return assayData;
    }

    public void setAssayData(List<AssayData> assayData) {
        this.assayData = assayData;
    }
}
