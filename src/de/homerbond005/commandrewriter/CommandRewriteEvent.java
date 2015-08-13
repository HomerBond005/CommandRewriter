package de.homerbond005.commandrewriter;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandRewriteEvent extends Event implements Cancellable {

	private final PlayerCommandPreprocessEvent commandPreprocessEvent;
	private final String fullIssuedCommand;
	private final String rewriteTrigger;
	private List<String> messageToSend;
	private boolean cancelled;
	private CommandRewriteEvent.Unsafe unsafe = new CommandRewriteEvent.Unsafe();

	public CommandRewriteEvent(PlayerCommandPreprocessEvent commandPreprocessEvent, String fullIssuedCommand, String rewriteTrigger, List<String> messageToSend) {
		this.commandPreprocessEvent = commandPreprocessEvent;
		this.fullIssuedCommand = fullIssuedCommand;
		this.rewriteTrigger = rewriteTrigger;
		this.messageToSend = messageToSend;
	}

	/**
	 * optain unsafe methods of this event
	 */
	public CommandRewriteEvent.Unsafe unsafe() {
		return unsafe;
	}

	public class Unsafe {
		public PlayerCommandPreprocessEvent getCommandPreprocessEvent() {
			return commandPreprocessEvent;
		}
	}

	public String getFullIssuedCommand() {
		return fullIssuedCommand;
	}

	public String getRewriteTrigger() {
		return rewriteTrigger;
	}

	/**
	 * @return A (mutable) copy of the message which will be sent.
	 */
	public List<String> getMessageToSend() {
		return new ArrayList<>(messageToSend);
	}

	/**
	 *
	 * @param messageToSend The message which should be send. List entries are the lines. Set to {@code null} if you want to suppress the message.
	 */
	public void setMessageToSend(List<String> messageToSend) {
		this.messageToSend = messageToSend;
	}

	/**
	 * @see #setMessageToSend(List)
	 */
	public void setMessageToSend(String... messageToSend) {
		this.messageToSend = Arrays.asList(messageToSend);
	}

	/**
	 * @see #setCancelled(boolean)
	 * @return whether command rewriting is cancelled
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Whether to cancel the command rewriting.
	 * Not cancelled will do this:
	 * <ul>
	 *     <li>Cancel the {@link org.bukkit.event.player.PlayerCommandPreprocessEvent}</li>
	 *     <li>Sending the message optainable via {@link #getMessageToSend()} to the player</li>
	 * </ul>
	 * Cancelled will do this:
	 * <ul>
	 *     <li>Not changing the state of the {@link org.bukkit.event.player.PlayerCommandPreprocessEvent}</li>
	 *     <li>Not sending any message to the player.</li>
	 * </ul>
	 * @param cancelled
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	//////////////////////// Needed for custom events ////////////////////////

	private static HandlerList handlerList = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	public static HandlerList getHandlerList() {
		return handlerList;
	}
}
