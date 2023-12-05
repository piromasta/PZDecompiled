package zombie.radio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.chat.ChatMessage;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.VoiceManagerData;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugType;
import zombie.inventory.types.Radio;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.radio.StorySounds.SLSoundManager;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.radio.media.RecordedMedia;
import zombie.radio.scripting.RadioChannel;
import zombie.radio.scripting.RadioScript;
import zombie.radio.scripting.RadioScriptManager;

public final class ZomboidRadio {
   public static final String SAVE_FILE = "RADIO_SAVE.txt";
   private final ArrayList<WaveSignalDevice> devices = new ArrayList();
   private final ArrayList<WaveSignalDevice> broadcastDevices = new ArrayList();
   private RadioScriptManager scriptManager;
   private int DaysSinceStart = 0;
   private int lastRecordedHour;
   private final String[] playerLastLine = new String[4];
   private final Map<Integer, String> channelNames = new HashMap();
   private final Map<String, Map<Integer, String>> categorizedChannels = new HashMap();
   private final List<Integer> knownFrequencies = new ArrayList();
   private RadioDebugConsole debugConsole;
   private boolean hasRecievedServerData = false;
   private SLSoundManager storySoundManager = null;
   private static final String[] staticSounds = new String[]{"<bzzt>", "<fzzt>", "<wzzt>", "<szzt>"};
   public static boolean DEBUG_MODE = false;
   public static boolean DEBUG_XML = false;
   public static boolean DEBUG_SOUND = false;
   public static boolean POST_RADIO_SILENCE = false;
   public static boolean DISABLE_BROADCASTING = false;
   private static ZomboidRadio instance;
   private static RecordedMedia recordedMedia;
   public static boolean LOUISVILLE_OBFUSCATION = false;
   private String lastSaveFile;
   private String lastSaveContent;
   private HashMap<Integer, FreqListEntry> freqlist = new HashMap();
   private boolean hasAppliedRangeDistortion = false;
   private StringBuilder stringBuilder = new StringBuilder();
   private boolean hasAppliedInterference = false;
   private static int[] obfuscateChannels = new int[]{200, 201, 204, 93200, 98000, 101200};

   public static boolean hasInstance() {
      return instance != null;
   }

   public static ZomboidRadio getInstance() {
      if (instance == null) {
         instance = new ZomboidRadio();
      }

      return instance;
   }

   private ZomboidRadio() {
      this.lastRecordedHour = GameTime.instance.getHour();
      SLSoundManager.DEBUG = DEBUG_SOUND;

      for(int var1 = 0; var1 < staticSounds.length; ++var1) {
         ChatElement.addNoLogText(staticSounds[var1]);
      }

      ChatElement.addNoLogText("~");
      recordedMedia = new RecordedMedia();
   }

   public static boolean isStaticSound(String var0) {
      if (var0 != null) {
         String[] var1 = staticSounds;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            String var4 = var1[var3];
            if (var0.equals(var4)) {
               return true;
            }
         }
      }

