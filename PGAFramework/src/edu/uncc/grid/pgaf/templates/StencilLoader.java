package edu.uncc.grid.pgaf.templates;

import java.io.Serializable;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.pgaf.interfaces.basic.Stencil;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;
/**
 * This class will load the Stencil template.  Because the stencil does not have a 
 * data flow requirement, the advanced user can decide to set the Stencil Diffuse
 * and Gather functions in this class, and turning the option of setting a 
 * source-sink on the LoaderTemplate to false.  As done in this method's
 * instantiateSourceSink().
 * 
 * The advance user should not need to modify the PatternLoaderTemplate.  
 * 
 * @author jfvillal
 *
 */
public class StencilLoader extends PatternLoader {
	
	@Override
	public DataMap DiffuseDataUnit(int segment) throws SegmentNotStickingException {
		
		StencilData data = ((Stencil) this.OTemplate.getUserModule() ).DiffuseData(segment );
		DataContainer container = new DataContainer(segment, data);
		
		DataMap<String, Serializable> m = new DataMap<String, Serializable>();
		m.put(PatternLoader.INIT_DATA, container);
		return m;
	}

	/**
	 * This GatherDataUnit calls the Gather method from the Stencil Pattern.
	 */
	@Override
	public void GatherDataUnit(int segment, Data dat) {
		Stencil stc = (Stencil) this.OTemplate.getUserModule() ;
		DataMap<String, Serializable> m = (DataMap<String,Serializable>) dat;
		DataContainer container = (DataContainer) m.get(PatternLoader.INIT_DATA);
		stc.GatherData(segment, (StencilData)container.getPayload() );
	}

	@Override
	public int getDataUnitCount() {
		Stencil s = (Stencil) this.OTemplate.getUserModule();
		return s.getCellCount();
	}

	@Override
	public void initializeModule(String[] args) {
		// Not needed right now
	}

	@Override
	public boolean instantiateSourceSink() {
		return false; //no thanks, I'll used the deploy pattern to
			//instantiate my sink and source
	}

	@Override
	public boolean isOperator() {
		return false;
	}

}
