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

public class ThreadReview {

	/**
	 * @param args
	 */
	public void  method_this(){
		for(int i = 0; i < 10 ; i++){
			Thread t = new Thread( new ThreadPractice(this) , "thread");
			t.start();
		}
	}
	public static void main(String[] args) {
		ThreadReview rev = new ThreadReview();
		rev.method_this();
		
	}
	public static class ThreadPractice implements Runnable{
		ThreadReview rev;
		public ThreadPractice( ThreadReview r){
			rev = r;
		}
		public void run() {
			// TODO Auto-generated method stub
			try {
				rev.something();
				Thread.sleep(400);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("done");
		}
		
	}
	public void something(){
		
	}
}
