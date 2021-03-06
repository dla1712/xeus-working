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

package com.pinewoodbuilders.middleware;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.middleware.Middleware;
import com.pinewoodbuilders.database.transformers.GuildTransformer;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;

public class HasVotedTodayMiddleware extends Middleware {

    public HasVotedTodayMiddleware(Xeus avaire) {
        super(avaire);
    }

    @Override
    public String buildHelpDescription(@Nonnull CommandMessage context, @Nonnull String[] arguments) {
        if (!avaire.getConfig().getBoolean("vote-lock.enabled", true)) {
            return null;
        }
        return "**You must [vote for Ava](https://discordbots.org/bot/avaire) to use this command**";
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (avaire.getVoteManager().isEnabled() && isServerVIP(stack, message)) {
            return stack.next();
        }

        if (avaire.getVoteManager().hasVoted(message.getAuthor())) {
            return stack.next();
        }

        return runMessageCheck(message, () -> {
            avaire.getVoteManager().getMessenger().sendMustVoteMessage(message.getChannel());

            return false;
        });
    }

    private boolean isServerVIP(MiddlewareStack stack, Message message) {
        if (!message.getChannelType().isGuild()) {
            return false;
        }

        GuildTransformer transformer = stack.getDatabaseEventHolder().getGuild();
        return transformer != null && !transformer.getType().isDefault();
    }
}
