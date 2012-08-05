package de.HomerBond005.CommandRewriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import de.HomerBond005.CommandRewriter.Metrics.Graph;

public class CRPlugin extends JavaPlugin implements Listener{
	private PermissionsChecker pc;
	private Logger log;
	private HashMap<String, String> commands;
	private Metrics metrics;
	
	@Override
	public void onEnable(){
		log = getLogger();
		reload();
		getServer().getPluginManager().registerEvents(this, this);
		log.info("is enabled.");
	}
	
	@Override
	public void onDisable(){
		log.info("is disabled.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equalsIgnoreCase("cr")){
			Player player;
			if(sender instanceof Player)
				player = (Player) sender;
			else{
				sender.sendMessage("Use this command in-game!");
				return true;
			}
			if(args.length == 0)
				args = new String[]{"help"};
			if(args[0].equalsIgnoreCase("help")){
				player.sendMessage(ChatColor.GOLD+""+ChatColor.BOLD+"CommandRewriter: Help");
				player.sendMessage(ChatColor.GOLD+"/cr set <command> <text>"+ChatColor.GRAY+" Assign a text to a command.");
				player.sendMessage(ChatColor.GOLD+"/cr list"+ChatColor.GRAY+" List all set commands");
				player.sendMessage(ChatColor.GOLD+"/cr remove <command>"+ChatColor.GRAY+ "Unassign a text from a command.");
				player.sendMessage(ChatColor.GRAY+"You can use color codes like "+ChatColor.GOLD+"&6"+ChatColor.GRAY+" in the texts.");
				player.sendMessage(ChatColor.GRAY+"The symbol "+ChatColor.GOLD+"|"+ChatColor.GRAY+" will be parsed as new line.");
			}else if(args[0].equalsIgnoreCase("set")){
				if(pc.has(player, "CommandRewriter.set")){
					if(args.length >= 3){
						String text = "";
						for(int i = 2; i < args.length; i++){
							text += args[i]+" ";
						}
						getConfig().set("Commands."+args[1], text);
						saveConfig();
						commands.put(args[1].toLowerCase(), text);
						player.sendMessage(ChatColor.GREEN+"Successfully assigned the text to the command '"+args[1]+"'.");
					}else
						player.sendMessage(ChatColor.RED+"Use: /cr set <command> <text>");
				}else
					player.sendMessage(ChatColor.RED+"You do not have the required permission!");
			}else if(args[0].equalsIgnoreCase("list")){
				if(pc.has(player, "CommandRewriter.list")){
					player.sendMessage(ChatColor.GRAY+"The following messages are assigned:");
					for(Entry<String, String> entry : commands.entrySet()){
						player.sendMessage(ChatColor.GOLD+entry.getKey()+ChatColor.GRAY+": "+ChatColor.RESET+fm(entry.getValue()));
					}
				}else
					player.sendMessage(ChatColor.RED+"You do not have the required permission!");
			}else if(args[0].equalsIgnoreCase("remove")){
				if(pc.has(player, "CommandRewriter.remove")){
					if(args.length == 2){
						if(commands.containsKey(args[1].toLowerCase())){
							commands.remove(args[1].toLowerCase());
							getConfig().set("Commands."+args[1], null);
							saveConfig();
							player.sendMessage(ChatColor.GREEN+"Successfully remove the command '"+args[1]+"' from the CommandRewriter list.");
						}else
							player.sendMessage(ChatColor.RED+"The command '"+args[1]+"' is not used in CommandRewriter!");
					}else
						player.sendMessage(ChatColor.RED+"Use: /cr remove <command>");
				}else
					player.sendMessage(ChatColor.RED+"You do not have the required permission!");
			}else if(args[0].equalsIgnoreCase("reload")){
				reload();
				log.info("has been reloaded.");
				player.sendMessage(ChatColor.GREEN+"CommandRewriter has been successfully reloaded.");
			}else{
				player.sendMessage(ChatColor.RED+"See /cr help for help.");
			}
		}
		return true;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event){
		String first = event.getMessage().replaceFirst("/", "").split(" ")[0].toLowerCase();
		if(commands.containsKey(first)){
			for(String line : commands.get(first).split("\\|"))
				event.getPlayer().sendMessage(fm(line));
			event.setCancelled(true);
		}
	}
	
	private void reload(){
		getConfig().addDefault("Commands", new HashMap<String, String>());
		getConfig().options().copyDefaults(true);
		saveConfig();
		reloadConfig();
		commands = new HashMap<String, String>();
		Set<String> commandset = getConfig().getConfigurationSection("Commands").getKeys(false);
		for(String command : commandset){
			commands.put(command.toLowerCase(), getConfig().getString("Commands."+command));
		}
		pc = new PermissionsChecker(this, true);
		try {
			metrics = new Metrics(this);
		    Graph graphabbr = metrics.createGraph("Defined texts");
		    graphabbr.addPlotter(new Metrics.Plotter(""+commands.size()){
		            @Override
		            public int getValue() {
		                    return 1;
		            }

		    });
			metrics.start();
		} catch (IOException e) {
			log.warning("Error while enabling Metrics!");
		}
	}
	
	private String fm(String t){
		return t.replaceAll("(&([a-f0-9]))", "\u00A7$2");
	}
}
