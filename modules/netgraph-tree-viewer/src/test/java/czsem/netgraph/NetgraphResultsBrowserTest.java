package czsem.netgraph;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.Plugin;
import gate.creole.splitter.SentenceSplitter;
import gate.creole.tokeniser.DefaultTokeniser;

import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import czsem.fs.GateAnnotationsNodeAttributes;
import czsem.fs.query.FSQuery;
import czsem.fs.query.FSQuery.QueryData;
import czsem.fs.query.FSQuery.QueryMatch;
import czsem.gate.utils.GateAwareTreeIndexExtended;
import czsem.gate.utils.GateUtils;
import czsem.gate.utils.PRSetup;
import czsem.gate.utils.PRSetup.SinglePRSetup;
import czsem.netgraph.treesource.TreeSourceWithQueryMatch;


public class NetgraphResultsBrowserTest {
	
	public static void main(String[] args) throws Exception {
		GateUtils.initGateKeepLog();
		GateUtils.registerANNIE();
		Gate.getCreoleRegister().registerPlugin(new Plugin.Maven("uk.ac.gate.plugins", "stanford-corenlp", "8.5.1"));
		
		
		PRSetup[] prs = {
				new SinglePRSetup(DefaultTokeniser.class),
				new SinglePRSetup(SentenceSplitter.class),
				new SinglePRSetup("gate.stanford.Parser"),
		};
		
		Document doc = Factory.newDocument("Bills on ports and immigration were submitted by Senator Brownback, Republican of Kansas. This is the second sentence.");
		PRSetup.execGatePipeline(prs, "NetgraphResultsBrowser", doc);
		
		
		GateAwareTreeIndexExtended index = new GateAwareTreeIndexExtended(doc);
		AnnotationSet as = doc.getAnnotations();
		index.setNodesAS(as);
		index.addDependecies(as.get(null, Collections.singleton("args")));			

		QueryData data = new FSQuery.QueryData(index, new GateAnnotationsNodeAttributes(as));
		
		Iterable<QueryMatch> results = FSQuery.buildQuery("[]([]([]))").evaluate(data);
		
		
		JFrame fr = new JFrame(NetgraphResultsBrowser.class.getName());
	    fr.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	
		
	    TreeSourceWithQueryMatch src = new TreeSourceWithQueryMatch();
		NetgraphResultsBrowser rb = new NetgraphResultsBrowser(src);
		rb.initComponents();

		//rb.asIndexHelper.setSourceAS(as);
		src.setIndexAndUpdateSelection(doc, index);
		rb.setResults(results);
		
		
		//tv.setForest(attrs, fsTree);
	
		fr.add(rb);
		
		fr.pack();
		fr.setVisible(true);
		
	}

}
