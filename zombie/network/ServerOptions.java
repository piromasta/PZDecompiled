package zombie.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;

public class ServerOptions {
   public static final ServerOptions instance = new ServerOptions();
   private ArrayList<String> publicOptions = new ArrayList();
   public static HashMap<String, String> clientOptionsList = null;
   public static final int MAX_PORT = 65535;
   private ArrayList<ServerOption> options = new ArrayList();
   private HashMap<String, ServerOption> optionByName = new HashMap();
   public BooleanServerOption PVP = new BooleanServerOption(this, "PVP", true);
   public BooleanServerOption PauseEmpty = new BooleanServerOption(this, "PauseEmpty", true);
   public BooleanServerOption GlobalChat = new BooleanServerOption(this, "GlobalChat", true);
   public StringServerOption ChatStreams = new StringServerOption(this, "ChatStreams", "s,r,a,w,y,sh,f,all", -1);
   public BooleanServerOption Open = new BooleanServerOption(this, "Open", true);
   public TextServerOption ServerWelcomeMessage = new TextServerOption(this, "ServerWelcomeMessage", "Welcome to Project Zomboid Multiplayer! <LINE> <LINE> To interact with the Chat panel: press Tab, T, or Enter. <LINE> <LINE> The Tab key will change the target stream of the message. <LINE> <LINE> Global Streams: /all <LINE> Local Streams: /say, /yell <LINE> Special Steams: /whisper, /safehouse, /faction. <LINE> <LINE> Press the Up arrow to cycle through your message history. Click the Gear icon to customize chat. <LINE> <LINE> Happy surviving!", -1);
   public BooleanServerOption AutoCreateUserInWhiteList = new BooleanServerOption(this, "AutoCreateUserInWhiteList", false);
   public BooleanServerOption DisplayUserName = new BooleanServerOption(this, "DisplayUserName", true);
   public BooleanServerOption ShowFirstAndLastName = new BooleanServerOption(this, "ShowFirstAndLastName", false);
   public StringServerOption SpawnPoint = new StringServerOption(this, "SpawnPoint", "0,0,0", -1);
   public BooleanServerOption SafetySystem = new BooleanServerOption(this, "SafetySystem", true);
   public BooleanServerOption ShowSafety = new BooleanServerOption(this, "ShowSafety", true);
   public IntegerServerOption SafetyToggleTimer = new IntegerServerOption(this, "SafetyToggleTimer", 0, 1000, 2);
   public IntegerServerOption SafetyCooldownTimer = new IntegerServerOption(this, "SafetyCooldownTimer", 0, 1000, 3);
   public StringServerOption SpawnItems = new StringServerOption(this, "SpawnItems", "", -1);
   public IntegerServerOption DefaultPort = new IntegerServerOption(this, "DefaultPort", 0, 65535, 16261);
   public IntegerServerOption UDPPort = new IntegerServerOption(this, "UDPPort", 0, 65535, 16262);
   public IntegerServerOption ResetID = new IntegerServerOption(this, "ResetID", 0, 2147483647, Rand.Next(1000000000));
   public StringServerOption Mods = new StringServerOption(this, "Mods", "", -1);
   public StringServerOption Map = new StringServerOption(this, "Map", "Muldraugh, KY", -1);
   public BooleanServerOption DoLuaChecksum = new BooleanServerOption(this, "DoLuaChecksum", true);
   public BooleanServerOption DenyLoginOnOverloadedServer = new BooleanServerOption(this, "DenyLoginOnOverloadedServer", true);
   public BooleanServerOption Public = new BooleanServerOption(this, "Public", false);
   public StringServerOption PublicName = new StringServerOption(this, "PublicName", "My PZ Server", 64);
   public TextServerOption PublicDescription = new TextServerOption(this, "PublicDescription", "", 256);
   public IntegerServerOption MaxPlayers = new IntegerServerOption(this, "MaxPlayers", 1, 100, 32);
   public IntegerServerOption PingLimit = new IntegerServerOption(this, "PingLimit", 100, 2147483647, 400);
   public IntegerServerOption HoursForLootRespawn = new IntegerServerOption(this, "HoursForLootRespawn", 0, 2147483647, 0);
   public IntegerServerOption MaxItemsForLootRespawn = new IntegerServerOption(this, "MaxItemsForLootRespawn", 1, 2147483647, 4);
   public BooleanServerOption ConstructionPreventsLootRespawn = new BooleanServerOption(this, "ConstructionPreventsLootRespawn", true);
   public BooleanServerOption DropOffWhiteListAfterDeath = new BooleanServerOption(this, "DropOffWhiteListAfterDeath", false);
   public BooleanServerOption NoFire = new BooleanServerOption(this, "NoFire", false);
   public BooleanServerOption AnnounceDeath = new BooleanServerOption(this, "AnnounceDeath", false);
   public DoubleServerOption MinutesPerPage = new DoubleServerOption(this, "MinutesPerPage", 0.0, 60.0, 1.0);
   public IntegerServerOption SaveWorldEveryMinutes = new IntegerServerOption(this, "SaveWorldEveryMinutes", 0, 2147483647, 0);
   public BooleanServerOption PlayerSafehouse = new BooleanServerOption(this, "PlayerSafehouse", false);
   public BooleanServerOption AdminSafehouse = new BooleanServerOption(this, "AdminSafehouse", false);
   public BooleanServerOption SafehouseAllowTrepass = new BooleanServerOption(this, "SafehouseAllowTrepass", true);
   public BooleanServerOption SafehouseAllowFire = new BooleanServerOption(this, "SafehouseAllowFire", true);
   public BooleanServerOption SafehouseAllowLoot = new BooleanServerOption(this, "SafehouseAllowLoot", true);
   public BooleanServerOption SafehouseAllowRespawn = new BooleanServerOption(this, "SafehouseAllowRespawn", false);
   public IntegerServerOption SafehouseDaySurvivedToClaim = new IntegerServerOption(this, "SafehouseDaySurvivedToClaim", 0, 2147483647, 0);
   public IntegerServerOption SafeHouseRemovalTime = new IntegerServerOption(this, "SafeHouseRemovalTime", 0, 2147483647, 144);
   public BooleanServerOption SafehouseAllowNonResidential = new BooleanServerOption(this, "SafehouseAllowNonResidential", false);
   public BooleanServerOption AllowDestructionBySledgehammer = new BooleanServerOption(this, "AllowDestructionBySledgehammer", true);
   public BooleanServerOption SledgehammerOnlyInSafehouse = new BooleanServerOption(this, "SledgehammerOnlyInSafehouse", false);
   public BooleanServerOption KickFastPlayers = new BooleanServerOption(this, "KickFastPlayers", false);
   public StringServerOption ServerPlayerID = new StringServerOption(this, "ServerPlayerID", Integer.toString(Rand.Next(2147483647)), -1);
   public IntegerServerOption RCONPort = new IntegerServerOption(this, "RCONPort", 0, 65535, 27015);
   public StringServerOption RCONPassword = new StringServerOption(this, "RCONPassword", "", -1);
   public BooleanServerOption DiscordEnable = new BooleanServerOption(this, "DiscordEnable", false);
   public StringServerOption DiscordToken = new StringServerOption(this, "DiscordToken", "", -1);
   public StringServerOption DiscordChannel = new StringServerOption(this, "DiscordChannel", "", -1);
   public StringServerOption DiscordChannelID = new StringServerOption(this, "DiscordChannelID", "", -1);
   public StringServerOption Password = new StringServerOption(this, "Password", "", -1);
   public IntegerServerOption MaxAccountsPerUser = new IntegerServerOption(this, "MaxAccountsPerUser", 0, 2147483647, 0);
   public BooleanServerOption AllowCoop = new BooleanServerOption(this, "AllowCoop", true);
   public BooleanServerOption SleepAllowed = new BooleanServerOption(this, "SleepAllowed", false);
   public BooleanServerOption SleepNeeded = new BooleanServerOption(this, "SleepNeeded", false);
   public BooleanServerOption KnockedDownAllowed = new BooleanServerOption(this, "KnockedDownAllowed", true);
   public BooleanServerOption SneakModeHideFromOtherPlayers = new BooleanServerOption(this, "SneakModeHideFromOtherPlayers", true);
   public StringServerOption WorkshopItems = new StringServerOption(this, "WorkshopItems", "", -1);
   public StringServerOption SteamScoreboard = new StringServerOption(this, "SteamScoreboard", "true", -1);
   public BooleanServerOption SteamVAC = new BooleanServerOption(this, "SteamVAC", true);
   public BooleanServerOption UPnP = new BooleanServerOption(this, "UPnP", true);
   public BooleanServerOption VoiceEnable = new BooleanServerOption(this, "VoiceEnable", true);
   public DoubleServerOption VoiceMinDistance = new DoubleServerOption(this, "VoiceMinDistance", 0.0, 100000.0, 10.0);
   public DoubleServerOption VoiceMaxDistance = new DoubleServerOption(this, "VoiceMaxDistance", 0.0, 100000.0, 100.0);
   public BooleanServerOption Voice3D = new BooleanServerOption(this, "Voice3D", true);
   public DoubleServerOption SpeedLimit = new DoubleServerOption(this, "SpeedLimit", 10.0, 150.0, 70.0);
   public BooleanServerOption LoginQueueEnabled = new BooleanServerOption(this, "LoginQueueEnabled", false);
   public IntegerServerOption LoginQueueConnectTimeout = new IntegerServerOption(this, "LoginQueueConnectTimeout", 20, 1200, 60);
   public StringServerOption server_browser_announced_ip = new StringServerOption(this, "server_browser_announced_ip", "", -1);
   public BooleanServerOption PlayerRespawnWithSelf = new BooleanServerOption(this, "PlayerRespawnWithSelf", false);
   public BooleanServerOption PlayerRespawnWithOther = new BooleanServerOption(this, "PlayerRespawnWithOther", false);
   public DoubleServerOption FastForwardMultiplier = new DoubleServerOption(this, "FastForwardMultiplier", 1.0, 100.0, 40.0);
   public BooleanServerOption DisableSafehouseWhenPlayerConnected = new BooleanServerOption(this, "DisableSafehouseWhenPlayerConnected", false);
   public BooleanServerOption Faction = new BooleanServerOption(this, "Faction", true);
   public IntegerServerOption FactionDaySurvivedToCreate = new IntegerServerOption(this, "FactionDaySurvivedToCreate", 0, 2147483647, 0);
   public IntegerServerOption FactionPlayersRequiredForTag = new IntegerServerOption(this, "FactionPlayersRequiredForTag", 1, 2147483647, 1);
   public BooleanServerOption DisableRadioStaff = new BooleanServerOption(this, "DisableRadioStaff", false);
   public BooleanServerOption DisableRadioAdmin = new BooleanServerOption(this, "DisableRadioAdmin", true);
   public BooleanServerOption DisableRadioGM = new BooleanServerOption(this, "DisableRadioGM", true);
   public BooleanServerOption DisableRadioOverseer = new BooleanServerOption(this, "DisableRadioOverseer", false);
   public BooleanServerOption DisableRadioModerator = new BooleanServerOption(this, "DisableRadioModerator", false);
   public BooleanServerOption DisableRadioInvisible = new BooleanServerOption(this, "DisableRadioInvisible", true);
   public StringServerOption ClientCommandFilter = new StringServerOption(this, "ClientCommandFilter", "-vehicle.*;+vehicle.damageWindow;+vehicle.fixPart;+vehicle.installPart;+vehicle.uninstallPart", -1);
   public StringServerOption ClientActionLogs = new StringServerOption(this, "ClientActionLogs", "ISEnterVehicle;ISExitVehicle;ISTakeEngineParts;", -1);
   public BooleanServerOption PerkLogs = new BooleanServerOption(this, "PerkLogs", true);
   public IntegerServerOption ItemNumbersLimitPerContainer = new IntegerServerOption(this, "ItemNumbersLimitPerContainer", 0, 9000, 0);
   public IntegerServerOption BloodSplatLifespanDays = new IntegerServerOption(this, "BloodSplatLifespanDays", 0, 365, 0);
   public BooleanServerOption AllowNonAsciiUsername = new BooleanServerOption(this, "AllowNonAsciiUsername", false);
   public BooleanServerOption BanKickGlobalSound = new BooleanServerOption(this, "BanKickGlobalSound", true);
   public BooleanServerOption RemovePlayerCorpsesOnCorpseRemoval = new BooleanServerOption(this, "RemovePlayerCorpsesOnCorpseRemoval", false);
   public BooleanServerOption TrashDeleteAll = new BooleanServerOption(this, "TrashDeleteAll", false);
   public BooleanServerOption PVPMeleeWhileHitReaction = new BooleanServerOption(this, "PVPMeleeWhileHitReaction", false);
   public BooleanServerOption MouseOverToSeeDisplayName = new BooleanServerOption(this, "MouseOverToSeeDisplayName", true);
   public BooleanServerOption HidePlayersBehindYou = new BooleanServerOption(this, "HidePlayersBehindYou", true);
   public DoubleServerOption PVPMeleeDamageModifier = new DoubleServerOption(this, "PVPMeleeDamageModifier", 0.0, 500.0, 30.0);
   public DoubleServerOption PVPFirearmDamageModifier = new DoubleServerOption(this, "PVPFirearmDamageModifier", 0.0, 500.0, 50.0);
   public DoubleServerOption CarEngineAttractionModifier = new DoubleServerOption(this, "CarEngineAttractionModifier", 0.0, 10.0, 0.5);
   public BooleanServerOption PlayerBumpPlayer = new BooleanServerOption(this, "PlayerBumpPlayer", false);
   public IntegerServerOption MapRemotePlayerVisibility = new IntegerServerOption(this, "MapRemotePlayerVisibility", 1, 3, 1);
   public IntegerServerOption BackupsCount = new IntegerServerOption(this, "BackupsCount", 1, 300, 5);
   public BooleanServerOption BackupsOnStart = new BooleanServerOption(this, "BackupsOnStart", true);
   public BooleanServerOption BackupsOnVersionChange = new BooleanServerOption(this, "BackupsOnVersionChange", true);
   public IntegerServerOption BackupsPeriod = new IntegerServerOption(this, "BackupsPeriod", 0, 1500, 0);
   public BooleanServerOption AntiCheatProtectionType1 = new BooleanServerOption(this, "AntiCheatProtectionType1", true);
   public BooleanServerOption AntiCheatProtectionType2 = new BooleanServerOption(this, "AntiCheatProtectionType2", true);
   public BooleanServerOption AntiCheatProtectionType3 = new BooleanServerOption(this, "AntiCheatProtectionType3", true);
   public BooleanServerOption AntiCheatProtectionType4 = new BooleanServerOption(this, "AntiCheatProtectionType4", true);
   public BooleanServerOption AntiCheatProtectionType5 = new BooleanServerOption(this, "AntiCheatProtectionType5", true);
   public BooleanServerOption AntiCheatProtectionType6 = new BooleanServerOption(this, "AntiCheatProtectionType6", true);
   public BooleanServerOption AntiCheatProtectionType7 = new BooleanServerOption(this, "AntiCheatProtectionType7", true);
   public BooleanServerOption AntiCheatProtectionType8 = new BooleanServerOption(this, "AntiCheatProtectionType8", true);
   public BooleanServerOption AntiCheatProtectionType9 = new BooleanServerOption(this, "AntiCheatProtectionType9", true);
   public BooleanServerOption AntiCheatProtectionType10 = new BooleanServerOption(this, "AntiCheatProtectionType10", true);
   public BooleanServerOption AntiCheatProtectionType11 = new BooleanServerOption(this, "AntiCheatProtectionType11", true);
   public BooleanServerOption AntiCheatProtectionType12 = new BooleanServerOption(this, "AntiCheatProtectionType12", true);
   public BooleanServerOption AntiCheatProtectionType13 = new BooleanServerOption(this, "AntiCheatProtectionType13", true);
   public BooleanServerOption AntiCheatProtectionType14 = new BooleanServerOption(this, "AntiCheatProtectionType14", true);
   public BooleanServerOption AntiCheatProtectionType15 = new BooleanServerOption(this, "AntiCheatProtectionType15", true);
   public BooleanServerOption AntiCheatProtectionType16 = new BooleanServerOption(this, "AntiCheatProtectionType16", true);
   public BooleanServerOption AntiCheatProtectionType17 = new BooleanServerOption(this, "AntiCheatProtectionType17", true);
   public BooleanServerOption AntiCheatProtectionType18 = new BooleanServerOption(this, "AntiCheatProtectionType18", true);
   public BooleanServerOption AntiCheatProtectionType19 = new BooleanServerOption(this, "AntiCheatProtectionType19", true);
   public BooleanServerOption AntiCheatProtectionType20 = new BooleanServerOption(this, "AntiCheatProtectionType20", true);
   public BooleanServerOption AntiCheatProtectionType21 = new BooleanServerOption(this, "AntiCheatProtectionType21", true);
   public BooleanServerOption AntiCheatProtectionType22 = new BooleanServerOption(this, "AntiCheatProtectionType22", true);
   public BooleanServerOption AntiCheatProtectionType23 = new BooleanServerOption(this, "AntiCheatProtectionType23", true);
   public BooleanServerOption AntiCheatProtectionType24 = new BooleanServerOption(this, "AntiCheatProtectionType24", true);
   public DoubleServerOption AntiCheatProtectionType2ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType2ThresholdMultiplier", 1.0, 10.0, 3.0);
   public DoubleServerOption AntiCheatProtectionType3ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType3ThresholdMultiplier", 1.0, 10.0, 1.0);
   public DoubleServerOption AntiCheatProtectionType4ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType4ThresholdMultiplier", 1.0, 10.0, 1.0);
   public DoubleServerOption AntiCheatProtectionType9ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType9ThresholdMultiplier", 1.0, 10.0, 1.0);
   public DoubleServerOption AntiCheatProtectionType15ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType15ThresholdMultiplier", 1.0, 10.0, 1.0);
   public DoubleServerOption AntiCheatProtectionType20ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType20ThresholdMultiplier", 1.0, 10.0, 1.0);
   public DoubleServerOption AntiCheatProtectionType22ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType22ThresholdMultiplier", 1.0, 10.0, 1.0);
   public DoubleServerOption AntiCheatProtectionType24ThresholdMultiplier = new DoubleServerOption(this, "AntiCheatProtectionType24ThresholdMultiplier", 1.0, 10.0, 6.0);
   public static ArrayList<String> cardList = null;

