package edu.uncc.grid.pgaf.datamodules;

import java.io.Serializable;
import java.util.List;

public interface AllToAllData extends Serializable {
	public void setSyncDataList(List<Serializable> dat);
	public Serializable getSyncData();
}
