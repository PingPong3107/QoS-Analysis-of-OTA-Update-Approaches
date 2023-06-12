package com.ota.update.repos;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.ota.update.dal.DeltaFlagRepoDAL;
import com.ota.update.entities.DeltaFlag;

/**
 * Repository that uses the MongoTemplate to communicate with the database.
 */
@Repository
public class DeltaFlagRepo implements DeltaFlagRepoDAL{

	private final MongoTemplate mongoTemplate;

	public DeltaFlagRepo(MongoTemplate mongoTemplate){
		this.mongoTemplate=mongoTemplate;
	}

	@Override
	public DeltaFlag save(DeltaFlag b) {
		return mongoTemplate.save(b);
	}

	@Override
	public DeltaFlag findById(int id) {
		return mongoTemplate.findById(id, DeltaFlag.class);
	}

	@Override
	public DeltaFlag updateDeltaFlag(DeltaFlag b) {
		return mongoTemplate.save(b);
	}

	@Override
	public void deleteDeltaFlag(DeltaFlag b) {
		mongoTemplate.remove(b);		
	}

	@Override
	public void deleteAll() {
		mongoTemplate.dropCollection(DeltaFlag.class);		
	}

	
	
}