   public ServerOptions() {
      this.publicOptions.clear();
      this.publicOptions.addAll(this.optionByName.keySet());
      this.publicOptions.remove("Password");
      this.publicOptions.remove("RCONPort");
      this.publicOptions.remove("RCONPassword");
      this.publicOptions.remove(this.DiscordToken.getName());
      this.publicOptions.remove(this.DiscordChannel.getName());
      this.publicOptions.remove(this.DiscordChannelID.getName());
      Collections.sort(this.publicOptions);
   }

   private void initOptions() {
      initClientCommandsHelp();
      Iterator var1 = this.options.iterator();

      while(var1.hasNext()) {
         ServerOption var2 = (ServerOption)var1.next();
         var2.asConfigOption().resetToDefault();
      }

   }

   public ArrayList<String> getPublicOptions() {
      return this.publicOptions;
   }

   public ArrayList<ServerOption> getOptions() {
      return this.options;
   }

   public static void initClientCommandsHelp() {
      clientOptionsList = new HashMap();
      clientOptionsList.put("help", Translator.getText("UI_ServerOptionDesc_Help"));
      clientOptionsList.put("changepwd", Translator.getText("UI_ServerOptionDesc_ChangePwd"));
      clientOptionsList.put("roll", Translator.getText("UI_ServerOptionDesc_Roll"));
      clientOptionsList.put("card", Translator.getText("UI_ServerOptionDesc_Card"));
      clientOptionsList.put("safehouse", Translator.getText("UI_ServerOptionDesc_SafeHouse"));
   }

