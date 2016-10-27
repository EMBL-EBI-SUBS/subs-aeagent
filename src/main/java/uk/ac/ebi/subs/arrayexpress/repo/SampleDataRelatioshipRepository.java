package uk.ac.ebi.subs.arrayexpress.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.subs.arrayexpress.model.SampleDataRelationship;

public interface SampleDataRelatioshipRepository extends MongoRepository<SampleDataRelationship, String> {
}
