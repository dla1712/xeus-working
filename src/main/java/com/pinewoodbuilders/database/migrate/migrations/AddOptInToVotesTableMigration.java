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

package com.pinewoodbuilders.database.migrate.migrations;

import com.pinewoodbuilders.Constants;
import com.pinewoodbuilders.contracts.database.migrations.Migration;
import com.pinewoodbuilders.database.schema.Schema;

import java.sql.SQLException;

public class AddOptInToVotesTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Sat, Jun 02, 2018 2:48 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        if (schema.hasColumn(Constants.BOT_VOTES_TABLE_NAME, "opt_in")) {
            return true;
        }

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` ADD `opt_in` TINYINT(1) NOT NULL DEFAULT '1';",
            Constants.BOT_VOTES_TABLE_NAME
        ));

        return true;
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        if (!schema.hasColumn(Constants.BOT_VOTES_TABLE_NAME, "opt_in")) {
            return true;
        }

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `opt_in`;",
            Constants.BOT_VOTES_TABLE_NAME
        ));

        return true;
    }
}
