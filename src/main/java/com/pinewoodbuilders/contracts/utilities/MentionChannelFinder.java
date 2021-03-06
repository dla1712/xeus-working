/*
 * Copyright (c) 2019.
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

package com.pinewoodbuilders.contracts.utilities;

import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface MentionChannelFinder {

    /**
     * Tries to find the a channel using the given JDA message object and
     * name, if no channel were found then {@code NULL} will be returned.
     *
     * @param message The JDA message object instance.
     * @param name    The name of the channel to look for.
     * @return Possibly-null, the first channel with the given name.
     */
    @Nullable
    GuildChannel find(@Nonnull Message message, @Nonnull String name);
}
