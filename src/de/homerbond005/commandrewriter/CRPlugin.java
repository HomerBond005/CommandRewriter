package de.homerbond005.commandrewriter;

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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.homerbond005.commandrewriter.Metrics.Graph;

public class CRPlugin extends JavaPlugin implements Listener {
	private Logger log;
	private HashMap<String, String> commands;
	private Metrics metrics;
	private HashMap<Player, String> creators;

	@Override
	public void onEnable() {
		log = getLogger();
		creators = new HashMap<Player, String>();
		reload();
		getServer().getPluginManager().registerEvents(this, this);
		log.info("is enabled.");
	}

	@Override
	public void onDisable() {
		log.info("is disabled.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equalsIgnoreCase("cr")) {
			Player player;
			if (sender instanceof Player)
				player = (Player) sender;
			else {
				sender.sendMessage("Use this command in-game!");
				return true;
			}
			if (args.length == 0)
				args = new String[] { "help" };
			if (args[0].equalsIgnoreCase("help")) {
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "CommandRewriter: Help");
				player.sendMessage(ChatColor.GOLD + "/cr set <command>" + ChatColor.GRAY + " Start the rewrite assistent to the given command.");
				player.sendMessage(ChatColor.GOLD + "/cr list" + ChatColor.GRAY + " List all set commands");
				player.sendMessage(ChatColor.GOLD + "/cr remove <command>" + ChatColor.GRAY + "Unassign a text from a command.");
				player.sendMessage(ChatColor.GOLD + "/cr reload" + ChatColor.GRAY + "Reload the config.");
				player.sendMessage(ChatColor.GRAY + "You can use color codes like " + ChatColor.GOLD + "&6" + ChatColor.GRAY + " in the texts.");
				player.sendMessage(ChatColor.GRAY + "The symbol " + ChatColor.GOLD + "|" + ChatColor.GRAY + " will be parsed as new line.");
			} else if (args[0].equalsIgnoreCase("set")) {
				if (player.hasPermission("CommandRewriter.set")) {
					if (args.length >= 2) {
						String com = "";
						for (int i = 1; i < args.length; i++) {
							com += args[i] + " ";
						}
						creators.put(player, com.trim().toLowerCase());
						player.sendMessage(ChatColor.GREEN + "Now type the message that should be assigned to the command.");
						player.sendMessage(ChatColor.GREEN + "Type !abort to abort");
					} else
						player.sendMessage(ChatColor.RED + "Use: /cr set <command>");
				} else
					player.sendMessage(ChatColor.RED + "You do not have the required permission!");
			} else if (args[0].equalsIgnoreCase("list")) {
				if (player.hasPermission("CommandRewriter.list")) {
					player.sendMessage(ChatColor.GRAY + "The following messages are assigned:");
					for (Entry<String, String> entry : commands.entrySet()) {
						player.sendMessage(ChatColor.GOLD + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + fm(entry.getValue()));
					}
				} else
					player.sendMessage(ChatColor.RED + "You do not have the required permission!");
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (player.hasPermission("CommandRewriter.remove")) {
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
							player.sendMessage(ChatColor.GREEN + "Successfully remove the command '" + com + "' from the CommandRewriter list.");
						} else
							player.sendMessage(ChatColor.RED + "The command '" + args[1] + "' is not used in CommandRewriter!");
					} else
						player.sendMessage(ChatColor.RED + "Use: /cr remove <command>");
				} else
					player.sendMessage(ChatColor.RED + "You do not have the required permission!");
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (player.hasPermission("CommandRewriter.reload")) {
					reload();
					log.info("has been reloaded.");
					player.sendMessage(ChatColor.GREEN + "CommandRewriter has been successfully reloaded.");
				} else
					player.sendMessage(ChatColor.RED + "You do not have the required permission!");
			} else {
				player.sendMessage(ChatColor.RED + "See /cr help for help.");
			}
		}
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (creators.containsKey(player)) {
			if (event.getMessage().equalsIgnoreCase("!abort")) {
				player.sendMessage(ChatColor.RED + "You have aborted the CommandRewriter assistent.");
			} else {
				String command = creators.get(player).toLowerCase();
				String message = event.getMessage();
				if (commands.containsKey(command)) {
					player.sendMessage(ChatColor.RED + "The command '" + command + "' is already rewritten.");
					player.sendMessage(ChatColor.RED + "The value text will be overwritten with your one.");
				}
				commands.put(command, message);
				getConfig().set("Commands." + (command.replace(' ', ' ')), message);
				saveConfig();
				player.sendMessage(ChatColor.GREEN + "Successfully assigned the text to the command '" + command + "'.");
			}
			creators.remove(player);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String[] parts = event.getMessage().trim().replaceFirst("/", "").toLowerCase().split(" ");
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
		if (!matching.equals("")) {
			for (String line : commands.get(matching).split("\\|"))
				event.getPlayer().sendMessage(fm(line));
			event.setCancelled(true);
		}
	}

	private void reload() {
		reloadConfig();
		getConfig().addDefault("Commands", new HashMap<String, String>());
		getConfig().options().copyDefaults(true);
		saveConfig();
		reloadConfig();
		commands = new HashMap<String, String>();
		Set<String> commandset = getConfig().getConfigurationSection("Commands").getKeys(false);
		for (String command : commandset) {
			commands.put(command.toLowerCase(), getConfig().getString("Commands." + command));
		}
		try {
			metrics = new Metrics(this);
			Graph graphabbr = metrics.createGraph("Defined texts");
			graphabbr.addPlotter(new Metrics.Plotter("" + commands.size()) {
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

	private String fm(String t) {
		return ChatColor.translateAlternateColorCodes('&', t);
	}
}
