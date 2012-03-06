package edu.uncc.grid.seeds.comm.dependency;

import java.io.Serializable;

/**
 * Use this interface to handle split and coalesce actions on Dependency Packets
 * The Dependency class will require a class implementing this interface.
 * @author jfvillal
 *
 */
public interface SplitCoalesceHandler {
	/**
	 * Called when a packet needs to be split to be send on multiple
	 * sibling dependency streams
	 * @param packet the parent packet to be split into multiple sibling packets
	 * @return an array of sibling packets.
	 */
	public Serializable[] onSplit( Serializable packet);
	/**
	 * Called when multiple sibling packets need to be coalesce into
	 * the parent packet.
	 * @param pakets the sibling packets
	 * @return the integrated parent packet
	 */
	public Serializable onCoalesce(Serializable pakets[] );
}
