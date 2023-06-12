package com.ota.update.repos;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.ota.update.dal.RolloutRepoDAL;
import com.ota.update.entities.Rollout;

/**
 * A repository which uses the mongotemplate to connect with the database.
 */
@Repository
public class RolloutRepo implements RolloutRepoDAL {

	private final MongoTemplate mongoTemplate;

	public RolloutRepo(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Rollout saveRollout(Rollout rollout) {
		return mongoTemplate.save(rollout);
	}

	@Override
	public List<Rollout> getAllRollouts() {
		return mongoTemplate.findAll(Rollout.class);
	}

	@Override
	public Rollout findById(int id) {
		return mongoTemplate.findById(id, Rollout.class);
	}

	@Override
	public Rollout updateRollout(Rollout rollout) {
		return mongoTemplate.save(rollout);
	}

	@Override
	public void deleteAll() {
		mongoTemplate.dropCollection(Rollout.class);
	}

	@Override
	public void deleteById(int groupId) {
		Query query= new Query();
		query.addCriteria(Criteria.where("groupId").is(String.valueOf(groupId)));
		mongoTemplate.remove(query, Rollout.class);
	}

}
