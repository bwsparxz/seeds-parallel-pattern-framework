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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This sample application shows how to load a class during runtime when we don't know 
 * what is the name of the class.
 * the use has to supply the name of the class.
 * @author jfvillal
 *
 */

public class TestClassLoader {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		try {
			Class cls = loader.loadClass("edu.uncc.grid.pgaf.test.SampleClass");
			
			
			Method meth = cls.getMethod("printSomething", (Class[])null);
			
			Object obj = cls.newInstance();
			
			
			
			//SampleInterface inter = (SampleInterface) obj;
			
			//inter.printSomething();
			meth.invoke(obj, null);
			
			//SampleInterfaceClass caster = new SampleInterfaceClass();
			//SampleInterface sample_interface = (SampleInterface) obj.cast(caster);
			//sample_interface.printSomething();
			System.out.println("DONE");
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	public interface SampleInterface{
		public void printSomething();
	}
	public static class SampleInterfaceClass implements SampleInterface{
		
		public void printSomething() {
			System.out.println("hello");
			
		}
		
	}
}
