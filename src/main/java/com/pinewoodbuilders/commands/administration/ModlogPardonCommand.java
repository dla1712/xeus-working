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

import com.pinewoodbuilders.Constants;
import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.commands.Command;
import com.pinewoodbuilders.contracts.commands.CommandGroup;
import com.pinewoodbuilders.contracts.commands.CommandGroups;
import com.pinewoodbuilders.database.collection.Collection;
import com.pinewoodbuilders.database.collection.DataRow;
import com.pinewoodbuilders.database.transformers.GlobalSettingsTransformer;
import com.pinewoodbuilders.database.transformers.GuildSettingsTransformer;
import com.pinewoodbuilders.database.transformers.GuildTransformer;
import com.pinewoodbuilders.moderation.local.warn.WarnContainer;
import com.pinewoodbuilders.modlog.local.moderation.Modlog;
import com.pinewoodbuilders.modlog.local.shared.ModlogAction;
import com.pinewoodbuilders.modlog.local.shared.ModlogType;
import com.pinewoodbuilders.contracts.permission.GuildPermissionCheckType;
import com.pinewoodbuilders.utilities.XeusPermissionUtil;
import com.pinewoodbuilders.utilities.NumberUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModlogPardonCommand extends Command {

    public ModlogPardonCommand(Xeus avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Pardon Command";
    }

    @Override
    public String getDescription() {
        return "Pardons the given modlog case ID, removing it from the users modlog history log and locking the message so it can't be edited." +
            "\nServer admins can pardon any modlog cases, while everyone else can only pardon modlog cases they themselves are the cause for." +
            "\n\n**Note:** This command does not revert the action for the modlog case, so if the user was banned, they will not be unbanned using this command.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command <modlog id> [reason]` - Pardons the given modlog case ID with the given reason."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Collections.singletonList("`:command 9 Whoops, was a mistake`");
    }

    @Override
    public List <Class <? extends Command>> getRelations() {
        return Arrays.asList(
            ModlogHistoryCommand.class,
            WarnCommand.class,
            SoftBanCommand.class,
            BanCommand.class,
            UnbanCommand.class,
            KickCommand.class,
            VoiceKickCommand.class
        );
    }

    @Override
    public List <String> getTriggers() {
        return Collections.singletonList("pardon");
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "throttle:user,1,4",
            "isGuildHROrHigher",
            "requireOne:user,text.manage_messages,general.kick_members,general.ban_members"
        );
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MODERATION);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            return sendErrorMessage(context, "errors.errorOccurredWhileLoading", "server settings");
        }

        if (transformer.getModlog() == null) {
            return sendErrorMessage(context, context.i18n("modlogNotEnabled"));
        }

        if (args.length == 0) {
            return sendErrorMessage(context, "errors.missingArgument", "case id");
        }

        int caseId = NumberUtil.parseInt(args[0], -1);
        if (caseId < 1 || caseId > transformer.getModlogCase()) {
            return sendErrorMessage(context, context.i18n("invalidCaseId", transformer.getModlogCase()));
        }

        final String reason = args.length == 1
            ? "No reason was given."
            : String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        try {
            Collection collection = avaire.getDatabase().newQueryBuilder(Constants.LOG_TABLE_NAME)
                .where("guild_id", context.getGuild().getId())
                .where("modlogCase", caseId)
                .get();

            if (collection.isEmpty()) {
                return sendErrorMessage(context, context.i18n("couldntFindCaseWithId", caseId));
            }

            DataRow row = collection.first();
            int permissionLevel = XeusPermissionUtil.getPermissionLevel(context).getLevel();
            if (!canEditModlogCase(context, row, permissionLevel)) {
                return sendErrorMessage(context, context.i18n("couldntFindCaseWithId", caseId));
            }

            if (row.getBoolean("pardon", false)) {
                return sendErrorMessage(context, context.i18n("caseAlreadyPardoned", caseId));
            }

            ModlogType type = ModlogType.fromId(row.getInt("type", -1));
            if (type != null && !type.isPunishment()) {
                return sendErrorMessage(context, context.i18n("notAPunishment", caseId));
            }

            avaire.getDatabase().newQueryBuilder(Constants.LOG_TABLE_NAME)
                .useAsync(true)
                .where("guild_id", context.getGuild().getId())
                .where("modlogCase", caseId)
                .update(statement -> statement.set("pardon", true));

            ModlogAction modlogAction = new ModlogAction(
                ModlogType.PARDON,
                context.getAuthor(), null,
                caseId + ":" + row.getString("message_id") + "\n" + reason
            );

            Modlog.log(avaire, context.getGuild(), transformer, modlogAction);

            context.makeSuccess(context.i18n("message"))
                .set("case", caseId)
                .queue();

            if (type == ModlogType.WARN) {
                GuildSettingsTransformer settings = context.getGuildSettingsTransformer();
                if (settings == null) return true;
                GlobalSettingsTransformer globalSettings = settings.getGlobalSettings();
                if (globalSettings == null) return true;
                if (!globalSettings.getNewWarnSystem()) return true;

                for (WarnContainer c : avaire.getWarningsManager().getWarns(context.getGuild().getIdLong(), row.getLong("target_id")))
                    if (c.getCaseId().equals(String.valueOf(caseId))) {
                        try {
                            avaire.getWarningsManager().unregisterWarn(context.guild.getIdLong(), row.getLong("target_id"), String.valueOf(caseId));
                            User u = avaire.getShardManager().getUserById(row.getLong("target_id"));
                            if (u != null) {
                                Modlog.notifyUser(u, context.guild, modlogAction, "Warn has been removed from the database, your warn total has now lessened.");
                            }
                        } catch (SQLException e) {
                            context.makeError("Failed to remove the warn in the database, this warn may not expire... Please check with the developer.").queue();
                        }
                    }
            }
        } catch (SQLException e) {
            return sendErrorMessage(context, "Failed to load the case ID from the database, please try again later.");
        }


        return true;
    }

    private boolean canEditModlogCase(CommandMessage context, DataRow collection, int permissionLevel) {
        return context.getMember().hasPermission(Permission.ADMINISTRATOR)
            || collection.getString("user_id", "").equals(context.getAuthor().getId()) || permissionLevel >= GuildPermissionCheckType.MAIN_GLOBAL_MODERATOR.getLevel();
    }
}
