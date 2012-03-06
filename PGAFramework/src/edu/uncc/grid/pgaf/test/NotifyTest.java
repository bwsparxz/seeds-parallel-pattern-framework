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

public class NotifyTest {
		/**
		 * @param args
		 */
		public static void main(String[] args) {
			
			Object obj = new Object();
			Thread t = new Thread( new Producer(obj));
			t.start();
			Thread o = new Thread( new Consumer(obj));
			o.start();
			
			try {
				t.join();
				o.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		public static class Producer implements Runnable{
			Object obj;
			public Producer( Object j ){
				obj = j ;
			}
			@Override
			public void run() {
				
					
				synchronized(obj){
					obj.notify();
				}
			}
			
		}

		
		public static class Consumer implements Runnable{
			Object obj;
			public Consumer( Object j ){
				obj = j ;
			}
			@Override
			public void run() {
				synchronized( obj){
					System.out.println("start waiting");
					try {
						obj.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("the objeect is ready");
				}
			}
			
		}
	
	
	
}