   public void init() {
      this.initOptions();
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var1 = new File(var10002 + File.separator + "Server");
      if (!var1.exists()) {
         var1.mkdirs();
      }

      var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var2 = new File(var10002 + File.separator + "Server" + File.separator + GameServer.ServerName + ".ini");
      if (var2.exists()) {
         try {
            Core.getInstance().loadOptions();
         } catch (IOException var4) {
            var4.printStackTrace();
         }

         if (this.loadServerTextFile(GameServer.ServerName)) {
            this.saveServerTextFile(GameServer.ServerName);
         }
      } else {
         String var10003 = ZomboidFileSystem.instance.getCacheDir();
         this.initSpawnRegionsFile(new File(var10003 + File.separator + "Server" + File.separator + GameServer.ServerName + "_spawnregions.lua"));
         this.saveServerTextFile(GameServer.ServerName);
      }

      LoggerManager.init();
   }

   public void resetRegionFile() {
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var1 = new File(var10002 + File.separator + "Server" + File.separator + GameServer.ServerName + "_spawnregions.lua");
      var1.delete();
      this.initSpawnRegionsFile(var1);
   }

   private void initSpawnRegionsFile(File var1) {
      if (!var1.exists()) {
         DebugLog.log("creating server spawnregions file \"" + var1.getPath() + "\"");

         try {
            var1.createNewFile();
            FileWriter var2 = new FileWriter(var1);
            var2.write("function SpawnRegions()" + System.lineSeparator());
            var2.write("\treturn {" + System.lineSeparator());
            var2.write("\t\t{ name = \"Muldraugh, KY\", file = \"media/maps/Muldraugh, KY/spawnpoints.lua\" }," + System.lineSeparator());
            var2.write("\t\t{ name = \"West Point, KY\", file = \"media/maps/West Point, KY/spawnpoints.lua\" }," + System.lineSeparator());
            var2.write("\t\t{ name = \"Rosewood, KY\", file = \"media/maps/Rosewood, KY/spawnpoints.lua\" }," + System.lineSeparator());
            var2.write("\t\t{ name = \"Riverside, KY\", file = \"media/maps/Riverside, KY/spawnpoints.lua\" }," + System.lineSeparator());
            var2.write("\t\t-- Uncomment the line below to add a custom spawnpoint for this server." + System.lineSeparator());
            String var10001 = GameServer.ServerName;
            var2.write("--\t\t{ name = \"Twiggy's Bar\", serverfile = \"" + var10001 + "_spawnpoints.lua\" }," + System.lineSeparator());
            var2.write("\t}" + System.lineSeparator());
            var2.write("end" + System.lineSeparator());
            var2.close();
            String var10002 = var1.getParent();
            var2 = new FileWriter(var10002 + File.separator + GameServer.ServerName + "_spawnpoints.lua");
            var2.write("function SpawnPoints()" + System.lineSeparator());
            var2.write("\treturn {" + System.lineSeparator());
            var2.write("\t\tunemployed = {" + System.lineSeparator());
            var2.write("\t\t\t{ worldX = 40, worldY = 22, posX = 67, posY = 201 }" + System.lineSeparator());
            var2.write("\t\t}" + System.lineSeparator());
            var2.write("\t}" + System.lineSeparator());
            var2.write("end" + System.lineSeparator());
            var2.close();
         } catch (Exception var3) {
            var3.printStackTrace();
         }

      }
   }

