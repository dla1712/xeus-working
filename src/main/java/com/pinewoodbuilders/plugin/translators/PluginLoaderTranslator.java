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

package com.pinewoodbuilders.plugin.translators;

import com.pinewoodbuilders.contracts.plugin.Plugin;
import com.pinewoodbuilders.plugin.PluginHolder;
import com.pinewoodbuilders.plugin.PluginLoader;
import com.pinewoodbuilders.plugin.PluginRepository;

import java.util.List;

public class PluginLoaderTranslator implements Plugin {

    private final PluginLoader loader;
    private PluginHolder holder = null;

    /**
     * Creates the new plugin loader translator instance, this is
     * used to act as a mediator between the JSON data loaded
     * from the plugins list from github, and the actual
     * plugin data of the plugin that is installed.
     *
     * @param loader  The plugin loader for the installed plugin.
     * @param plugins The list of plugins that should be matched
     *                against the loaded plugin.
     */
    public PluginLoaderTranslator(PluginLoader loader, List<PluginHolder> plugins) {
        this.loader = loader;

        for (PluginHolder plugin : plugins) {
            if (plugin.getName().equalsIgnoreCase(loader.getName())) {
                this.holder = plugin;
                break;
            }
        }
    }

    @Override
    public String getName() {
        return loader.getName();
    }

    @Override
    public String getDescription() {
        if (loader.getDescription() != null) {
            return loader.getDescription();
        }
        return holder == null ? null : holder.getDescription();
    }

    @Override
    public List<String> getAuthors() {
        if (loader.getAuthors().isEmpty() && holder != null) {
            return holder.getAuthors();
        }
        return loader.getAuthors();
    }

    @Override
    public PluginRepository getRepository() {
        if (holder == null) {
            return null;
        }
        return holder.getRepository();
    }

    @Override
    public boolean isInstalled() {
        return true;
    }
}
