package uk.ac.ebi.subs.arrayexpress.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.ac.ebi.subs.arrayexpress.model.SampleDataRelationship;

public interface SampleDataRelatioshipRepository extends MongoRepository<SampleDataRelationship, String> {


    @Query(value="{ assay.sampleUses.sampleRef.accession: ?0 }")
    Page<SampleDataRelationship> findBySampleAccession(String sampleAccession, Pageable pageable);
}