   public String getOption(String var1) {
      ServerOption var2 = this.getOptionByName(var1);
      return var2 == null ? null : var2.asConfigOption().getValueAsString();
   }

   public Boolean getBoolean(String var1) {
      ServerOption var2 = this.getOptionByName(var1);
      return var2 instanceof BooleanServerOption ? (Boolean)((BooleanServerOption)var2).getValueAsObject() : null;
   }

   public Float getFloat(String var1) {
      ServerOption var2 = this.getOptionByName(var1);
      return var2 instanceof DoubleServerOption ? (float)((DoubleServerOption)var2).getValue() : null;
   }

   public Double getDouble(String var1) {
      ServerOption var2 = this.getOptionByName(var1);
      return var2 instanceof DoubleServerOption ? ((DoubleServerOption)var2).getValue() : null;
   }

   public Integer getInteger(String var1) {
      ServerOption var2 = this.getOptionByName(var1);
      return var2 instanceof IntegerServerOption ? ((IntegerServerOption)var2).getValue() : null;
   }

   public void putOption(String var1, String var2) {
      ServerOption var3 = this.getOptionByName(var1);
      if (var3 != null) {
         var3.asConfigOption().parse(var2);
      }

   }

   public void putSaveOption(String var1, String var2) {
      this.putOption(var1, var2);
      this.saveServerTextFile(GameServer.ServerName);
   }

