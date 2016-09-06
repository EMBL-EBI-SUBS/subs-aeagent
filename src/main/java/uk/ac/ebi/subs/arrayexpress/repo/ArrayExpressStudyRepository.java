package uk.ac.ebi.subs.arrayexpress.repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.subs.arrayexpress.model.ArrayExpressStudy;

public interface ArrayExpressStudyRepository extends MongoRepository<ArrayExpressStudy,String> {
}