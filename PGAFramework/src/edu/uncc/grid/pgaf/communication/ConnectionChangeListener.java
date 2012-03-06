package edu.uncc.grid.pgaf.communication;

/**
 * 
 * This class's method is called when when a connection change event happens.  This is mainly design
 * to allow one of the dataflow perceptrons to migrate, or to split its communication line.
 * 
 * @author jfvillal
 *
 */
public interface ConnectionChangeListener {
	public void onHibernateConnection(ConnectionChangedMessage message);
}
