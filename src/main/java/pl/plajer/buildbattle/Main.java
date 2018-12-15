/*
 * BuildBattle 4 - Ultimate building competition minigame
 * Copyright (C) 2018  Plajer's Lair - maintained by Plajer and Tigerpanzer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.plajer.buildbattle;

import java.util.Arrays;
import java.util.List;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import pl.plajer.buildbattle.api.StatsStorage;
import pl.plajer.buildbattle.arena.Arena;
import pl.plajer.buildbattle.arena.ArenaManager;
import pl.plajer.buildbattle.arena.ArenaRegistry;
import pl.plajer.buildbattle.commands.MainCommand;
import pl.plajer.buildbattle.events.GameEvents;
import pl.plajer.buildbattle.events.JoinEvents;
import pl.plajer.buildbattle.events.QuitEvents;
import pl.plajer.buildbattle.handlers.BungeeManager;
import pl.plajer.buildbattle.handlers.ChatManager;
import pl.plajer.buildbattle.handlers.PermissionManager;
import pl.plajer.buildbattle.handlers.PlaceholderManager;
import pl.plajer.buildbattle.handlers.SignManager;
import pl.plajer.buildbattle.handlers.items.SpecialItem;
import pl.plajer.buildbattle.handlers.language.LanguageManager;
import pl.plajer.buildbattle.handlers.language.LanguageMigrator;
import pl.plajer.buildbattle.handlers.setup.SetupInventoryEvents;
import pl.plajer.buildbattle.menus.GameInventories;
import pl.plajer.buildbattle.menus.OptionsMenu;
import pl.plajer.buildbattle.menus.particles.ParticleHandler;
import pl.plajer.buildbattle.menus.particles.ParticleMenu;
import pl.plajer.buildbattle.menus.playerheads.PlayerHeadsMenu;
import pl.plajer.buildbattle.menus.themevoter.VoteMenuListener;
import pl.plajer.buildbattle.user.User;
import pl.plajer.buildbattle.user.UserManager;
import pl.plajer.buildbattle.utils.CuboidSelector;
import pl.plajer.buildbattle.utils.LegacyDataFixer;
import pl.plajer.buildbattle.utils.MessageUtils;
import pl.plajerlair.core.database.MySQLDatabase;
import pl.plajerlair.core.services.ServiceRegistry;
import pl.plajerlair.core.services.exception.ReportedException;
import pl.plajerlair.core.services.update.UpdateChecker;
import pl.plajerlair.core.utils.ConfigUtils;

/**
 * Created by Tom on 17/08/2015.
 */
public class Main extends JavaPlugin {

  private static boolean debug;
  private boolean databaseActivated = false;
  private boolean forceDisable = false;
  private MySQLDatabase database;
  private UserManager userManager;
  private BungeeManager bungeeManager;
  private boolean bungeeActivated;
  private boolean inventoryManagerEnabled;
  private SignManager signManager;
  private CuboidSelector cuboidSelector;
  private GameInventories gameInventories;
  private VoteItems voteItems;
  private OptionsMenu optionsMenu;
  private String version;
  private List<String> filesToGenerate = Arrays.asList("arenas", "particles", "lobbyitems", "stats", "voteItems", "mysql");

