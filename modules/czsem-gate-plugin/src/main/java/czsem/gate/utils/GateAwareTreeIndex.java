package czsem.gate.utils;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import czsem.fs.TreeIndex;
import czsem.fs.depcfg.DependencySource;


public class GateAwareTreeIndex extends TreeIndex
{
	
	protected void addDependency(Annotation a)
	{
		Integer[] dep = GateUtils.decodeEdge(a);
		addDependency(dep[0], dep[1], a.getType());
	}; 
	
	protected void addTokenDpendency(Annotation a, String feature_name)
	{
		Integer child = (Integer) a.getFeatures().get(feature_name);
		if (child == null) return;
		addDependency(a.getId(), child, a.getType()+'.'+feature_name);		
	}; 

	public void addDependecies(AnnotationSet dependenciesAS)
	{
		for (Annotation dep : dependenciesAS)
		{
			addDependency(dep);
		}							
	}
	
	public void addTokenDependecies(AnnotationSet tokenAS, String feature_name)
	{
		for (Annotation toc : tokenAS)
		{
			addTokenDpendency(toc, feature_name);
		}							
	}
	
	/** <b>Not including root!!!</b> **/
	public List<Annotation> getAllCildrenAnnotations(AnnotationSet annotations)
	{
		List<Annotation> ret = new ArrayList<Annotation>();
		
		for (Set<Integer> children_ids : childIndex.values())
		{
			for (Integer id : children_ids)
			{
				ret.add(annotations.get(id));
			}			
		}

		return ret;
	}

	
	public GateAwareTreeIndex() {}

	public GateAwareTreeIndex (AnnotationSet dependencyAnnotatons)
	{
		addDependecies(dependencyAnnotatons);							
	}


	public void addDependencies(DependencySource src, Document doc) {
		src.addDependenciesToIndex(doc, this);
	}

	public void addDependency(Annotation parentAnn, Annotation childAnn, String dependencyType) {
		addDependency(parentAnn.getId(), childAnn.getId(), dependencyType);
	}


}
