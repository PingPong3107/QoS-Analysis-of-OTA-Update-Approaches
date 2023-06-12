package com.ota.update.repos;

import java.util.List;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.ota.update.dal.GroupRepoDAL;
import com.ota.update.entities.CarGroup;

/**
 * A repository which uses the mongotemplate to connect with the database.
 */
@Repository
public class GroupRepo implements GroupRepoDAL{

	private final MongoTemplate mongoTemplate;


	@Autowired
	public GroupRepo(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public CarGroup saveCarGroup(CarGroup group) {
		return mongoTemplate.save(group);
	}

	@Override
	public List<CarGroup> getAllGroups() {
		return mongoTemplate.findAll(CarGroup.class);
	}

	@Override
	public CarGroup findById(int id) {
		return mongoTemplate.findById(id, CarGroup.class);
	}

	@Override
	public CarGroup updateCarGroup(CarGroup group) {
		return mongoTemplate.save(group);
	}

	@Override
	public void deleteCarGroup(CarGroup group) {
		mongoTemplate.remove(group);
	}

	@Override
	public void deleteAll() {
		mongoTemplate.dropCollection(CarGroup.class);		
	}

	
}
