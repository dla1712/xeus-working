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

package com.pinewoodbuilders.commands.administration;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.commands.Command;
import com.pinewoodbuilders.contracts.commands.CommandGroup;
import com.pinewoodbuilders.contracts.commands.CommandGroups;
import com.pinewoodbuilders.factories.MessageFactory;
import com.pinewoodbuilders.utilities.MentionableUtil;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UserAvatarCommand extends Command {

    public UserAvatarCommand(Xeus avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "User Avatar Command";
    }

    @Override
    public String getDescription() {
        return "Get the profile picture of someone on the server by name, id, or mentions.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <user | user id>` - Gets the avatar of the given user.");
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command @Senither`",
            "`:command`"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("avatar");
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.INFORMATION);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        User user = context.getAuthor();
        if (args.length > 0) {
            user = MentionableUtil.getUser(context, new String[]{String.join(" ", args)});
        }

        if (user == null) {
            return sendErrorMessage(context, context.i18n("noUserFound", args[0]));
        }

        String avatarUrl = generateAvatarUrl(user);
        MessageFactory.makeEmbeddedMessage(context.getChannel())
            .setTitle(context.i18n("title", user.getName(), user.getDiscriminator()), avatarUrl)
            .requestedBy(context.getMember())
            .setImage(avatarUrl)
            .queue();

        return true;
    }

    private String generateAvatarUrl(User user) {
        String avatarUrl = user.getEffectiveAvatarUrl();
        String[] parts = avatarUrl.split("\\.");

        String extension = parts[parts.length - 1];

        return avatarUrl + "?size=256&." + extension;
    }
}