      return false;
   }

   public RadioScriptManager getScriptManager() {
      return this.scriptManager;
   }

   public int getDaysSinceStart() {
      return this.DaysSinceStart;
   }

   public ArrayList<WaveSignalDevice> getDevices() {
      return this.devices;
   }

   public ArrayList<WaveSignalDevice> getBroadcastDevices() {
      return this.broadcastDevices;
   }

   public void setHasRecievedServerData(boolean var1) {
      this.hasRecievedServerData = var1;
   }

   public void addChannelName(String var1, int var2, String var3) {
      this.addChannelName(var1, var2, var3, true);
   }

   public void addChannelName(String var1, int var2, String var3, boolean var4) {
      if (var4 || !this.channelNames.containsKey(var2)) {
         if (!this.categorizedChannels.containsKey(var3)) {
            this.categorizedChannels.put(var3, new HashMap());
         }

         ((Map)this.categorizedChannels.get(var3)).put(var2, var1);
         this.channelNames.put(var2, var1);
         this.knownFrequencies.add(var2);
      }

   }

   public void removeChannelName(int var1) {
      if (this.channelNames.containsKey(var1)) {
         this.channelNames.remove(var1);
         Iterator var2 = this.categorizedChannels.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            if (((Map)var3.getValue()).containsKey(var1)) {
               ((Map)var3.getValue()).remove(var1);
            }
         }
      }

   }

   public Map<Integer, String> GetChannelList(String var1) {
      return this.categorizedChannels.containsKey(var1) ? (Map)this.categorizedChannels.get(var1) : null;
   }

   public String getChannelName(int var1) {
      return this.channelNames.containsKey(var1) ? (String)this.channelNames.get(var1) : null;
   }

   public int getRandomFrequency() {
      return this.getRandomFrequency(88000, 108000);
   }

   public int getRandomFrequency(int var1, int var2) {
      int var3 = 91100;

      do {
         var3 = Rand.Next(var1, var2);
         var3 /= 200;
         var3 *= 200;
      } while(this.knownFrequencies.contains(var3));

      return var3;
   }

   public Map<String, Map<Integer, String>> getFullChannelList() {
      return this.categorizedChannels;
   }

   public void WriteRadioServerDataPacket(ByteBufferWriter var1) {
      var1.putInt(this.categorizedChannels.size());
      Iterator var2 = this.categorizedChannels.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         GameWindow.WriteString(var1.bb, (String)var3.getKey());
         var1.putInt(((Map)var3.getValue()).size());
         Iterator var4 = ((Map)var3.getValue()).entrySet().iterator();

         while(var4.hasNext()) {
            Map.Entry var5 = (Map.Entry)var4.next();
            var1.putInt((Integer)var5.getKey());
            GameWindow.WriteString(var1.bb, (String)var5.getValue());
         }
      }

      var1.putByte((byte)(POST_RADIO_SILENCE ? 1 : 0));
   }

   public void Init(int var1) {
      POST_RADIO_SILENCE = false;
      boolean var2 = false;
      boolean var3 = DebugLog.isEnabled(DebugType.Radio);
      if (var3) {
         DebugLog.Radio.println("");
         DebugLog.Radio.println("################## Radio Init ##################");
      }

      RadioAPI.getInstance();
      recordedMedia.init();
      this.lastRecordedHour = GameTime.instance.getHour();
      GameMode var4 = this.getGameMode();
      if (DEBUG_MODE && !var4.equals(GameMode.Server)) {
         DebugLog.setLogEnabled(DebugType.Radio, true);
         this.debugConsole = new RadioDebugConsole();
      }

      if (var4.equals(GameMode.Client)) {
         GameClient.sendRadioServerDataRequest();
         if (var3) {
            DebugLog.Radio.println("Radio (Client) loaded.");
            DebugLog.Radio.println("################################################");
         }

         this.scriptManager = null;
      } else {
         this.scriptManager = RadioScriptManager.getInstance();
         this.scriptManager.init(var1);

         try {
            if (!Core.getInstance().isNoSave()) {
               ZomboidFileSystem.instance.getFileInCurrentSave("radio", "data").mkdirs();
            }

            ArrayList var5 = RadioData.fetchAllRadioData();
            Iterator var6 = var5.iterator();

            label109:
            while(var6.hasNext()) {
               RadioData var7 = (RadioData)var6.next();
               Iterator var8 = var7.getRadioChannels().iterator();

               while(true) {
                  while(true) {
                     if (!var8.hasNext()) {
                        continue label109;
                     }

                     RadioChannel var9 = (RadioChannel)var8.next();
                     ObfuscateChannelCheck(var9);
                     RadioChannel var10 = null;
                     if (this.scriptManager.getChannels().containsKey(var9.GetFrequency())) {
                        var10 = (RadioChannel)this.scriptManager.getChannels().get(var9.GetFrequency());
                     }

                     if (var10 != null && (!var10.getRadioData().isVanilla() || var9.getRadioData().isVanilla())) {
                        if (var3) {
                           DebugLogStream var10000 = DebugLog.Radio;
                           String var10001 = var9.GetName();
                           var10000.println("Unable to add channel: " + var10001 + ", frequency '" + var9.GetFrequency() + "' taken.");
                        }
                     } else {
                        this.scriptManager.AddChannel(var9, true);
                     }
                  }
               }
            }

            LuaEventManager.triggerEvent("OnLoadRadioScripts", this.scriptManager, var1 == -1);
            if (var1 == -1) {
               if (var3) {
                  DebugLog.Radio.println("Radio setting new game start times");
               }

               SandboxOptions var12 = SandboxOptions.instance;
               int var14 = var12.TimeSinceApo.getValue() - 1;
               if (var14 < 0) {
                  var14 = 0;
               }

               if (var3) {
                  DebugLog.log(DebugType.Radio, "Time since the apocalypse: " + var12.TimeSinceApo.getValue());
               }

               if (var14 > 0) {
                  this.DaysSinceStart = (int)((float)var14 * 30.5F);
                  if (var3) {
                     DebugLog.Radio.println("Time since the apocalypse in days: " + this.DaysSinceStart);
                  }

                  this.scriptManager.simulateScriptsUntil(this.DaysSinceStart, true);
               }

               this.checkGameModeSpecificStart();
            } else {
               boolean var13 = this.Load();
               if (!var13) {
                  SandboxOptions var15 = SandboxOptions.instance;
                  int var16 = var15.TimeSinceApo.getValue() - 1;
                  if (var16 < 0) {
                     var16 = 0;
                  }

                  this.DaysSinceStart = (int)((float)var16 * 30.5F);
                  this.DaysSinceStart += GameTime.instance.getNightsSurvived();
               }

               if (this.DaysSinceStart > 0) {
                  this.scriptManager.simulateScriptsUntil(this.DaysSinceStart, false);
               }
            }

            var2 = true;
         } catch (Exception var11) {
            ExceptionLogger.logException(var11);
         }

         if (var3) {
            if (var2) {
               DebugLog.Radio.println("Radio loaded.");
            }

            DebugLog.Radio.println("################################################");
            DebugLog.Radio.println("");
         }
      }
   }

   private void checkGameModeSpecificStart() {
      Iterator var1;
      Map.Entry var2;
      if (Core.GameMode.equals("Initial Infection")) {
         var1 = this.scriptManager.getChannels().entrySet().iterator();

         while(var1.hasNext()) {
            var2 = (Map.Entry)var1.next();
            RadioScript var3 = ((RadioChannel)var2.getValue()).getRadioScript("init_infection");
            if (var3 != null) {
               var3.clearExitOptions();
               var3.AddExitOption(((RadioChannel)var2.getValue()).getCurrentScript().GetName(), 100, 0);
               ((RadioChannel)var2.getValue()).setActiveScript("init_infection", this.DaysSinceStart);
            } else {
               ((RadioChannel)var2.getValue()).getCurrentScript().setStartDayStamp(this.DaysSinceStart + 1);
            }
         }
      } else if (Core.GameMode.equals("Six Months Later")) {
         var1 = this.scriptManager.getChannels().entrySet().iterator();

         while(var1.hasNext()) {
            var2 = (Map.Entry)var1.next();
            if (((RadioChannel)var2.getValue()).GetName().equals("Classified M1A1")) {
               ((RadioChannel)var2.getValue()).setActiveScript("numbers", this.DaysSinceStart);
            } else if (((RadioChannel)var2.getValue()).GetName().equals("NNR Radio")) {
               ((RadioChannel)var2.getValue()).setActiveScript("pastor", this.DaysSinceStart);
            }
         }
      }

   }

   public void Save() throws FileNotFoundException, IOException {
      if (!Core.getInstance().isNoSave()) {
         GameMode var1 = this.getGameMode();
         if ((var1.equals(GameMode.Server) || var1.equals(GameMode.SinglePlayer)) && this.scriptManager != null) {
            File var2 = ZomboidFileSystem.instance.getFileInCurrentSave("radio", "data");
            if (var2.exists() && var2.isDirectory()) {
               String var3 = ZomboidFileSystem.instance.getFileNameInCurrentSave("radio", "data", "RADIO_SAVE.txt");

               String var4;
               try {
                  StringWriter var5 = new StringWriter(1024);

                  try {
                     int var10001 = this.DaysSinceStart;
                     var5.write("DaysSinceStart = " + var10001 + System.lineSeparator());
                     boolean var17 = LOUISVILLE_OBFUSCATION;
                     var5.write("LvObfuscation = " + var17 + System.lineSeparator());
                     this.scriptManager.Save(var5);
                     var4 = var5.toString();
                  } catch (Throwable var14) {
                     try {
                        var5.close();
                     } catch (Throwable var9) {
                        var14.addSuppressed(var9);
                     }

                     throw var14;
                  }

                  var5.close();
               } catch (IOException var15) {
                  ExceptionLogger.logException(var15);
                  return;
               }

               if (var3.equals(this.lastSaveFile) && var4.equals(this.lastSaveContent)) {
                  return;
               }

               this.lastSaveFile = var3;
               this.lastSaveContent = var4;
               File var16 = new File(var3);
               if (DebugLog.isEnabled(DebugType.Radio)) {
                  DebugLog.Radio.println("Saving radio: " + var3);
               }

               try {
                  FileWriter var6 = new FileWriter(var16, false);

                  try {
                     var6.write(var4);
                  } catch (Throwable var12) {
                     try {
                        var6.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }

                     throw var12;
                  }

                  var6.close();
               } catch (Exception var13) {
                  ExceptionLogger.logException(var13);
               }
            }
         }

         if (recordedMedia != null) {
            try {
               recordedMedia.save();
            } catch (Exception var10) {
               var10.printStackTrace();
            }
         }

      }
   }

   public boolean Load() throws FileNotFoundException, IOException {
      boolean var1 = false;
      GameMode var2 = this.getGameMode();
      if (var2.equals(GameMode.Server) || var2.equals(GameMode.SinglePlayer)) {
         Iterator var3 = this.scriptManager.getChannels().entrySet().iterator();

         while(var3.hasNext()) {
            Map.Entry var4 = (Map.Entry)var3.next();
            ((RadioChannel)var4.getValue()).setActiveScriptNull();
         }

         ArrayList var25 = new ArrayList();
         String var26 = ZomboidFileSystem.instance.getFileNameInCurrentSave("radio", "data", "RADIO_SAVE.txt");
         File var5 = new File(var26);
         if (!var5.exists()) {
            return false;
         } else {
            if (DebugLog.isEnabled(DebugType.Radio)) {
               DebugLog.log(DebugType.Radio, "Loading radio save:" + var26);
            }

            try {
               FileReader var6 = new FileReader(var5);

               try {
                  BufferedReader var7 = new BufferedReader(var6);

                  String var8;
                  try {
                     while((var8 = var7.readLine()) != null) {
                        var8 = var8.trim();
                        if (!var8.startsWith("DaysSinceStart") && !var8.startsWith("LvObfuscation")) {
                           var25.add(var8);
                        } else {
                           String[] var9;
                           if (var8.startsWith("DaysSinceStart")) {
                              var9 = var8.split("=");
                              this.DaysSinceStart = Integer.parseInt(var9[1].trim());
                           }

                           if (var8.startsWith("LvObfuscation")) {
                              var9 = var8.split("=");
                              LOUISVILLE_OBFUSCATION = Boolean.parseBoolean(var9[1].trim());
                           }
                        }
                     }
                  } catch (Throwable var22) {
                     try {
                        var7.close();
                     } catch (Throwable var19) {
                        var22.addSuppressed(var19);
                     }

                     throw var22;
                  }

                  var7.close();
               } catch (Throwable var23) {
                  try {
                     var6.close();
                  } catch (Throwable var18) {
                     var23.addSuppressed(var18);
                  }

                  throw var23;
               }

               var6.close();
            } catch (Exception var24) {
               var24.printStackTrace();
               return false;
            }

            boolean var27;
            try {
               DebugLog.log("Radio Loading channels...");
               this.scriptManager.Load(var25);
               return var1;
            } catch (Exception var20) {
               var20.printStackTrace();
               var27 = false;
            } finally {
               var1 = true;
            }

            return var27;
         }
      } else {
         return var1;
      }
   }

   public void Reset() {
      instance = null;
      if (this.scriptManager != null) {
         this.scriptManager.reset();
      }

   }

   public void UpdateScripts(int var1, int var2) {
      GameMode var3 = this.getGameMode();
      if (var3.equals(GameMode.Server) || var3.equals(GameMode.SinglePlayer)) {
         if (var1 == 0 && this.lastRecordedHour != 0) {
            ++this.DaysSinceStart;
         }

         this.lastRecordedHour = var1;
         if (this.scriptManager != null) {
            this.scriptManager.UpdateScripts(this.DaysSinceStart, var1, var2);
         }

         try {
            this.Save();
         } catch (Exception var6) {
            System.out.println(var6.getMessage());
         }
      }

      if (var3.equals(GameMode.Client) || var3.equals(GameMode.SinglePlayer)) {
         for(int var4 = 0; var4 < this.devices.size(); ++var4) {
            WaveSignalDevice var5 = (WaveSignalDevice)this.devices.get(var4);
            if (var5.getDeviceData().getIsTurnedOn() && var5.HasPlayerInRange()) {
               var5.getDeviceData().TriggerPlayerListening(true);
            }
         }
      }

      if (var3.equals(GameMode.Client) && !this.hasRecievedServerData) {
         GameClient.sendRadioServerDataRequest();
      }

   }

   public void render() {
      GameMode var1 = this.getGameMode();
      if (DEBUG_MODE && !var1.equals(GameMode.Server) && this.debugConsole != null) {
         this.debugConsole.render();
      }

      if (!var1.equals(GameMode.Server) && this.storySoundManager != null) {
         this.storySoundManager.render();
      }

   }

   private void addFrequencyListEntry(boolean var1, DeviceData var2, int var3, int var4) {
      if (var2 != null) {
         if (!this.freqlist.containsKey(var2.getChannel())) {
            this.freqlist.put(var2.getChannel(), new FreqListEntry(var1, var2, var3, var4));
         } else if (((FreqListEntry)this.freqlist.get(var2.getChannel())).deviceData.getTransmitRange() < var2.getTransmitRange()) {
            FreqListEntry var5 = (FreqListEntry)this.freqlist.get(var2.getChannel());
            var5.isInvItem = var1;
            var5.deviceData = var2;
            var5.sourceX = var3;
            var5.sourceY = var4;
         }

      }
   }

   public void update() {
      this.LouisvilleObfuscationCheck();
      if (DEBUG_MODE && this.debugConsole != null) {
         this.debugConsole.update();
      }

      GameMode var1 = this.getGameMode();
      if ((var1.equals(GameMode.Server) || var1.equals(GameMode.SinglePlayer)) && this.DaysSinceStart > 14 && !POST_RADIO_SILENCE) {
         POST_RADIO_SILENCE = true;
         if (GameServer.bServer) {
            GameServer.sendRadioPostSilence();
         }
      }

      if (!var1.equals(GameMode.Server) && this.storySoundManager != null) {
         this.storySoundManager.update(this.DaysSinceStart, GameTime.instance.getHour(), GameTime.instance.getMinutes());
      }

      if ((var1.equals(GameMode.Server) || var1.equals(GameMode.SinglePlayer)) && this.scriptManager != null) {
         this.scriptManager.update();
      }

      if (var1.equals(GameMode.SinglePlayer) || var1.equals(GameMode.Client)) {
         for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
            IsoPlayer var3 = IsoPlayer.players[var2];
            if (var3 != null && var3.getLastSpokenLine() != null && (this.playerLastLine[var2] == null || !this.playerLastLine[var2].equals(var3.getLastSpokenLine()))) {
               String var4 = var3.getLastSpokenLine();
               this.playerLastLine[var2] = var4;
               if (!var1.equals(GameMode.Client) || (!var3.accessLevel.equals("admin") && !var3.accessLevel.equals("gm") && !var3.accessLevel.equals("overseer") && !var3.accessLevel.equals("moderator") || !ServerOptions.instance.DisableRadioStaff.getValue() && (!ServerOptions.instance.DisableRadioAdmin.getValue() || !var3.accessLevel.equals("admin")) && (!ServerOptions.instance.DisableRadioGM.getValue() || !var3.accessLevel.equals("gm")) && (!ServerOptions.instance.DisableRadioOverseer.getValue() || !var3.accessLevel.equals("overseer")) && (!ServerOptions.instance.DisableRadioModerator.getValue() || !var3.accessLevel.equals("moderator"))) && (!ServerOptions.instance.DisableRadioInvisible.getValue() || !var3.isInvisible())) {
                  this.freqlist.clear();
                  if (!GameClient.bClient && !GameServer.bServer) {
                     for(int var9 = 0; var9 < IsoPlayer.numPlayers; ++var9) {
                        this.checkPlayerForDevice(IsoPlayer.players[var9], var3);
                     }
                  } else if (GameClient.bClient) {
                     ArrayList var5 = GameClient.instance.getPlayers();

                     for(int var6 = 0; var6 < var5.size(); ++var6) {
                        this.checkPlayerForDevice((IsoPlayer)var5.get(var6), var3);
                     }
                  }

                  Iterator var10 = this.broadcastDevices.iterator();

                  while(var10.hasNext()) {
                     WaveSignalDevice var12 = (WaveSignalDevice)var10.next();
                     if (var12 != null && var12.getDeviceData() != null && var12.getDeviceData().getIsTurnedOn() && var12.getDeviceData().getIsTwoWay() && var12.HasPlayerInRange() && !var12.getDeviceData().getMicIsMuted() && this.GetDistance((int)var3.getX(), (int)var3.getY(), (int)var12.getX(), (int)var12.getY()) < var12.getDeviceData().getMicRange()) {
                        this.addFrequencyListEntry(true, var12.getDeviceData(), (int)var12.getX(), (int)var12.getY());
                     }
                  }

                  if (this.freqlist.size() > 0) {
                     Color var11 = var3.getSpeakColour();
                     Iterator var13 = this.freqlist.entrySet().iterator();

                     while(var13.hasNext()) {
                        Map.Entry var7 = (Map.Entry)var13.next();
                        FreqListEntry var8 = (FreqListEntry)var7.getValue();
                        this.SendTransmission(var8.sourceX, var8.sourceY, (Integer)var7.getKey(), this.playerLastLine[var2], (String)null, (String)null, var11.r, var11.g, var11.b, var8.deviceData.getTransmitRange(), false);
                     }
                  }
               }
            }
         }
      }

   }

   private void checkPlayerForDevice(IsoPlayer var1, IsoPlayer var2) {
      boolean var3 = var1 == var2;
      if (var1 != null) {
         Radio var4 = var1.getEquipedRadio();
         if (var4 != null && var4.getDeviceData() != null && var4.getDeviceData().getIsPortable() && var4.getDeviceData().getIsTwoWay() && var4.getDeviceData().getIsTurnedOn() && !var4.getDeviceData().getMicIsMuted() && (var3 || this.GetDistance((int)var2.getX(), (int)var2.getY(), (int)var1.getX(), (int)var1.getY()) < var4.getDeviceData().getMicRange())) {
            this.addFrequencyListEntry(true, var4.getDeviceData(), (int)var1.getX(), (int)var1.getY());
         }
      }

   }

   private boolean DeviceInRange(int var1, int var2, int var3, int var4, int var5) {
      return var1 > var3 - var5 && var1 < var3 + var5 && var2 > var4 - var5 && var2 < var4 + var5 && Math.sqrt(Math.pow((double)(var1 - var3), 2.0) + Math.pow((double)(var2 - var4), 2.0)) < (double)var5;
   }

   private int GetDistance(int var1, int var2, int var3, int var4) {
      return (int)Math.sqrt(Math.pow((double)(var1 - var3), 2.0) + Math.pow((double)(var2 - var4), 2.0));
   }

   private void DistributeToPlayerOnClient(IsoPlayer var1, int var2, int var3, int var4, String var5, String var6, String var7, float var8, float var9, float var10, int var11, boolean var12) {
      if (var1 != null && var1.getOnlineID() != -1) {
         VoiceManagerData var13 = VoiceManagerData.get(var1.getOnlineID());
         synchronized(var13.radioData) {
            Iterator var15 = var13.radioData.iterator();

            while(var15.hasNext()) {
               VoiceManagerData.RadioData var16 = (VoiceManagerData.RadioData)var15.next();
               if (var16.isReceivingAvailable(var4)) {
                  this.DistributeToPlayerInternal(var16.getDeviceData().getParent(), var1, var2, var3, var5, var6, var7, var8, var9, var10, var11);
               }
            }
         }
      }

   }

   private void DistributeToPlayer(IsoPlayer var1, int var2, int var3, int var4, String var5, String var6, String var7, float var8, float var9, float var10, int var11, boolean var12) {
      if (var1 != null) {
         Radio var13 = var1.getEquipedRadio();
         if (var13 != null && var13.getDeviceData() != null && var13.getDeviceData().getIsPortable() && var13.getDeviceData().getIsTurnedOn() && var13.getDeviceData().getChannel() == var4) {
            if (var13.getDeviceData().getDeviceVolume() <= 0.0F) {
               return;
            }

            if (var13.getDeviceData().isPlayingMedia() || var13.getDeviceData().isNoTransmit()) {
               return;
            }

            this.DistributeToPlayerInternal(var13, var1, var2, var3, var5, var6, var7, var8, var9, var10, var11);
         }
      }

   }

   private void DistributeToPlayerInternal(WaveSignalDevice var1, IsoPlayer var2, int var3, int var4, String var5, String var6, String var7, float var8, float var9, float var10, int var11) {
      boolean var12 = false;
      int var13 = -1;
      if (var11 < 0) {
         var12 = true;
      } else {
         var13 = this.GetDistance((int)var2.getX(), (int)var2.getY(), var3, var4);
         if (var13 > 3 && var13 < var11) {
            var12 = true;
         }
      }

      if (var12) {
         if (var11 > 0) {
            this.hasAppliedRangeDistortion = false;
            var5 = this.doDeviceRangeDistortion(var5, var11, var13);
         }

         if (!this.hasAppliedRangeDistortion) {
            var1.AddDeviceText(var2, var5, var8, var9, var10, var6, var7, var13);
         } else {
            var1.AddDeviceText(var5, 0.5F, 0.5F, 0.5F, var6, var7, var13);
         }
      }

   }

   private void DistributeTransmission(int var1, int var2, int var3, String var4, String var5, String var6, float var7, float var8, float var9, int var10, boolean var11) {
      int var16;
      if (!var11) {
         if (!GameClient.bClient && !GameServer.bServer) {
            for(var16 = 0; var16 < IsoPlayer.numPlayers; ++var16) {
               this.DistributeToPlayer(IsoPlayer.players[var16], var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
            }
         } else if (GameClient.bClient) {
            IsoPlayer[] var12 = IsoPlayer.players;
            int var13 = var12.length;

            for(int var14 = 0; var14 < var13; ++var14) {
               IsoPlayer var15 = var12[var14];
               this.DistributeToPlayerOnClient(var15, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
            }

            return;
         }
      }

      if (this.devices.size() != 0) {
         for(var16 = 0; var16 < this.devices.size(); ++var16) {
            WaveSignalDevice var17 = (WaveSignalDevice)this.devices.get(var16);
            if (var17 != null && var17.getDeviceData() != null && var17.getDeviceData().getIsTurnedOn() && var11 == var17.getDeviceData().getIsTelevision()) {
               if (var17.getDeviceData().isPlayingMedia() || var17.getDeviceData().isNoTransmit()) {
                  return;
               }

               if (var3 == var17.getDeviceData().getChannel()) {
                  boolean var18 = false;
                  if (var10 == -1) {
                     var18 = true;
                  } else if (var1 != (int)var17.getX() && var2 != (int)var17.getY()) {
                     var18 = true;
                  }

                  if (var18) {
                     int var19 = -1;
                     if (var10 > 0) {
                        this.hasAppliedRangeDistortion = false;
                        var19 = this.GetDistance((int)var17.getX(), (int)var17.getY(), var1, var2);
                        var4 = this.doDeviceRangeDistortion(var4, var10, var19);
                     }

                     if (!this.hasAppliedRangeDistortion) {
                        var17.AddDeviceText(var4, var7, var8, var9, var5, var6, var19);
                     } else {
                        var17.AddDeviceText(var4, 0.5F, 0.5F, 0.5F, var5, var6, var19);
                     }
                  }
               }
            }
         }

      }
   }

   private String doDeviceRangeDistortion(String var1, int var2, int var3) {
      float var4 = (float)var2 * 0.9F;
      if (var4 < (float)var2 && (float)var3 > var4) {
         float var5 = 100.0F * (((float)var3 - var4) / ((float)var2 - var4));
         var1 = this.scrambleString(var1, (int)var5, false);
         this.hasAppliedRangeDistortion = true;
      }

      return var1;
   }

   public GameMode getGameMode() {
      if (!GameClient.bClient && !GameServer.bServer) {
         return GameMode.SinglePlayer;
      } else {
         return GameServer.bServer ? GameMode.Server : GameMode.Client;
      }
   }

   public String getRandomBzztFzzt() {
      int var1 = Rand.Next(staticSounds.length);
      return staticSounds[var1];
   }

   private String applyWeatherInterference(String var1, int var2) {
      if (ClimateManager.getInstance().getWeatherInterference() <= 0.0F) {
         return var1;
      } else {
         int var3 = (int)(ClimateManager.getInstance().getWeatherInterference() * 100.0F);
         return this.scrambleString(var1, var3, var2 == -1);
      }
   }

   private String scrambleString(String var1, int var2, boolean var3) {
      return this.scrambleString(var1, var2, var3, (String)null);
   }

   public String scrambleString(String var1, int var2, boolean var3, String var4) {
      this.hasAppliedInterference = false;
      StringBuilder var5 = this.stringBuilder;
      var5.setLength(0);
      if (var2 <= 0) {
         return var1;
      } else if (var2 >= 100) {
         return var4 != null ? var4 : this.getRandomBzztFzzt();
      } else {
         this.hasAppliedInterference = true;
         String var9;
         int var10;
         if (var3) {
            char[] var6 = var1.toCharArray();
            boolean var7 = false;
            boolean var8 = false;
            var9 = "";

            for(var10 = 0; var10 < var6.length; ++var10) {
               char var11 = var6[var10];
               if (var8) {
                  var9 = var9 + var11;
                  if (var11 == ']') {
                     var5.append(var9);
                     var9 = "";
                     var8 = false;
                  }
               } else if (var11 == '[' || Character.isWhitespace(var11) && var10 > 0 && !Character.isWhitespace(var6[var10 - 1])) {
                  int var12 = Rand.Next(100);
                  if (var12 > var2) {
                     var5.append(var9).append(" ");
                     var7 = false;
                  } else if (!var7) {
                     var5.append(var4 != null ? var4 : this.getRandomBzztFzzt()).append(" ");
                     var7 = true;
                  }

                  if (var11 == '[') {
                     var9 = "[";
                     var8 = true;
                  } else {
                     var9 = "";
                  }
               } else {
                  var9 = var9 + var11;
               }
            }

            if (var9 != null && var9.length() > 0) {
               var5.append(var9);
            }
         } else {
            boolean var13 = false;
            String[] var14 = var1.split("\\s+");

            for(int var15 = 0; var15 < var14.length; ++var15) {
               var9 = var14[var15];
               var10 = Rand.Next(100);
               if (var10 > var2) {
                  var5.append(var9).append(" ");
                  var13 = false;
               } else if (!var13) {
                  var5.append(var4 != null ? var4 : this.getRandomBzztFzzt()).append(" ");
                  var13 = true;
               }
            }
         }

         return var5.toString();
      }
   }

   public void ReceiveTransmission(int var1, int var2, int var3, String var4, String var5, String var6, float var7, float var8, float var9, int var10, boolean var11) {
      GameMode var12 = this.getGameMode();
      if (var12.equals(GameMode.Server)) {
         this.SendTransmission(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
      } else {
         this.DistributeTransmission(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
      }

   }

   public void SendTransmission(int var1, int var2, ChatMessage var3, int var4) {
      Color var5 = var3.getTextColor();
      int var6 = var3.getRadioChannel();
      this.SendTransmission(var1, var2, var6, var3.getText(), (String)null, (String)null, var5.r, var5.g, var5.b, var4, false);
   }

   public void SendTransmission(int var1, int var2, int var3, String var4, String var5, String var6, float var7, float var8, float var9, int var10, boolean var11) {
      this.SendTransmission(-1L, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
   }

   public void SendTransmission(long var1, int var3, int var4, int var5, String var6, String var7, String var8, float var9, float var10, float var11, int var12, boolean var13) {
      GameMode var14 = this.getGameMode();
      if (!var13 && (var14 == GameMode.Server || var14 == GameMode.SinglePlayer)) {
         this.hasAppliedInterference = false;
         var6 = this.applyWeatherInterference(var6, var12);
         if (this.hasAppliedInterference) {
            var9 = 0.5F;
            var10 = 0.5F;
            var11 = 0.5F;
            var8 = "";
         }
      }

      if (var14.equals(GameMode.SinglePlayer)) {
         this.ReceiveTransmission(var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13);
      } else if (var14.equals(GameMode.Server)) {
         GameServer.sendIsoWaveSignal(var1, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13);
      } else if (var14.equals(GameMode.Client)) {
         GameClient.sendIsoWaveSignal(var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13);
      }

   }

   public void PlayerListensChannel(int var1, boolean var2, boolean var3) {
      GameMode var4 = this.getGameMode();
      if (!var4.equals(GameMode.SinglePlayer) && !var4.equals(GameMode.Server)) {
         if (var4.equals(GameMode.Client)) {
            GameClient.sendPlayerListensChannel(var1, var2, var3);
         }
      } else if (this.scriptManager != null) {
         this.scriptManager.PlayerListensChannel(var1, var2, var3);
      }

   }

   public void RegisterDevice(WaveSignalDevice var1) {
      if (var1 != null) {
         if (!GameServer.bServer && !this.devices.contains(var1)) {
            this.devices.add(var1);
         }

         if (!GameServer.bServer && var1.getDeviceData().getIsTwoWay() && !this.broadcastDevices.contains(var1)) {
            this.broadcastDevices.add(var1);
         }

      }
   }

   public void UnRegisterDevice(WaveSignalDevice var1) {
      if (var1 != null) {
         if (!GameServer.bServer && this.devices.contains(var1)) {
            this.devices.remove(var1);
         }

         if (!GameServer.bServer && var1.getDeviceData().getIsTwoWay() && this.broadcastDevices.contains(var1)) {
            this.broadcastDevices.remove(var1);
         }

      }
   }

   public Object clone() {
      return null;
   }

   public String computerize(String var1) {
      StringBuilder var2 = this.stringBuilder;
      var2.setLength(0);
      char[] var3 = var1.toCharArray();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         char var6 = var3[var5];
         if (Character.isLetter(var6)) {
            var2.append(Rand.NextBool(2) ? Character.toLowerCase(var6) : Character.toUpperCase(var6));
         } else {
            var2.append(var6);
         }
      }

      return var2.toString();
   }

   public RecordedMedia getRecordedMedia() {
      return recordedMedia;
   }

   public void setDisableBroadcasting(boolean var1) {
      DISABLE_BROADCASTING = var1;
   }

   public boolean getDisableBroadcasting() {
      return DISABLE_BROADCASTING;
   }

   public void setDisableMediaLineLearning(boolean var1) {
      RecordedMedia.DISABLE_LINE_LEARNING = var1;
   }

   public boolean getDisableMediaLineLearning() {
      return RecordedMedia.DISABLE_LINE_LEARNING;
   }

   private void LouisvilleObfuscationCheck() {
      if (!GameClient.bClient && !GameServer.bServer) {
         IsoPlayer var1 = IsoPlayer.getInstance();
         if (var1 != null && var1.getY() < 3550.0F) {
            LOUISVILLE_OBFUSCATION = true;
         }

      }
   }

   public static void ObfuscateChannelCheck(RadioChannel var0) {
      if (var0.isVanilla()) {
         int var1 = var0.GetFrequency();

         for(int var2 = 0; var2 < obfuscateChannels.length; ++var2) {
            if (var1 == obfuscateChannels[var2]) {
               var0.setLouisvilleObfuscate(true);
            }
         }

      }
   }

   private static final class FreqListEntry {
      public boolean isInvItem = false;
      public DeviceData deviceData;
      public int sourceX = 0;
      public int sourceY = 0;

      public FreqListEntry(boolean var1, DeviceData var2, int var3, int var4) {
         this.isInvItem = var1;
         this.deviceData = var2;
         this.sourceX = var3;
         this.sourceY = var4;
      }
   }
}
