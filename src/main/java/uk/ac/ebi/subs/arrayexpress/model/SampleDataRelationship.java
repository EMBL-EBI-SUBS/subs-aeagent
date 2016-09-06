package uk.ac.ebi.subs.arrayexpress.model;


import uk.ac.ebi.subs.data.submittable.Assay;
import uk.ac.ebi.subs.data.submittable.AssayData;
import uk.ac.ebi.subs.data.submittable.Sample;

public class SampleDataRelationship {

    Assay assay;
    Sample sample;
    AssayData assayData;

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

    public AssayData getAssayData() {
        return assayData;
    }

    public void setAssayData(AssayData assayData) {
        this.assayData = assayData;
    }
}
