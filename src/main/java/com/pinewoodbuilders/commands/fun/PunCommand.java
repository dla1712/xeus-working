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

package com.pinewoodbuilders.commands.fun;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.chat.PlaceholderMessage;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.commands.Command;
import com.pinewoodbuilders.factories.RequestFactory;
import com.pinewoodbuilders.requests.Request;
import com.pinewoodbuilders.requests.Response;
import com.pinewoodbuilders.requests.service.PunService;
import com.pinewoodbuilders.utilities.RandomUtil;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PunCommand extends Command {

    public PunCommand(Xeus avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Pun Command";
    }

    @Override
    public String getDescription() {
        return "Gets a random pun about a random subject, or about provide your own subject to get puns about that instead.\nAll puns are loaded from: [icanhazdadjoke.com](https://icanhazdadjoke.com/)";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Gets a random pun",
            "`:command <query>` - Gets a pun about the given query.");
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command` - Gets a random pun.",
            "`:command chicken` - Gets a random pun about chickens."
        );
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("pun", "dadjoke");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("throttle:channel,2,5");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return getAndSendSingleJoke(context);
        }

        makeRequest(context, true)
            .addParameter("term", String.join(" ", args))
            .send((Consumer<Response>) response -> {
                String query = String.join(" ", args).trim();
                PunService service = (PunService) response.toService(PunService.class);

                if (!service.hasData()) {
                    context.makeWarning(context.i18n(
                        "noResultsWithQuery"
                    )).set("query", query).queue();
                    return;
                }

                PunService.Pun pun = (PunService.Pun) RandomUtil.pickRandom(service.getResults());

                sendPun(context, pun.getJoke(), query);
            });

        return true;
    }

    private boolean getAndSendSingleJoke(CommandMessage context) {
        makeRequest(context, false).send((Consumer<Response>) response -> {
            JSONObject json = new JSONObject(response.toString());

            if (!json.has("joke")) {
                context.makeWarning(context.i18n("noResults")).queue();
                return;
            }

            sendPun(context, json.getString("joke"), null);
        });

        return true;
    }

    private Request makeRequest(CommandMessage context, boolean withSearch) {
        return RequestFactory.makeGET("https://icanhazdadjoke.com/" + (withSearch ? "search" : ""))
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", String.format("Xeus Bot (ID:%s, GitHub:https://gitlab.com/pinewood-builders/discord/xeus)",
                context.getJDA().getSelfUser().getId()
            ));
    }

    private void sendPun(CommandMessage context, String joke, String query) {
        PlaceholderMessage message = context.makeSuccess(joke)
            .requestedBy(context.getAuthor());

        if (query != null) {
            User user = context.getAuthor();

            message.setFooter(String.format("Requested by %s#%s | %s",
                user.getName(), user.getDiscriminator(),
                context.i18n("query", query)
            ), user.getEffectiveAvatarUrl());
        }

        message.queue();
    }
}
