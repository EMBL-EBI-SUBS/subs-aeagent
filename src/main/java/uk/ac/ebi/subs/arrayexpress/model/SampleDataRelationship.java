package uk.ac.ebi.subs.arrayexpress.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import uk.ac.ebi.subs.data.component.SampleUse;
import uk.ac.ebi.subs.data.submittable.Assay;
import uk.ac.ebi.subs.data.submittable.AssayData;

import java.util.List;

@CompoundIndexes({
        @CompoundIndex(name = "sample_use_ref_accession", def = "{ 'sampleUses.sampleRef.accession': 1}"),
})
public class SampleDataRelationship {
    @Id
    String id;

    Assay assay;
    List<SampleUse> sampleUses;
    List<AssayData> assayData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Assay getAssay() {
        return assay;
    }

    public void setAssay(Assay assay) {
        this.assay = assay;
    }

    public List<SampleUse> getSampleUses() {
        return sampleUses;
    }

    public void setSampleUses(List<SampleUse> sampleUses) {
        this.sampleUses = sampleUses;
    }

    public List<AssayData> getAssayData() {
        return assayData;
    }

    public void setAssayData(List<AssayData> assayData) {
        this.assayData = assayData;
    }
}
