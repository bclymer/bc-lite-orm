package com.bclymer.bcliteorm;

public interface BcOrmChangeListener {

	public void inserted(Object inserted);
	public void updated(Object updated);
	public void insertedOrUpdated(Object insertedOrUpdated);
	public void deleted(Object deleted);
	
}
