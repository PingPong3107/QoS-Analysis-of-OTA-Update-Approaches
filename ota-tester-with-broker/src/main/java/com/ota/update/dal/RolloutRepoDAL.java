package com.ota.update.dal;

import java.util.List;

import com.ota.update.entities.Rollout;

/**
 * Interface used to define the functionality of the rollout repo. Here continuous rollouts are registered.
 */
public interface RolloutRepoDAL {
	Rollout saveRollout(Rollout rollout);

	List<Rollout> getAllRollouts();

	Rollout findById(int id);

	Rollout updateRollout(Rollout rollout);

	void deleteById(int id);

	void deleteAll();
}