  public static void debug(LogLevel level, String thing) {
    if (debug) {
      switch (level) {
        case INFO:
          Bukkit.getConsoleSender().sendMessage("[Build Battle Debugger] " + thing);
          break;
        case WARN:
          Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[Build Battle Debugger] " + thing);
          break;
        case ERROR:
          Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[Build Battle Debugger] " + thing);
          break;
        case WTF:
          Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "[Build Battle Debugger] [SEVERE]" + thing);
          break;
        case TASK:
          Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[Build Battle Debugger] Running task '" + thing + "'");
          break;
      }
    }
  }


  public CuboidSelector getCuboidSelector() {
    return cuboidSelector;
  }

  public GameInventories getGameInventories() {
    return gameInventories;
  }

  public VoteItems getVoteItems() {
    return voteItems;
  }

  public OptionsMenu getOptionsMenu() {
    return optionsMenu;
  }

  public BungeeManager getBungeeManager() {
    return bungeeManager;
  }

  public boolean isBungeeActivated() {
    return bungeeActivated;
  }

  public SignManager getSignManager() {
    return signManager;
  }

  public boolean isInventoryManagerEnabled() {
    return inventoryManagerEnabled;
  }

  public boolean is1_11_R1() {
    return version.equalsIgnoreCase("v1_11_R1");
  }

  public boolean is1_12_R1() {
    return version.equalsIgnoreCase("v1_12_R1");
  }

  @Override
  public void onEnable() {
    ServiceRegistry.registerService(this);
    try {
      version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
      try {
        Class.forName("org.spigotmc.SpigotConfig");
      } catch (Exception e) {
        MessageUtils.thisVersionIsNotSupported();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Your server software is not supported by Build Battle!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "We support only Spigot and Spigot forks only! Shutting off...");
        forceDisable = true;
        getServer().getPluginManager().disablePlugin(this);
        return;
      }
      if (version.contains("v1_10") || version.contains("v1_9") || version.contains("v1_8") || version.contains("v1_7") || version.contains("v1_6")) {
        MessageUtils.thisVersionIsNotSupported();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Your server version is not supported by BuildBattle!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Sadly, we must shut off. Maybe you consider updating your server version?");
        forceDisable = true;
        getServer().getPluginManager().disablePlugin(this);
        return;
      }
      setupConfigValues();
      //check if using 2.0.0 releases
      if (ConfigUtils.getConfig(this, "language").isSet("PREFIX") && ConfigUtils.getConfig(this, "language").isSet("Unlocks-at-level")) {
        LanguageMigrator.migrateToNewFormat();
      }
      debug(LogLevel.INFO, "Main setup started");
      saveDefaultConfig();
      LanguageManager.init(this);
      new LegacyDataFixer(this);
      initializeClasses();
      if (getConfig().getBoolean("BungeeActivated")) {
        bungeeManager = new BungeeManager(this);
      }
      for (String s : filesToGenerate) {
        ConfigUtils.getConfig(this, s);
      }
      if (databaseActivated) {
        FileConfiguration config = ConfigUtils.getConfig(this, "mysql");
        database = new MySQLDatabase(this, config.getString("address"), config.getString("user"), config.getString("password"),
            config.getInt("min-connections"), config.getInt("max-connections"));
      }
      userManager = new UserManager(this);
      loadStatsForPlayersOnline();
    } catch (Exception ex) {
      new ReportedException(this, ex);
    }
  }

  private void checkUpdate() {
    if (!getConfig().getBoolean("Update-Notifier.Enabled", true)) {
      return;
    }
    UpdateChecker.init(this, 44703).requestUpdateCheck().whenComplete((result, exception) -> {
      if (!result.requiresUpdate()) {
        return;
      }
      if (result.getNewestVersion().contains("b")) {
        if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
          Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildBattle] Your software is ready for update! However it's a BETA VERSION. Proceed with caution.");
          Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildBattle] Current version %old%, latest version %new%".replace("%old%", getDescription().getVersion()).replace("%new%",
              result.getNewestVersion()));
        }
        return;
      }
      MessageUtils.updateIsHere();
      Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Your Build Battle plugin is outdated! Download it to keep with latest changes and fixes.");
      Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Disable this option in config.yml if you wish.");
      Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Current version: " + ChatColor.RED + getDescription().getVersion() + ChatColor.YELLOW + " Latest version: " + ChatColor.GREEN + result.getNewestVersion());
    });
  }

  @Override
  public void onDisable() {
    if (forceDisable) {
      return;
    }
    for (final Player player : getServer().getOnlinePlayers()) {
      Arena arena = ArenaRegistry.getArena(player);
      if (arena != null) {
        player.setGameMode(GameMode.SURVIVAL);
        if (ConfigPreferences.isBossBarEnabled()) {
          arena.getGameBar().removePlayer(player);
        }
        ArenaManager.leaveAttempt(player, arena);
      }
      final User user = userManager.getUser(player.getUniqueId());
      for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
        userManager.saveStatistic(user, stat);
      }
      userManager.removeUser(player.getUniqueId());
    }
    if (databaseActivated) {
      getMySQLDatabase().getManager().shutdownConnPool();
    }
  }

  private void setupConfigValues() {
    debug = getConfig().getBoolean("Debug", false);
    bungeeActivated = getConfig().getBoolean("BungeeActivated", false);
    inventoryManagerEnabled = getConfig().getBoolean("InventoryManager", true);
    databaseActivated = getConfig().getBoolean("DatabaseActivated", false);
  }

  private void initializeClasses() {
    new ConfigPreferences(this);
    new ChatManager();
    PermissionManager.init();
    new SetupInventoryEvents(this);
    new MainCommand(this);
    ConfigPreferences.loadOptions();
    ParticleMenu.loadFromConfig();
    PlayerHeadsMenu.loadHeadItems();
    ArenaRegistry.registerArenas();
    //load signs after arenas
    signManager = new SignManager(this);
    SpecialItem.loadAll();
    voteItems = new VoteItems();
    optionsMenu = new OptionsMenu();
    ParticleHandler particleHandler = new ParticleHandler(this);
    particleHandler.start();
    Metrics metrics = new Metrics(this);
    metrics.addCustomChart(new Metrics.SimplePie("bungeecord_hooked", () -> String.valueOf(bungeeActivated)));
    metrics.addCustomChart(new Metrics.SimplePie("locale_used", () -> LanguageManager.getPluginLocale().getPrefix()));
    metrics.addCustomChart(new Metrics.SimplePie("update_notifier", () -> {
      if (getConfig().getBoolean("Update-Notifier.Enabled", true)) {
        if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
          return "Enabled with beta notifier";
        } else {
          return "Enabled";
        }
      } else {
        if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
          return "Beta notifier only";
        } else {
          return "Disabled";
        }
      }
    }));
    new JoinEvents(this);
    new QuitEvents(this);
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new PlaceholderManager().register();
    }
    cuboidSelector = new CuboidSelector(this);
    UpdateChecker.init(this, 44703);
    checkUpdate();
    new GameEvents(this);
    new VoteMenuListener(this);
    gameInventories = new GameInventories();
  }

  public boolean isDatabaseActivated() {
    return databaseActivated;
  }

  public MySQLDatabase getMySQLDatabase() {
    return database;
  }

  public UserManager getUserManager() {
    return userManager;
  }

  private void loadStatsForPlayersOnline() {
    for (final Player player : getServer().getOnlinePlayers()) {
      if (bungeeActivated) {
        ArenaRegistry.getArenas().get(0).teleportToLobby(player);
      }
      User user = userManager.getUser(player.getUniqueId());
      for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
        userManager.loadStatistic(user, stat);
      }
    }
  }

  public enum LogLevel {
    INFO, WARN, ERROR, WTF /*what a terrible failure*/, TASK
  }

}
