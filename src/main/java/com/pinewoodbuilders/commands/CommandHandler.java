/*
 * Copyright (c) 2018.
 *
 * This file is part of Xeus.
 *
 * Xeus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeus.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.pinewoodbuilders.commands;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.Constants;
import com.pinewoodbuilders.commands.system.JSONCmdMapCommand;
import com.pinewoodbuilders.contracts.commands.Command;
import com.pinewoodbuilders.contracts.commands.CommandSource;
import com.pinewoodbuilders.database.controllers.GuildController;
import com.pinewoodbuilders.database.transformers.GuildTransformer;
import com.pinewoodbuilders.exceptions.InvalidCommandPrefixException;
import com.pinewoodbuilders.exceptions.MissingCommandDescriptionException;
import com.pinewoodbuilders.metrics.Metrics;
import com.pinewoodbuilders.middleware.MiddlewareHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.internal.utils.Checks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class CommandHandler {

    private static final Set<CommandContainer> COMMANDS = new HashSet<>();

    /**
     * Get command container from the given command instance.
     *
     * @param command The command instance.
     * @return Possibly-null, The registered command container instance.
     */
    public static CommandContainer getCommand(Command command) {
        for (CommandContainer container : COMMANDS) {
            if (container.getCommand().isSame(command)) {
                return container;
            }
        }

        return null;
    }

    /**
     * Get the command container from the given command class instance.
     *
     * @param command The command class instance.
     * @return Possibly-null, The registered command container instance.
     */
    public static CommandContainer getCommand(@Nonnull Class<? extends Command> command) {
        for (CommandContainer container : COMMANDS) {
            if (container.getCommand().getClass().getTypeName().equals(command.getTypeName())) {
                return container;
            }
        }
        return null;
    }

    /**
     * Get the command matching the message raw contents first argument, both
     * the command prefix and the command trigger must match for the command
     * to be returned, if the guild/server that the command was executed
     * in has a custom prefix set the custom prefix will be used to
     * match the command instead.
     * <p>
     * If a commands priority is set to {@link CommandPriority#IGNORED}
     * the command will be omitted from the search.
     *
     * @param message The JDA message object for the current message.
     * @return Possibly-null, The command matching the given command with the highest priority.
     */
    public static CommandContainer getCommand(Message message) {
        return getCommand(message, message.getContentRaw().split(" ")[0].toLowerCase());
    }

    /**
     * Gets the command matching the given command, both the command prefix
     * and the command trigger must match for the command to be returned,
     * if the guild/server that the command was executed in has a
     * custom prefix set, the custom prefix will be used to
     * match the command instead.
     * <p>
     * If no commands was found matching the given command string, the guilds
     * aliases will be checked instead if the current guild has any.
     *
     * @param avaire  The Xeus application class instance.
     * @param message The JDA message object for the current message.
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given command with the highest priority, or the alias command matching the given command.
     */
    public static CommandContainer getCommand(Xeus avaire, Message message, @Nonnull String command) {
        CommandContainer commandContainer = getCommand(message);
        if (commandContainer != null) {
            return commandContainer;
        }
        return getCommandByAlias(avaire, message, command);
    }

    /**
     * Get the command matching the given command, both the command prefix
     * and the command trigger must match for the command to be returned,
     * if the guild/server that the command was executed in has a
     * custom prefix set the custom prefix will be used to
     * match the command instead.
     * <p>
     * If a commands priority is set to {@link CommandPriority#IGNORED}
     * the command will be omitted from the search.
     *
     * @param message The JDA message object for the current message.
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given command with the highest priority.
     */
    public static CommandContainer getCommand(Message message, @Nonnull String command) {
        List<CommandContainer> commands = new ArrayList<>();
        for (CommandContainer container : COMMANDS) {
            String commandPrefix = container.getCommand().generateCommandPrefix(message);
            for (String trigger : container.getTriggers()) {
                if (command.equalsIgnoreCase(commandPrefix + trigger)) {
                    commands.add(container);
                }
            }
        }

        return getHighPriorityCommandFromCommands(commands);
    }

    /**
     * Get the command matching the given command, both the command prefix
     * and the command trigger must match for the command to be returned,
     * this method will ignore any command prefix that might've been
     * set by the guild/server, and will instead use the default.
     * <p>
     * If a commands priority is set to {@link CommandPriority#IGNORED}
     * the command will be omitted from the search.
     *
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given command with the highest priority.
     */
    public static CommandContainer getRawCommand(@Nonnull String command) {
        List<CommandContainer> commands = new ArrayList<>();
        for (CommandContainer container : COMMANDS) {
            String commandPrefix = container.getDefaultPrefix();
            for (String trigger : container.getTriggers()) {
                if (command.equalsIgnoreCase(commandPrefix + trigger)) {
                    commands.add(container);
                }
            }
        }

        return getHighPriorityCommandFromCommands(commands);
    }

    /**
     * Gets the command matching the given command alias for the current message if
     * the message was sent in a guild and the guild has at least one alias set.
     *
     * @param avaire  The Xeus application class instance.
     * @param message The JDA message object for the current message.
     * @param command The command string that should be matched with the commands.
     * @return Possibly-null, The command matching the given alias with the highest priority.
     */
    public static CommandContainer getCommandByAlias(Xeus avaire, Message message, @Nonnull String command) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, message);
        if (transformer == null || transformer.getAliases().isEmpty()) {
            return null;
        }

        String[] aliasArguments = null;
        String commandString = command.split(" ")[0].toLowerCase();
        List<CommandContainer> commands = new ArrayList<>();
        for (Map.Entry<String, String> entry : transformer.getAliases().entrySet()) {
            if (commandString.equalsIgnoreCase(entry.getKey())) {
                CommandContainer commandContainer = getRawCommand(entry.getValue().split(" ")[0]);
                if (commandContainer != null) {
                    commands.add(commandContainer);
                    aliasArguments = entry.getValue().split(" ");
                }
            }
        }

        CommandContainer commandContainer = getHighPriorityCommandFromCommands(commands);

        if (commandContainer == null) {
            return null;
        }

        if (aliasArguments == null || aliasArguments.length == 1) {
            return commandContainer;
        }
        return new AliasCommandContainer(commandContainer, Arrays.copyOfRange(aliasArguments, 1, aliasArguments.length));
    }

    /**
     * Get any command matching the given command trigger, this method will
     * use a lazy comparison by omitting the command prefix and only
     * comparing the command triggers, if a commands priority is
     * set to {@link CommandPriority#IGNORED} the command will
     * be omitted from the search.
     *
     * @param commandTrigger The command trigger that should be lazy searched for.
     * @return Possibly-null, The command matching the given command trigger with the highest priority.
     */
    public static CommandContainer getLazyCommand(@Nonnull String commandTrigger) {
        List<CommandContainer> commands = new ArrayList<>();
        for (CommandContainer container : COMMANDS) {
            if (container.getPriority().equals(CommandPriority.IGNORED)) {
                continue;
            }

            for (String trigger : container.getTriggers()) {
                if (commandTrigger.equalsIgnoreCase(trigger)) {
                    commands.add(container);
                }
            }
        }

        return getHighPriorityCommandFromCommands(commands);
    }

    /**
     * Generates a linked hash map of all commands registered to the command handler,
     * where the key is the name of the category the command is linked to, and the
     * value is the category data context, the category context will contain
     * the category prefix and the command data context.
     * <p>
     * <b>Note:</b> This method is used for generating the necessary data to produce the
     * commandMap.json file(See {@link JSONCmdMapCommand JSON Command Map Command} for
     * more info).
     *
     * @param context The command message context that should be used to to generate
     *                the command data, or {@code NULL}.
     * @return The generated command map containing details about all the registered commands.
     */
    public static LinkedHashMap<String, CategoryDataContext> generateCommandMapFrom(@Nullable CommandMessage context) {
        if (context == null) {
            context = new CommandMessage();
        }

        LinkedHashMap<String, CategoryDataContext> map = new LinkedHashMap<>();
        for (Category category : CategoryHandler.getValues()) {
            LinkedHashMap<String, CommandDataContext> categoryCommands = new LinkedHashMap<>();

            for (CommandContainer container : CommandHandler.getCommands().stream()
                .filter(container -> container.getCategory().equals(category))
                .sorted(Comparator.comparing(container -> container.getCommand().getClass().getSimpleName()))
                .collect(Collectors.toList())) {

                context.setI18nCommandPrefix(container);

                categoryCommands.put(
                    container.getCommand().getClass().getSimpleName(),
                    new CommandDataContext(container)
                );
            }

            if (!categoryCommands.isEmpty()) {
                map.put(category.getName(), new CategoryDataContext(
                    category.getPrefix(), categoryCommands
                ));
            }
        }

        return map;
    }

    /**
     * Gets the highest priority command from the given command
     * list, if the list is empty null is returned instead.
     *
     * @param commands The list of commands matching some query.
     * @return Possibly-null, The command container with the highest priority.
     */
    private static CommandContainer getHighPriorityCommandFromCommands(List<CommandContainer> commands) {
        if (commands.isEmpty()) {
            return null;
        }

        if (commands.size() == 1) {
            return commands.get(0);
        }

        //noinspection ConstantConditions
        return commands.stream().sorted((first, second) -> {
            if (first.getPriority().equals(second.getPriority())) {
                return 0;
            }
            return first.getPriority().isGreaterThan(second.getPriority()) ? -1 : 1;
        }).findFirst().get();
    }

    /**
     * Register the given command into the command handler, creating the
     * command container and saving it into the commands collection.
     *
     * @param command The command that should be registered into the command handler.
     */
    @SuppressWarnings("ConstantConditions")
    public static void register(@Nonnull Command command) {
        Category category = CategoryHandler.fromCommand(command);
        Checks.notNull(category, String.format("%s :: %s", command.getName(), "Invalid command category, command category"));

        try {
            Checks.notNull(command.getDescription(new FakeCommandMessage()), String.format("%s :: %s", command.getName(), "Command description"));
            Checks.notNull(command.getDescription(null), String.format("%s :: %s", command.getName(), "Command description with null"));
            Checks.notNull(command.getDescription(), String.format("%s :: %s", command.getName(), "Command description with no arguments"));
        } catch (StackOverflowError e) {
            throw new MissingCommandDescriptionException(command);
        }

        for (String trigger : command.getTriggers()) {
            for (CommandContainer container : COMMANDS) {
                for (String subTrigger : container.getTriggers()) {
                    if (Objects.equals(category.getPrefix() + trigger, container.getDefaultPrefix() + subTrigger)) {
                        throw new InvalidCommandPrefixException(category.getPrefix() + trigger, command.getName(), container.getCommand().getName());
                    }
                }
            }
        }

        for (String middleware : command.getMiddleware()) {
            String[] parts = middleware.split(":");

            if (MiddlewareHandler.getMiddleware(parts[0]) == null) {
                throw new IllegalArgumentException("Middleware reference may not be null, " + parts[0] + " is not a valid middleware!");
            }
        }

        String commandUri = null;

        CommandSource annotation = command.getClass().getAnnotation(CommandSource.class);
        if (annotation != null && annotation.uri().trim().length() > 0) {
            commandUri = annotation.uri();
        } else if (command.getClass().getTypeName().startsWith(Constants.PACKAGE_COMMAND_PATH)) {
            String[] split = command.getClass().toString().split("\\.");

            commandUri = String.format(Constants.SOURCE_URI, split[split.length - 2], split[split.length - 1]);
        }

        Metrics.commandsExecuted.labels(command.getClass().getSimpleName()).inc(0D);

        COMMANDS.add(new CommandContainer(command, category, commandUri));
    }

    /**
     * Unregisters the given command class from the command register.
     *
     * @param commandClass The command class that should be unregistered.
     * @return {@code True} if the command was unregistered successfully,
     * {@code False} if the command is not registered.
     */
    public static boolean unregister(@Nonnull Class<? extends Command> commandClass) {
        synchronized (COMMANDS) {
            Iterator<CommandContainer> iterator = COMMANDS.iterator();

            while (iterator.hasNext()) {
                CommandContainer container = iterator.next();
                if (container.getCommand().getClass().getTypeName().equals(commandClass.getTypeName())) {
                    iterator.remove();

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a collection of all the commands
     * registered into the command handler.
     *
     * @return A collection of all the commands registered with the command handler.
     */
    public static Collection<CommandContainer> getCommands() {
        return COMMANDS;
    }

    private static boolean hasImplementedADescriptionMethod(Command command) {
        try {
            Xeus.getLogger().info("{} called hasImplementedADescriptionMethod::withArgs", command.getClass().getTypeName());
            //noinspection JavaReflectionMemberAccess
            command.getClass().getMethod("getDescription", CommandMessage.class);

            return true;
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Xeus.getLogger().info("{} called hasImplementedADescriptionMethod::noArgs", command.getClass().getTypeName());
            command.getClass().getMethod("getDescription");

            return true;
        } catch (NoSuchMethodException ignored) {
        }

        return false;
    }
}
