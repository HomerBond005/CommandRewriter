package de.homerbond005.commandrewriter;

import de.homerbond005.commandrewriter.lib.org.mcstats.Metrics;
import de.homerbond005.commandrewriter.lib.org.mcstats.Metrics.Graph;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class CRPlugin extends JavaPlugin implements Listener {
	private Logger log;
	private Map<String, String> commands;
	private Map<UUID, String> creators;

	@Override
	public void onEnable() {
		log = getLogger();
		creators = new HashMap<>();
		reload();
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("cr")) {
			if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
				sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "CommandRewriter: Help");
				sender.sendMessage(ChatColor.GOLD + "/cr set <command>" + ChatColor.GRAY + " Start the rewrite assistent to the given command.");
				sender.sendMessage(ChatColor.GOLD + "/cr list" + ChatColor.GRAY + " List all set commands");
				sender.sendMessage(ChatColor.GOLD + "/cr remove <command>" + ChatColor.GRAY + "Unassign a text from a command.");
				sender.sendMessage(ChatColor.GOLD + "/cr reload" + ChatColor.GRAY + "Reload the config.");
				sender.sendMessage(ChatColor.GRAY + "You can use color codes like " + ChatColor.GOLD + "&6" + ChatColor.GRAY + " in the texts.");
				sender.sendMessage(ChatColor.GRAY + "The symbol " + ChatColor.GOLD + "|" + ChatColor.GRAY + " will be parsed as new line.");
			} else {
				if (args[0].equalsIgnoreCase("set")) {
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "Use this command in-game!");
						return true;
					}
					Player plr = (Player) sender;
					if (sender.hasPermission("CommandRewriter.set")) {
						if (args.length >= 2) {
							String com = "";
							for (int i = 1; i < args.length; i++) {
								com += args[i] + " ";
							}
							creators.put(plr.getUniqueId(), com.trim().toLowerCase());
							sender.sendMessage(ChatColor.GREEN + "Now type the message that should be assigned to the command.");
							sender.sendMessage(ChatColor.GREEN + "Type !abort to abort");
						} else
							sender.sendMessage(ChatColor.RED + "Use: /cr set <command>");
					} else
						sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
				} else if (args[0].equalsIgnoreCase("list")) {
					if (sender.hasPermission("CommandRewriter.list")) {
						sender.sendMessage(ChatColor.GRAY + "The following messages are assigned:");
						for (Entry<String, String> entry : commands.entrySet()) {
							sender.sendMessage(ChatColor.GOLD + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + colorCodes(entry.getValue()));
						}
					} else
						sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
				} else if (args[0].equalsIgnoreCase("remove")) {
					if (sender.hasPermission("CommandRewriter.remove")) {
						if (args.length >= 2) {
							String com = "";
							for (int i = 1; i < args.length; i++) {
								com += args[i] + " ";
							}
							com = com.trim();
							if (commands.containsKey(com.toLowerCase())) {
								commands.remove(com.toLowerCase());
								getConfig().set("Commands." + com, null);
								saveConfig();
								sender.sendMessage(ChatColor.GREEN + "Successfully remove the command '" + com + "' from the CommandRewriter list.");
							} else
								sender.sendMessage(ChatColor.RED + "The command '" + args[1] + "' is not used in CommandRewriter!");
						} else
							sender.sendMessage(ChatColor.RED + "Use: /cr remove <command>");
					} else
						sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
				} else if (args[0].equalsIgnoreCase("reload")) {
					if (sender.hasPermission("CommandRewriter.reload")) {
						reload();
						log.info("has been reloaded.");
						sender.sendMessage(ChatColor.GREEN + "CommandRewriter has been successfully reloaded.");
					} else
						sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
				} else {
					sender.sendMessage(ChatColor.RED + "See /cr help for help.");
				}
			}
		} else {
			sender.sendMessage("[CommandRewriter] Unknown Command " + command.getName() + " should be handled by me (says bukkit / plugin yml)!");
		}
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if (creators.containsKey(uuid)) {
			if (event.getMessage().equalsIgnoreCase("!abort")) {
				player.sendMessage(ChatColor.RED + "You have aborted the CommandRewriter assistent.");
			} else {
				String command = creators.get(uuid).toLowerCase();
				String message = event.getMessage();
				if (commands.containsKey(command)) {
					player.sendMessage(ChatColor.RED + "The command '" + command + "' is already rewritten.");
					player.sendMessage(ChatColor.RED + "The value text will be overwritten with your one.");
				}
				commands.put(command, message);
				getConfig().set("Commands." + command, message);
				saveConfig();
				player.sendMessage(ChatColor.GREEN + "Successfully assigned the text to the command '" + command + "'.");
			}
			creators.remove(uuid);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String standardizedMessage = event.getMessage().trim().toLowerCase();
		if (standardizedMessage.startsWith("/")) {
			standardizedMessage = standardizedMessage.substring(1);
		}
		String[] parts = standardizedMessage.split(" ");
		String matching = "";
		for (int i = parts.length; i > 0; i--) {
			String check = "";
			for (int w = 0; w < i; w++) {
				check += parts[w] + " ";
			}
			check = check.trim();
			if (commands.containsKey(check)) {
				matching = check;
				break;
			}
		}
		if (!matching.isEmpty()) {
			List<String> msgs = Arrays.asList(commands.get(matching).split("\\|"));
			CommandRewriteEvent evt = new CommandRewriteEvent(event, event.getMessage(), matching, msgs);
			getServer().getPluginManager().callEvent(evt);
			if (evt.isCancelled()) {
				return;
			}
			msgs = evt.getMessageToSend();
			if (msgs != null) {
				for (String line : msgs) {
					event.getPlayer().sendMessage(colorCodes(line));
				}
			}
			event.setCancelled(true);
		}
	}

	private void reload() {
		reloadConfig();
		getConfig().addDefault("Commands", new HashMap<String, String>());
		getConfig().options().copyDefaults(true);
		saveConfig();
		commands = new HashMap<>();
		ConfigurationSection commandsCfgSection = getConfig().getConfigurationSection("Commands");
		Set<String> commandSet = commandsCfgSection.getKeys(false);
		for (String command : commandSet) {
			commands.put(command.toLowerCase(), commandsCfgSection.getString(command));
		}
		try {
			Metrics metrics = new Metrics(this);
			Graph graphabbr = metrics.createGraph("Defined texts");
			graphabbr.addPlotter(new Metrics.Plotter(Integer.toString(commands.size())) {
				@Override
				public int getValue() {
					return 1;
				}

			});
			metrics.start();
		} catch (IOException ex) {
			log.warning("Error while enabling Metrics: " + ex);
			ex.printStackTrace();
		}
	}

	private static String colorCodes(String inputMsg) {
		return ChatColor.translateAlternateColorCodes('&', inputMsg);
	}
}
