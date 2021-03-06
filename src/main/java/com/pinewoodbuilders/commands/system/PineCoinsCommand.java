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

package com.pinewoodbuilders.commands.system;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.Constants;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.commands.SystemCommand;
import com.pinewoodbuilders.language.I18n;
import com.pinewoodbuilders.time.Carbon;
import com.pinewoodbuilders.utilities.MentionableUtil;
import com.pinewoodbuilders.utilities.NumberUtil;
import com.pinewoodbuilders.vote.VoteCacheEntity;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PineCoinsCommand extends SystemCommand {

    private static final Logger log = LoggerFactory.getLogger(PineCoinsCommand.class);

    public PineCoinsCommand(Xeus avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "PineCoins Command";
    }

    @Override
    public String getDescription() {
        return "Allows a bot administrator to give or take PineCoins form a user by their ID, or by mentioning them.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command give <user ID> <amount>` - Gives the user the given amount of vote points.",
            "`:command take <user ID> <amount>` - Takes the given amount of vote points from the user."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command give @Senither 50` - Give Senither 50 PineCoins.",
            "`:command take @Senither 99` - Take 99 PineCoins from Senither."
        );
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("pine-coin");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!hasEnoughArguments(context, args)) {
            return false;
        }

        Type type = Type.fromName(args[0]);
        if (type.equals(Type.UNKNOWN)) {
            return sendErrorMessage(context, "errors.invalidProperty", "type", "type");
        }

        User user = MentionableUtil.getUser(context, args, 1);
        if (user == null) {
            try {
                user = avaire.getShardManager().getUserById(args[1]);

                if (user == null) {
                    return sendErrorMessage(context, "errors.invalidProperty", "user", "user");
                }
            } catch (NumberFormatException ignored) {
                return sendErrorMessage(context, "errors.invalidProperty", "user", "user");
            }
        }

        if (user.isBot()) {
            return sendErrorMessage(context, "You can't give or take PineCoins for bot accounts.");
        }

        int amount = NumberUtil.parseInt(args[2], -1);
        if (amount < 1) {
            return sendErrorMessage(context, "The amount has to be at least `1` or higher.");
        }

        try {
            VoteCacheEntity entity = avaire.getVoteManager().getVoteEntity(user);

            if (entity == null) {
                createRecord(user.getIdLong(), type.equals(Type.GIVE) ? amount : 0);
            } else {
                updateRecord(type, entity, amount);
            }

            context.makeSuccess(type.equals(Type.GIVE)
                ? ":target have successfully been given **:amount** PineCoins!"
                : ":target have successfully had **:amount** PineCoins removed!"
            )
                .set("target", user.getAsMention())
                .set("amount", amount)
                .queue();

            return true;
        } catch (SQLException e) {
            log.error("Failed to modify the PineCoins for the user with an ID of {}, error: {}", user.getId(), e.getMessage(), e);

            return sendErrorMessage(context, "Something went wrong while trying to modify the users PineCoins, error: {0}", e.getMessage());
        }
    }

    private boolean hasEnoughArguments(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "errors.missingArgument", "type");
        }

        if (args.length == 1) {
            return sendErrorMessage(context, "errors.missingArgument", "user");
        }

        if (args.length == 2) {
            return sendErrorMessage(context, "errors.missingArgument", "amount");
        }

        avaire.getVoteManager().getVoteEntity(context.getAuthor());

        return true;
    }

    private void createRecord(long userId, int amount) throws SQLException {
        avaire.getDatabase().newQueryBuilder(Constants.BOT_VOTES_TABLE_NAME)
            .insert(statement -> {
                statement.set("user_id", userId);
                statement.set("expires_in", Carbon.now().toDayDateTimeString());
                statement.set("points", amount);
                statement.set("points_total", amount);
            });
    }

    private void updateRecord(Type type, VoteCacheEntity entity, int amount) throws SQLException {
        if (type.equals(Type.TAKE) && entity.getVotePoints() - amount < 0) {
            amount = entity.getVotePoints();
        }

        int finalAmount = amount;
        avaire.getDatabase().newQueryBuilder(Constants.BOT_VOTES_TABLE_NAME)
            .where("user_id", entity.getUserId())
            .update(statement -> {
                statement.setRaw("points", I18n.format("`points` {0} {1}",
                    type.equals(Type.GIVE) ? '+' : '-', finalAmount
                ));
                statement.setRaw("points_total", I18n.format("`points_total` {0} {1}",
                    type.equals(Type.GIVE) ? '+' : '-', finalAmount
                ));
            });

        entity.setVotePoints(entity.getVotePoints() + (
            type.equals(Type.GIVE) ? finalAmount : finalAmount * -1
        ));
    }

    private enum Type {

        GIVE, TAKE, UNKNOWN;

        public static Type fromName(String name) {
            for (Type type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
}
