package com.ota.update.dal;

import com.ota.update.entities.DeltaFlag;

/**
 * Interface defining the offered functionality of the repository used for the global delta mode.
 */
public interface DeltaRepoDAL {
	DeltaFlag save(DeltaFlag b);

	DeltaFlag findById(int id);

	DeltaFlag updateDeltaFlag(DeltaFlag b);

	void deleteDeltaFlag(DeltaFlag b);

	void deleteAll();
}
