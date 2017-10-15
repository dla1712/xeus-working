package com.avairebot.orion.handlers;

import com.avairebot.orion.Orion;
import com.avairebot.orion.contracts.handlers.EventHandler;
import com.avairebot.orion.database.controllers.GuildController;
import com.avairebot.orion.database.transformers.ChannelTransformer;
import com.avairebot.orion.database.transformers.GuildTransformer;
import com.avairebot.orion.permissions.Permissions;
import com.avairebot.orion.utilities.PlaceholderUtil;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;

public class GuildMemberJoin extends EventHandler {

    /**
     * Instantiates the event handler and sets the orion class instance.
     *
     * @param orion The Orion application class instance.
     */
    public GuildMemberJoin(Orion orion) {
        super(orion);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        GuildTransformer transformer = GuildController.fetchGuild(orion, event.getGuild());

        for (ChannelTransformer channelTransformer : transformer.getChannels()) {
            if (channelTransformer.getWelcome().isEnabled()) {
                TextChannel textChannel = event.getGuild().getTextChannelById(channelTransformer.getId());
                if (textChannel == null) {
                    continue;
                }

                textChannel.sendMessage(
                    PlaceholderUtil.parseChannel(textChannel,
                        PlaceholderUtil.parseUser(event.getUser(),
                            PlaceholderUtil.parseGuild(event.getGuild(),
                                channelTransformer.getWelcome().getMessage() == null ?
                                    "Welcome %user% to **%server%!**" :
                                    channelTransformer.getWelcome().getMessage()
                            )
                        )
                    )
                ).queue();
            }
        }

        if (event.getUser().isBot()) {
            return;
        }

        if (transformer.getAutorole() != null) {
            Role role = event.getGuild().getRoleById(transformer.getAutorole());
            if (role != null && event.getMember().hasPermission(Permissions.MANAGE_ROLES.getPermission())) {
                event.getGuild().getController().addSingleRoleToMember(
                    event.getMember(), role
                ).queue();
            }
        }
    }
}