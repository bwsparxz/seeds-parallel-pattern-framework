/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * This file is part of PGAFramework.
 *
 *   PGAFramework is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PGAFramework is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PGAFramework.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Other libraries and code used.
 *  
 *  This framework also used code and libraries from the Java
 *  Cog Kit project.  http://www.cogkit.org
 *  
 *  This product includes software developed by Sun Microsystems, Inc. 
 *  for JXTA(TM) technology.
 *  
 *  Also, code from UPNPLib is used
 *  
 *  And some extremely modified code from Practical JXTA is used.  
 *  This product includes software developed by DawningStreams, Inc.    
 *  
 */
package edu.uncc.grid.pgaf.test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * 
 * This class shows how to use a BlockingQueue to 
 * create a producer consumer behavior.
 * 
 * This is used to manage communication socket in PGAF
 * @author jfvillal
 *
 */


public class ProducerConsumerExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BlockingQueue<Integer> q = new ArrayBlockingQueue<Integer>(5, true);
		
		Producer pr = new Producer( q );
		Consumer co = new Consumer( q);

		Thread pr_t = new Thread( pr );
		Thread co_t = new Thread( co );
		
		pr_t.start();
		co_t.start();
		
		try {
			pr_t.join();
			co_t.join();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	
	public static class Producer implements Runnable{
		BlockingQueue q;
		public Producer( BlockingQueue _q){
			q = _q;
		}
		@Override
		public void run() {
			for(int i = 0; i < 10; i++){
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
				System.out.println("producer enqueuing " + i );
				try {
					q.put( i );
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
			}
		}
		
	}

	
	public static class Consumer implements Runnable{
		BlockingQueue<Integer> q;
		public Consumer( BlockingQueue<Integer> _q){
			q = _q;
		}
		@Override
		public void run() {
			for( int i = 0; i < 10; i++){
				
				Integer m;
				try {
					m = q.take();
					System.out.println(" got m :  " + m );
				} catch (InterruptedException e) {
				
					e.printStackTrace();
				}
				
				
				
			}
			
		}
		
	}
}