   public String changeOption(String var1, String var2) {
      ServerOption var3 = this.getOptionByName(var1);
      if (var3 == null) {
         return "Option " + var1 + " doesn't exist.";
      } else {
         var3.asConfigOption().parse(var2);
         return !this.saveServerTextFile(GameServer.ServerName) ? "An error as occured." : "Option : " + var1 + " is now : " + var3.asConfigOption().getValueAsString();
      }
   }

   public static ServerOptions getInstance() {
      return instance;
   }

   public static ArrayList<String> getClientCommandList(boolean var0) {
      String var1 = " <LINE> ";
      if (!var0) {
         var1 = "\n";
      }

      if (clientOptionsList == null) {
         initClientCommandsHelp();
      }

      ArrayList var2 = new ArrayList();
      Iterator var3 = clientOptionsList.keySet().iterator();
      String var4 = null;
      var2.add("List of commands : " + var1);

      while(var3.hasNext()) {
         var4 = (String)var3.next();
         var2.add("* " + var4 + " : " + (String)clientOptionsList.get(var4) + (var3.hasNext() ? var1 : ""));
      }

      return var2;
   }

   public static String getRandomCard() {
      if (cardList == null) {
         cardList = new ArrayList();
         cardList.add("the Ace of Clubs");
         cardList.add("a Two of Clubs");
         cardList.add("a Three of Clubs");
         cardList.add("a Four of Clubs");
         cardList.add("a Five of Clubs");
         cardList.add("a Six of Clubs");
         cardList.add("a Seven of Clubs");
         cardList.add("a Height of Clubs");
         cardList.add("a Nine of Clubs");
         cardList.add("a Ten of Clubs");
         cardList.add("the Jack of Clubs");
         cardList.add("the Queen of Clubs");
         cardList.add("the King of Clubs");
         cardList.add("the Ace of Diamonds");
         cardList.add("a Two of Diamonds");
         cardList.add("a Three of Diamonds");
         cardList.add("a Four of Diamonds");
         cardList.add("a Five of Diamonds");
         cardList.add("a Six of Diamonds");
         cardList.add("a Seven of Diamonds");
         cardList.add("a Height of Diamonds");
         cardList.add("a Nine of Diamonds");
         cardList.add("a Ten of Diamonds");
         cardList.add("the Jack of Diamonds");
         cardList.add("the Queen of Diamonds");
         cardList.add("the King of Diamonds");
         cardList.add("the Ace of Hearts");
         cardList.add("a Two of Hearts");
         cardList.add("a Three of Hearts");
         cardList.add("a Four of Hearts");
         cardList.add("a Five of Hearts");
         cardList.add("a Six of Hearts");
         cardList.add("a Seven of Hearts");
         cardList.add("a Height of Hearts");
         cardList.add("a Nine of Hearts");
         cardList.add("a Ten of Hearts");
         cardList.add("the Jack of Hearts");
         cardList.add("the Queen of Hearts");
         cardList.add("the King of Hearts");
         cardList.add("the Ace of Spades");
         cardList.add("a Two of Spades");
         cardList.add("a Three of Spades");
         cardList.add("a Four of Spades");
         cardList.add("a Five of Spades");
         cardList.add("a Six of Spades");
         cardList.add("a Seven of Spades");
         cardList.add("a Height of Spades");
         cardList.add("a Nine of Spades");
         cardList.add("a Ten of Spades");
         cardList.add("the Jack of Spades");
         cardList.add("the Queen of Spades");
         cardList.add("the King of Spades");
      }

      return (String)cardList.get(Rand.Next(cardList.size()));
   }

