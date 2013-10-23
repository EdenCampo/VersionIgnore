package net.edencampo.versionignore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class VersionIgnoreGhost implements Listener
{
	VersionIgnore plugin;
	
	public VersionIgnoreGhost(VersionIgnore instance)
	{
		plugin = instance;
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e)
	{
		String name = e.getPlayer().getName();
		
		if(plugin.ghosts.contains(name))
		{
			int id = 0;
			while(id < plugin.ghosts.size())
			{
				if(plugin.ghosts.get(id).equalsIgnoreCase(name))
				{
					plugin.ghosts.remove(id);
				}
			}
		}
	}
}
