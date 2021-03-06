package czsem.gate.treex.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import czsem.gate.treex.TreexConfig;
import czsem.utils.AbstractConfig.ConfigLoadException;


public class TreexCloudFactory {
	private static final Logger logger = LoggerFactory.getLogger(TreexCloudFactory.class);
	
	private static volatile TreexCloudFactoryInterface instance;

	public static synchronized TreexCloudFactoryInterface getInstance() {
		if (instance == null) {
			try {
				TreexLocalAnalyserFactory f = new TreexLocalAnalyserFactory();
				String[] cmds = TreexConfig.getConfig().getTreexCloudFactoryCommands();
				if (cmds != null)
					f.setCmdArray(cmds);
						
				instance = f;
				
				logger.info("TreexCloudFactory loaded with cmd array {}", (Object) f.getCmdArray());
			} catch (ConfigLoadException e) {
				throw new RuntimeException(e);
			}
		}
		
		return instance;
	}

	public static synchronized void setInstance(TreexCloudFactoryInterface instance) {
		TreexCloudFactory.instance = instance;
	}


}
