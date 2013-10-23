package net.edencampo.versionignore;

public class VersionIgnoreLogger
{
	VersionIgnore plugin;
		
	public VersionIgnoreLogger(VersionIgnore instance)
	{
		plugin = instance;
	}
		
	public void logSevereError(String msg)
	{
		plugin.getLogger().severe(msg);
	}
		
	public void logWarning(String msg)
	{
		plugin.getLogger().warning(msg);
	}
		
	public void logInfo(String msg)
	{	
		plugin.getLogger().info(msg);
	}
	
	public void logDebug(String msg)
	{
		if(plugin.getConfig().getString("enableDebug").equalsIgnoreCase("true") || (plugin.getConfig().getString("enableDebug").equalsIgnoreCase("yes")))
		{
			plugin.getLogger().info("Debug: " + msg);	
		}
	}
}
