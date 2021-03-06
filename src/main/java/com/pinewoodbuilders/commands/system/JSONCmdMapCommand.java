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

package com.pinewoodbuilders.commands.system;

import com.pinewoodbuilders.Xeus;
import com.pinewoodbuilders.commands.CategoryDataContext;
import com.pinewoodbuilders.commands.CommandHandler;
import com.pinewoodbuilders.commands.CommandMessage;
import com.pinewoodbuilders.contracts.commands.SystemCommand;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class JSONCmdMapCommand extends SystemCommand {

    public JSONCmdMapCommand(Xeus avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "JSON Command Map";
    }

    @Override
    public String getDescription() {
        return "Creates a JSON map containing detailed information about each command and stores it in a `commandMap.json` file.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command` - Generates the command map and stores it in a file.");
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("jsoncmdmap");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        LinkedHashMap<String, CategoryDataContext> map = CommandHandler.generateCommandMapFrom(context);

        try (FileWriter file = new FileWriter("commandMap.json")) {
            file.write(Xeus.gson.toJson(map));

            context.makeSuccess("The `commandMap.json` file has been updated with the current command information.").queue();
        } catch (IOException e) {
            Xeus.getLogger().error("Something went wrong while trying to save the command map: {}", e.getMessage(), e);
            context.makeError("Failed to store the command map data, error: " + e.getMessage()).queue();
            return false;
        }

        return true;
    }
}
