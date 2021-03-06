package czsem.gate.utils;

import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import czsem.gate.plugins.LearningEvaluator;

public abstract class PRSetup
{
	public static class SinglePRSetup extends PRSetup 
	{
		private String pr_className;
		private FeatureMap fm;
		private String name = null;
	
		public SinglePRSetup(String className, String name)
		{
			pr_className = className;
			fm = Factory.newFeatureMap();
			this.name = name;
			
		}

		public SinglePRSetup(Class<?> cl, String name)
		{
			this(cl.getCanonicalName(), name);			
		}

		public SinglePRSetup(Class<?> cl)
		{
			this(cl, null);
		}

		public SinglePRSetup(String classneme)
		{
			this(classneme, null);
		}
				
		public SinglePRSetup putFeature(Object key, Object value)
		{
			fm.put(key, value);
			return this;
		}
		
		public SinglePRSetup putFeatureObjList(Object key, Object ... obj_list) {
			if (obj_list == null)
				fm.put(key, null);
			else
				fm.put(key, Arrays.asList(obj_list));
			return this;
		}
		
		public SinglePRSetup putFeatureList(Object key, String ... strig_list) {
			return putFeatureObjList(key, (Object[]) strig_list);
		}
	
		public ProcessingResource createPR() throws ResourceInstantiationException
		{
			return(ProcessingResource) Factory.createResource(pr_className, fm, null, name);			
		}				
	}
	
	public static class MLEvaluateSetup extends SinglePRSetup
	{

		private List<LearningEvaluator> evaluation_register;

		public MLEvaluateSetup(List<LearningEvaluator> evaluation_register)
		{
			super(LearningEvaluator.class);
			this.evaluation_register = evaluation_register;
		}

		@Override
		public ProcessingResource createPR() throws ResourceInstantiationException
		{
			ProcessingResource ret = super.createPR();
			evaluation_register.add((LearningEvaluator) ret);
			return ret;
		}

	}


	public abstract ProcessingResource createPR() throws ResourceInstantiationException;

	public static SerialAnalyserController buildGatePipeline(PRSetup [] prs, String name) throws ResourceInstantiationException	{
		return buildGatePipeline(Arrays.asList(prs), name);
	}

	public static SerialAnalyserController buildGatePipeline(String name, PRSetup ... prs) throws ResourceInstantiationException	{
		return buildGatePipeline(Arrays.asList(prs), name);
	}

	public static void execGatePipeline(PRSetup [] prs, String name, String docContent) throws ResourceInstantiationException, ExecutionException {
		Document doc = Factory.newDocument(docContent);
		execGatePipeline(prs, name, doc);
		Factory.deleteResource(doc);
	}

	public static void execGatePipeline(PRSetup [] prs, String name, Document doc) throws ResourceInstantiationException, ExecutionException {
		SerialAnalyserController pipe = buildGatePipeline(Arrays.asList(prs), name);
		execGatePipeline(pipe, name, doc);
		GateUtils.deepDeleteController(pipe);
	}

	public static void execGatePipeline(SerialAnalyserController pipe, String name, Document doc) throws ResourceInstantiationException, ExecutionException {
		Corpus corpus = Factory.newCorpus("corpus_for_"+name);
		pipe.setCorpus(corpus);
		corpus.add(doc);
		pipe.execute();
		corpus.clear();
		Factory.deleteResource(corpus);
	}

	public static SerialAnalyserController buildGatePipeline(List<PRSetup> prs, String name) throws ResourceInstantiationException
	{
		try {
			URL res = NotCheckingParametersSerialController.class.getResource(
					"/gate/creole/CreoleRegisterImpl.class");
			
			if (res != null)
				GateUtils.registerComponentIfNot(NotCheckingParametersSerialController.class);
			else {
				GateUtils.registerPluginDirectory("czsem-gate-plugin");
			}
			
		} catch (GateException | MalformedURLException e1) {
			throw new ResourceInstantiationException(e1);
		}
		
		SerialAnalyserController controller = (SerialAnalyserController) Factory.createResource(
			NotCheckingParametersSerialController.class.getCanonicalName());
		
		controller.setName(name);
	
		
		for (int i = 0; i < prs.size(); i++)
		{
			try {
				controller.add(prs.get(i).createPR());
			} catch (ResourceInstantiationException e) {
				try {
					GateUtils.deepDeleteController(controller);
				} catch (Throwable t) {
					e.addSuppressed(t);
				}
				
				throw e;
			}
		}
				
		return controller;		
	}		
}