   public void addOption(ServerOption var1) {
      if (this.optionByName.containsKey(var1.asConfigOption().getName())) {
         throw new IllegalArgumentException();
      } else {
         this.options.add(var1);
         this.optionByName.put(var1.asConfigOption().getName(), var1);
      }
   }

   public int getNumOptions() {
      return this.options.size();
   }

   public ServerOption getOptionByIndex(int var1) {
      return (ServerOption)this.options.get(var1);
   }

   public ServerOption getOptionByName(String var1) {
      return (ServerOption)this.optionByName.get(var1);
   }

   public boolean loadServerTextFile(String var1) {
      ConfigFile var2 = new ConfigFile();
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var3 = var10000 + File.separator + "Server" + File.separator + var1 + ".ini";
      if (var2.read(var3)) {
         Iterator var4 = var2.getOptions().iterator();

         while(var4.hasNext()) {
            ConfigOption var5 = (ConfigOption)var4.next();
            ServerOption var6 = (ServerOption)this.optionByName.get(var5.getName());
            if (var6 != null) {
               var6.asConfigOption().parse(var5.getValueAsString());
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean saveServerTextFile(String var1) {
      ConfigFile var2 = new ConfigFile();
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var3 = var10000 + File.separator + "Server" + File.separator + var1 + ".ini";
      ArrayList var4 = new ArrayList();
      Iterator var5 = this.options.iterator();

      while(var5.hasNext()) {
         ServerOption var6 = (ServerOption)var5.next();
         var4.add(var6.asConfigOption());
      }

      return var2.write(var3, 0, var4);
   }

   public int getMaxPlayers() {
      return Math.min(100, getInstance().MaxPlayers.getValue());
   }

   public static class BooleanServerOption extends BooleanConfigOption implements ServerOption {
      public BooleanServerOption(ServerOptions var1, String var2, boolean var3) {
         super(var2, var3);
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getTooltip() {
         return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
      }
   }

   public static class StringServerOption extends StringConfigOption implements ServerOption {
      public StringServerOption(ServerOptions var1, String var2, String var3, int var4) {
         super(var2, var3, var4);
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getTooltip() {
         return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
      }
   }

   public static class TextServerOption extends StringConfigOption implements ServerOption {
      public TextServerOption(ServerOptions var1, String var2, String var3, int var4) {
         super(var2, var3, var4);
         var1.addOption(this);
      }

      public String getType() {
         return "text";
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getTooltip() {
         return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
      }
   }

   public static class IntegerServerOption extends IntegerConfigOption implements ServerOption {
      public IntegerServerOption(ServerOptions var1, String var2, int var3, int var4, int var5) {
         super(var2, var3, var4, var5);
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getTooltip() {
         String var1 = Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
         String var2 = Translator.getText("Sandbox_MinMaxDefault", this.min, this.max, this.defaultValue);
         if (var1 == null) {
            return var2;
         } else {
            return var2 == null ? var1 : var1 + "\\n" + var2;
         }
      }
   }

   public static class DoubleServerOption extends DoubleConfigOption implements ServerOption {
      public DoubleServerOption(ServerOptions var1, String var2, double var3, double var5, double var7) {
         super(var2, var3, var5, var7);
         var1.addOption(this);
      }

      public ConfigOption asConfigOption() {
         return this;
      }

      public String getTooltip() {
         String var1 = Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
         String var2 = Translator.getText("Sandbox_MinMaxDefault", String.format("%.02f", this.min), String.format("%.02f", this.max), String.format("%.02f", this.defaultValue));
         if (var1 == null) {
            return var2;
         } else {
            return var2 == null ? var1 : var1 + "\\n" + var2;
         }
      }
   }

   public interface ServerOption {
      ConfigOption asConfigOption();

      String getTooltip();
   }
}
