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

package com.pinewoodbuilders.database.schema;

public enum DatabaseEngine {

    MyISAM("MyISAM"),
    InnoDB("InnoDB"),
    MERGE("MERGE"),
    MEMORY("MEMORY"),
    BDB("BDB"),
    EXAMPLE("EXAMPLE"),
    FEDERATED("FEDERATED"),
    ARCHIVE("ARCHIVE"),
    CSV("CSV"),
    BLACKHOLE("BLACKHOLE");

    private final String engine;

    DatabaseEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Gets the database engine string value.
     *
     * @return the database engines string value.
     */
    public String getEngine() {
        return engine;
    }
}
