Seeds is a framework to run programs faster using parallel programming.  The framework can be run on a single node with multiple cores, or on a cluster.  Its main use was as a laboratory to test parallel programming and Grid computing ideas.

The cluster middleware modudule include Ssh and Globus (http://www.globus.org/).

The framework self-deploys to a cluster of computers using the mentioned middleware modules

The implemented patterns include the workpool, the pipeline, a 5-point stencil and an all-to-all pattern.

The framework is written in Java programming language.  Its network back-end is done using Sun's JXTA standard (http://java.net/projects/jxta-jxse).  The high-performance connections are done using Java Sockets.

Publications:

B. Wilkinson, J. Villalobos and C. Ferner, "Pattern Programming Approach for Teaching
Parallel and Distributed Computing," SIGCSE 2013 Technical Symposium on Computer
Science Education, March 6-9, 2013, Denver, USA.

Villalobos, Jeremy. Wilkinson, Anthony. "Using Hierarchical Dependency Data Flows to Enable Dynamic Scalability on Parallel Patterns," IEEE International Symposium on Parallel and Distributed Processing Workshop (IPDPS), June 2011.

Villalobos, Jeremy (2011). Running Parallel Applications on a Heterogeneous Environment with Accessible Development Practices and Automatic Scalability. University of North Carolina Charlotte. 2011

Villalobos, Jeremy; Wilkinson, Anthony. "Skeleton/Pattern Programming with an Adder Operator for Grid and Cloud Platforms," Proceedings of the 2010 International Conference on Grid Computing & Applications, GCA 2010, July 2010.