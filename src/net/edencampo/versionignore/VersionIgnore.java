package net.edencampo.versionignore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.StructureModifier;

public class VersionIgnore extends JavaPlugin
{
	ProtocolManager protocolManager;
	
	VersionIgnoreLogger VILog = new VersionIgnoreLogger(this);
	
	public List<String> ghosts = new ArrayList<String>();
	
	public void onEnable()
	{
		CheckUpdate();
		
		try 
		{
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		    
		    VILog.logDebug("Metrics activation success.");
		} 
		catch (IOException e) 
		{
			VILog.logWarning("VersionIgnore failed to start usage tracking :(");
		}
		
		saveDefaultConfig();
		saveConfig();
		
		VILog.logDebug("Configuration saved and reloaded!");
		
		this.protocolManager = ProtocolLibrary.getProtocolManager();
		
		protocolManager.removePacketListeners(this);
		
		startVersionListener();
		startOutgameFixListener();
		
		VILog.logDebug("startVersionListener() and startOutgameFixListener() fired");
		
		Bukkit.getServer().getPluginManager().registerEvents(new VersionIgnoreGhost(this), this);
		
		VILog.logDebug("Events registered!");
		
		VILog.logInfo("Successfully loaded!");
	}
	
	public void onDisable()
	{
		VILog.logInfo("Successfully unloaded!");
	}
	
	protected void CheckUpdate()
	{
		String update = this.getConfig().getString("autoUpdate");
		
		if(update.equalsIgnoreCase("true") || update.equalsIgnoreCase("yes"))
		{
			Updater updater = new Updater(this, "version-ignore", this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);
			
	        Updater.UpdateResult upresult = updater.getResult();
	        
	        switch(upresult)
	        {
	            case SUCCESS:
	            	VILog.logInfo("VersionIgnore will be updated on next reload!");
	                break;
	            case FAIL_DOWNLOAD:
	            	VILog.logInfo("Download Failed: The auto-updater found an update, but was unable to download VersionIgnore.");
	                break;
	            case FAIL_DBO:
	            	VILog.logInfo("dev.bukkit.org Failed: for some reason, the updater was unable to contact DBO to download the file.");
	        }
		}
		else
		{
			VILog.logDebug("Skipped update-checking...");
		}
	}
	
	public void startVersionListener()
	{	
		VILog.logDebug("startVersionListener() fired!");
		
		protocolManager.addPacketListener(new PacketAdapter(this, ConnectionSide.CLIENT_SIDE, ListenerPriority.HIGHEST, GamePhase.LOGIN, Packets.Client.HANDSHAKE) {
		String username = "UNKNOWN";
		
			@Override
			public void onPacketReceiving(PacketEvent event)
			{	
				VILog.logDebug("onPacketReceiving called from startVersionListener()");
				
				if (event.getPacketID() == Packets.Client.HANDSHAKE)
				{
					VILog.logDebug("Recieved a handshake packet from an UNKOWN client");
					
					PacketContainer packet = event.getPacket();
		         
					int MCProtocol = packet.getIntegers().read(0);
					username = packet.getStrings().read(0);
					
					if(getConfig().getInt("server-protocol") == MCProtocol)
					{
						VILog.logDebug(username + " has sent correct MCProtocol version, skipping!");
						
						return;
					}
					
					packet.getIntegers().write(0, getConfig().getInt("server-protocol"));
					
					VILog.logDebug("Wrote field 0 in handshake, set to " + getConfig().getInt("server-protocol"));
					
					if(getConfig().getString("warnPlayers").equalsIgnoreCase("true") || getConfig().getString("warnPlayers").equalsIgnoreCase("yes"))
					{
						VILog.logDebug("Started a runnable for " + username + " to warn him..");
						
						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
						{
							public void run()
							{
								Player p = Bukkit.getPlayerExact(username);
								
								if(p == null)
								{
									VILog.logDebug("For some reason; " + username + " is still null, probably connecting issue?");
									
									return;
								}
								
								p.sendMessage(ChatColor.YELLOW + "Hello, " + username + "!");
								p.sendMessage(ChatColor.DARK_GREEN + "Looks like you have logged in from a version that differs to this servers version, you have been let in because of VersionIgnore!");
								p.sendMessage(ChatColor.DARK_GREEN + "I just wanted to inform you, that crashes and buggy stuff may occur if you keep playing on this version..");
								p.sendMessage(ChatColor.DARK_GREEN + "Be sure to watchout from this kind of stuff!");
								
								VILog.logDebug("Sent warning messages to " + username + "!");
								
								if(getConfig().getString("oldVersion-ghost").equalsIgnoreCase("true"))
								{
									p.sendMessage(ChatColor.DARK_RED + "You are now a ghost!");
									
									if(!ghosts.contains(p.getName()))
									{
										VILog.logDebug("Added " + p.getName() + " to ghosts arraylist");
										ghosts.add(p.getName());
									}
								}
							}
							
						}, 90L);
					}
					
					VILog.logDebug("Finished process for " + username);
		        }
		    }
		});
		
		VILog.logDebug("Successfully enabled version listener!");
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void startOutgameFixListener()
	{
		VILog.logDebug("startOutgameFixListener() fired!");
		
		if(getConfig().getString("serverList-fix").equalsIgnoreCase("true") || getConfig().getString("serverList-fix").equalsIgnoreCase("yes"))
		{
			VILog.logDebug("Registering a packet listener for valuies 255 and 254 (GET_INFO, KICK_DISCONNECT)");
			
			protocolManager.addPacketListener(new PacketAdapter(this, ConnectionSide.BOTH, ListenerPriority.HIGHEST, GamePhase.LOGIN, new Integer[] { Integer.valueOf(255), Integer.valueOf(Packets.Client.GET_INFO) })
			{
				int proto;
				
				@Override
				public void onPacketReceiving(PacketEvent event)
				{
					VILog.logDebug("onPacketReceiving called from startOutgameFixListener()");
					
					try
					{
						PacketContainer packet = event.getPacket();
						
						int clientInfoProto = packet.getIntegers().read(0);
						proto = clientInfoProto;
						VILog.logDebug("Client sent version protocol as " + clientInfoProto + "..");
					}
					catch (Exception localException)
					{
						VILog.logSevereError("ERROR: localException fired from onPacketReceiving (GET_INFO)!");
					}
				}
				
				public void onPacketSending(PacketEvent event)
				{
					VILog.logDebug("onPacketSending called from startOutgameFixListener()");
					
					try
					{
						PacketContainer packet = event.getPacket();

						StructureModifier packetString = packet.getSpecificModifier(String.class);
						
						VILog.logDebug("Current packetString: " + packet.getStrings().read(0));
						
						if(packet.getStrings().read(0).equalsIgnoreCase("Protocol error"))
						{
							VILog.logSevereError("Received protocol error from onPacketSending (KICK_DISCONNECT) at startOutgameFixListener!");
							return;
						}
						
						String fix = packet.getStrings().read(0).replaceFirst(getConfig().getString("server-protocol"), String.valueOf(proto));
						packetString.write(0, fix);
						
						VILog.logDebug("Fixed packetString, should not bug (" + fix + ")!");
					}
					catch (Exception localException)
					{
						VILog.logSevereError("ERROR: localException fired from onPacketSending (KICK_DISCONNECT)!");
					}
			    }
			});
			
			VILog.logDebug("Successfully enabled server-list listener!");
		}
	}
}