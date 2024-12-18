package zombie.core.raknet;

import fmod.FMODRecordPosition;
import fmod.FMODSoundData;
import fmod.FMOD_DriverInfo;
import fmod.FMOD_RESULT;
import fmod.SoundBuffer;
import fmod.javafmod;
import fmod.javafmodJNI;
import fmod.fmod.FMODManager;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.Platform;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Radio;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.FakeClientManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.radio.devices.DeviceData;
import zombie.vehicles.VehiclePart;

public class VoiceManager {
   private static final int FMOD_SOUND_MODE;
   public static final int modePPT = 1;
   public static final int modeVAD = 2;
   public static final int modeMute = 3;
   public static final int VADModeQuality = 1;
   public static final int VADModeLowBitrate = 2;
   public static final int VADModeAggressive = 3;
   public static final int VADModeVeryAggressive = 4;
   public static final int AGCModeAdaptiveAnalog = 1;
   public static final int AGCModeAdaptiveDigital = 2;
   public static final int AGCModeFixedDigital = 3;
   private static final int bufferSize = 192;
   private static final int complexity = 1;
   private static boolean serverVOIPEnable;
   private static int sampleRate;
   private static int period;
   private static int buffering;
   private static float minDistance;
   private static float maxDistance;
   private static boolean is3D;
   private boolean isEnable = true;
   private boolean isModeVAD = false;
   private boolean isModePPT = false;
   private int vadMode = 3;
   private int agcMode = 2;
   private int volumeMic;
   private int volumePlayers;
   public static boolean VoipDisabled;
   private boolean isServer;
   private static byte[] FMODReceiveBuffer;
   private final FMODSoundData FMODSoundData = new FMODSoundData();
   private final FMODRecordPosition FMODRecordPosition = new FMODRecordPosition();
   private int FMODSoundDataError = 0;
   private int FMODVoiceRecordDriverId;
   private long FMODChannelGroup = 0L;
   private long FMODRecordSound = 0L;
   private Semaphore recDevSemaphore;
   private boolean initialiseRecDev = false;
   private boolean initialisedRecDev = false;
   private long indicatorIsVoice = 0L;
   private Thread thread;
   private boolean bQuit;
   private long timeLast;
   private boolean isDebug = false;
   private boolean isDebugLoopback = false;
   private boolean isDebugLoopbackLong = false;
   public static VoiceManager instance;
   byte[] buf = new byte[192];
   private final Object notifier = new Object();
   private boolean bIsClient = false;
   private boolean bTestingMicrophone = false;
   private long testingMicrophoneMS = 0L;
   private static long timestamp;

   public VoiceManager() {
   }

   public static VoiceManager getInstance() {
      return instance;
   }

   public void DeinitRecSound() {
      this.initialisedRecDev = false;
      if (this.FMODRecordSound != 0L) {
         javafmod.FMOD_RecordSound_Release(this.FMODRecordSound);
         this.FMODRecordSound = 0L;
      }

      FMODReceiveBuffer = null;
   }

   public void ResetRecSound() {
      int var1;
      if (this.initialisedRecDev && this.FMODRecordSound != 0L) {
         var1 = javafmod.FMOD_System_RecordStop(this.FMODVoiceRecordDriverId);
         if (var1 != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStop result=%d", var1);
         }
      }

      this.DeinitRecSound();
      this.FMODRecordSound = javafmod.FMOD_System_CreateRecordSound((long)this.FMODVoiceRecordDriverId, (long)(FMODManager.FMOD_2D | FMODManager.FMOD_OPENUSER | FMODManager.FMOD_SOFTWARE), (long)FMODManager.FMOD_SOUND_FORMAT_PCM16, (long)sampleRate, this.agcMode);
      if (this.FMODRecordSound == 0L) {
         DebugLog.Voice.warn("FMOD_System_CreateSound result=%d", this.FMODRecordSound);
      }

      javafmod.FMOD_System_SetRecordVolume(1L - Math.round(Math.pow(1.4, (double)(11 - this.volumeMic))));
      if (this.initialiseRecDev) {
         var1 = javafmod.FMOD_System_RecordStart(this.FMODVoiceRecordDriverId, this.FMODRecordSound, true);
         if (var1 != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStart result=%d", var1);
         }
      }

      javafmod.FMOD_System_SetVADMode(this.vadMode - 1);
      FMODReceiveBuffer = new byte[2048];
      this.initialisedRecDev = true;
   }

   public void VoiceRestartClient(boolean var1) {
      if (GameClient.connection != null) {
         if (var1) {
            this.loadConfig();
            this.VoiceConnectReq(GameClient.connection.getConnectedGUID());
         } else {
            this.threadSafeCode(this::DeinitRecSound);
            this.VoiceConnectClose(GameClient.connection.getConnectedGUID());
            this.loadConfig();
         }
      } else {
         this.loadConfig();
         if (var1) {
            this.InitRecDeviceForTest();
         } else {
            this.threadSafeCode(this::DeinitRecSound);
         }
      }

   }

   void VoiceInitClient() {
      this.isServer = false;
      this.recDevSemaphore = new Semaphore(1);
      FMODReceiveBuffer = null;
      RakVoice.RVInit(192);
      RakVoice.SetComplexity(1);
   }

   void VoiceInitServer(boolean var1, int var2, int var3, int var4, int var5, double var6, double var8, boolean var10) {
      this.isServer = true;
      if (!(var3 == 2 | var3 == 5 | var3 == 10 | var3 == 20 | var3 == 40 | var3 == 60)) {
         DebugLog.Voice.error("Invalid period=%d", var3);
      } else if (!(var2 == 8000 | var2 == 16000 | var2 == 24000)) {
         DebugLog.Voice.error("Invalid sample rate=%d", var2);
      } else if (var4 < 0 | var4 > 10) {
         DebugLog.Voice.error("Invalid quality=%d", var4);
      } else if (var5 < 0 | var5 > 32000) {
         DebugLog.Voice.error("Invalid buffering=%d", var5);
      } else {
         sampleRate = var2;
         RakVoice.RVInitServer(var1, var2, var3, var4, var5, (float)var6, (float)var8, var10);
      }
   }

   void VoiceConnectAccept(long var1) {
      if (this.isEnable) {
         DebugLog.Voice.debugln("uuid=%x", var1);
      }

   }

   void InitRecDeviceForTest() {
      this.threadSafeCode(this::ResetRecSound);
   }

   void VoiceOpenChannelReply(long var1, ByteBuffer var3) {
      if (this.isEnable) {
         DebugLog.Voice.debugln("uuid=%d", var1);
         if (this.isServer) {
            return;
         }

         try {
            if (GameClient.bClient) {
               serverVOIPEnable = var3.getInt() != 0;
               sampleRate = var3.getInt();
               period = var3.getInt();
               var3.getInt();
               buffering = var3.getInt();
               minDistance = var3.getFloat();
               maxDistance = var3.getFloat();
               is3D = var3.getInt() != 0;
            } else {
               serverVOIPEnable = RakVoice.GetServerVOIPEnable();
               sampleRate = RakVoice.GetSampleRate();
               period = RakVoice.GetSendFramePeriod();
               buffering = RakVoice.GetBuffering();
               minDistance = RakVoice.GetMinDistance();
               maxDistance = RakVoice.GetMaxDistance();
               is3D = RakVoice.GetIs3D();
            }
         } catch (Exception var8) {
            DebugLog.Voice.printException(var8, "RakVoice params set failed", LogSeverity.Error);
            return;
         }

         DebugLog.Voice.debugln("enabled=%b, sample-rate=%d, period=%d, complexity=%d, buffering=%d, is3D=%b", serverVOIPEnable, sampleRate, period, 1, buffering, is3D);

         try {
            this.recDevSemaphore.acquire();
         } catch (InterruptedException var7) {
            var7.printStackTrace();
         }

         int var4 = is3D ? FMODManager.FMOD_3D | FMOD_SOUND_MODE : FMOD_SOUND_MODE;
         Iterator var5 = VoiceManagerData.data.iterator();

         while(var5.hasNext()) {
            VoiceManagerData var6 = (VoiceManagerData)var5.next();
            if (var6.userplaysound != 0L) {
               javafmod.FMOD_Sound_SetMode(var6.userplaysound, var4);
            }
         }

         long var9 = javafmod.FMOD_System_SetRawPlayBufferingPeriod((long)buffering);
         if (var9 != (long)FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_SetRawPlayBufferingPeriod result=%d", var9);
         }

         this.ResetRecSound();
         this.recDevSemaphore.release();
         if (this.isDebug) {
            VoiceDebug.createAndShowGui();
         }
      }

   }

   public void VoiceConnectReq(long var1) {
      if (this.isEnable) {
         DebugLog.Voice.debugln("uuid=%x", var1);
         VoiceManagerData.data.clear();
         RakVoice.RequestVoiceChannel(var1);
      }

   }

   public void VoiceConnectClose(long var1) {
      if (this.isEnable) {
         DebugLog.Voice.debugln("uuid=%x", var1);
         RakVoice.CloseVoiceChannel(var1);
      }

   }

   public void setMode(int var1) {
      if (var1 == 3) {
         this.isModeVAD = false;
         this.isModePPT = false;
      } else if (var1 == 1) {
         this.isModeVAD = false;
         this.isModePPT = true;
      } else if (var1 == 2) {
         this.isModeVAD = true;
         this.isModePPT = false;
      }

   }

   public void setVADMode(int var1) {
      if (!(var1 < 1 | var1 > 4)) {
         this.vadMode = var1;
         if (this.initialisedRecDev) {
            this.threadSafeCode(() -> {
               javafmod.FMOD_System_SetVADMode(this.vadMode - 1);
            });
         }
      }
   }

   public void setAGCMode(int var1) {
      if (!(var1 < 1 | var1 > 3)) {
         this.agcMode = var1;
         if (this.initialisedRecDev) {
            this.threadSafeCode(this::ResetRecSound);
         }
      }
   }

   public void setVolumePlayers(int var1) {
      if (!(var1 < 0 | var1 > 11)) {
         if (var1 <= 10) {
            this.volumePlayers = var1;
         } else {
            this.volumePlayers = 12;
         }

         if (this.initialisedRecDev) {
            ArrayList var2 = VoiceManagerData.data;

            for(int var3 = 0; var3 < var2.size(); ++var3) {
               VoiceManagerData var4 = (VoiceManagerData)var2.get(var3);
               if (var4 != null && var4.userplaychannel != 0L) {
                  javafmod.FMOD_Channel_SetVolume(var4.userplaychannel, (float)((double)this.volumePlayers * 0.2));
               }
            }

         }
      }
   }

   public void setVolumeMic(int var1) {
      if (!(var1 < 0 | var1 > 11)) {
         if (var1 <= 10) {
            this.volumeMic = var1;
         } else {
            this.volumeMic = 12;
         }

         if (this.initialisedRecDev) {
            this.threadSafeCode(() -> {
               javafmod.FMOD_System_SetRecordVolume(1L - Math.round(Math.pow(1.4, (double)(11 - this.volumeMic))));
            });
         }
      }
   }

   public static void playerSetMute(String var0) {
      ArrayList var1 = GameClient.instance.getPlayers();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoPlayer var3 = (IsoPlayer)var1.get(var2);
         if (var0.equals(var3.username)) {
            VoiceManagerData var4 = VoiceManagerData.get(var3.OnlineID);
            var4.userplaymute = !var4.userplaymute;
            var3.isVoiceMute = var4.userplaymute;
            break;
         }
      }

   }

   public static boolean playerGetMute(String var0) {
      ArrayList var2 = GameClient.instance.getPlayers();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoPlayer var4 = (IsoPlayer)var2.get(var3);
         if (var0.equals(var4.username)) {
            boolean var1 = VoiceManagerData.get(var4.OnlineID).userplaymute;
            return var1;
         }
      }

      return true;
   }

   public void LuaRegister(Platform var1, KahluaTable var2) {
      KahluaTable var3 = var1.newTable();
      var3.rawset("playerSetMute", new JavaFunction() {
         public int call(LuaCallFrame var1, int var2) {
            Object var3 = var1.get(1);
            VoiceManager.playerSetMute((String)var3);
            return 1;
         }
      });
      var3.rawset("playerGetMute", new JavaFunction() {
         public int call(LuaCallFrame var1, int var2) {
            Object var3 = var1.get(1);
            var1.push(VoiceManager.playerGetMute((String)var3));
            return 1;
         }
      });
      var3.rawset("RecordDevices", new JavaFunction() {
         public int call(LuaCallFrame var1, int var2) {
            if (!Core.SoundDisabled && !VoiceManager.VoipDisabled) {
               int var7 = javafmod.FMOD_System_GetRecordNumDrivers();
               KahluaTable var4 = var1.getPlatform().newTable();

               for(int var5 = 0; var5 < var7; ++var5) {
                  FMOD_DriverInfo var6 = new FMOD_DriverInfo();
                  javafmod.FMOD_System_GetRecordDriverInfo(var5, var6);
                  var4.rawset(var5 + 1, var6.name);
               }

               var1.push(var4);
               return 1;
            } else {
               KahluaTable var3 = var1.getPlatform().newTable();
               var1.push(var3);
               return 1;
            }
         }
      });
      var2.rawset("VoiceManager", var3);
   }

   private void setUserPlaySound(long var1, float var3) {
      var3 = IsoUtils.clamp(var3 * IsoUtils.lerp((float)this.volumePlayers, 0.0F, 12.0F), 0.0F, 1.0F);
      javafmod.FMOD_Channel_SetVolume(var1, var3);
   }

   private long getUserPlaySound(short var1) {
      VoiceManagerData var2 = VoiceManagerData.get(var1);
      if (var2.userplaychannel == 0L) {
         var2.userplaysound = 0L;
         int var3 = is3D ? FMODManager.FMOD_3D | FMOD_SOUND_MODE : FMOD_SOUND_MODE;
         var2.userplaysound = javafmod.FMOD_System_CreateRAWPlaySound((long)var3, (long)FMODManager.FMOD_SOUND_FORMAT_PCM16, (long)sampleRate);
         if (var2.userplaysound == 0L) {
            DebugLog.Voice.warn("FMOD_System_CreateSound result=%d", var2.userplaysound);
         }

         var2.userplaychannel = javafmod.FMOD_System_PlaySound(var2.userplaysound, false);
         if (var2.userplaychannel == 0L) {
            DebugLog.Voice.warn("FMOD_System_PlaySound result=%d", var2.userplaychannel);
         }

         javafmod.FMOD_Channel_SetVolume(var2.userplaychannel, (float)((double)this.volumePlayers * 0.2));
         if (is3D) {
            javafmod.FMOD_Channel_Set3DMinMaxDistance(var2.userplaychannel, minDistance / 2.0F, maxDistance);
         }

         javafmod.FMOD_Channel_SetChannelGroup(var2.userplaychannel, this.FMODChannelGroup);
      }

      return var2.userplaysound;
   }

   public void InitVMClient() {
      if (!Core.SoundDisabled && !VoipDisabled) {
         int var1 = javafmod.FMOD_System_GetRecordNumDrivers();
         this.FMODVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
         if (this.FMODVoiceRecordDriverId < 0 && var1 > 0) {
            Core.getInstance().setOptionVoiceRecordDevice(1);
            this.FMODVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
         }

         if (var1 < 1) {
            DebugLog.Voice.debugln("Microphone not found");
            this.initialiseRecDev = false;
         } else if (this.FMODVoiceRecordDriverId < 0 | this.FMODVoiceRecordDriverId >= var1) {
            DebugLog.Voice.warn("Invalid record device");
            this.initialiseRecDev = false;
         } else {
            this.initialiseRecDev = true;
         }

         this.isEnable = Core.getInstance().getOptionVoiceEnable();
         this.setMode(Core.getInstance().getOptionVoiceMode());
         this.vadMode = Core.getInstance().getOptionVoiceVADMode();
         this.volumeMic = Core.getInstance().getOptionVoiceVolumeMic();
         this.volumePlayers = Core.getInstance().getOptionVoiceVolumePlayers();
         this.FMODChannelGroup = javafmod.FMOD_System_CreateChannelGroup("VOIP");
         this.VoiceInitClient();
         this.FMODRecordSound = 0L;
         if (this.isEnable) {
            this.InitRecDeviceForTest();
         }

         if (this.isDebug) {
            VoiceDebug.createAndShowGui();
         }

         this.timeLast = System.currentTimeMillis();
         this.bQuit = false;
         this.thread = new Thread() {
            public void run() {
               while(!VoiceManager.this.bQuit) {
                  try {
                     VoiceManager.this.UpdateVMClient();
                     sleep((long)(VoiceManager.period / 2));
                  } catch (Exception var2) {
                     var2.printStackTrace();
                  }
               }

            }
         };
         this.thread.setName("VoiceManagerClient");
         this.thread.start();
      } else {
         this.isEnable = false;
         this.initialiseRecDev = false;
         this.initialisedRecDev = false;
         DebugLog.Voice.debugln("Disabled");
      }
   }

   public void loadConfig() {
      this.isEnable = Core.getInstance().getOptionVoiceEnable();
      this.setMode(Core.getInstance().getOptionVoiceMode());
      this.vadMode = Core.getInstance().getOptionVoiceVADMode();
      this.volumeMic = Core.getInstance().getOptionVoiceVolumeMic();
      this.volumePlayers = Core.getInstance().getOptionVoiceVolumePlayers();
   }

   public void UpdateRecordDevice() {
      if (this.initialisedRecDev) {
         this.threadSafeCode(this::UpdateRecordDeviceInternal);
      }
   }

   private void UpdateRecordDeviceInternal() {
      int var1 = javafmod.FMOD_System_RecordStop(this.FMODVoiceRecordDriverId);
      if (var1 != FMOD_RESULT.FMOD_OK.ordinal()) {
         DebugLog.Voice.warn("FMOD_System_RecordStop result=%d", var1);
      }

      this.FMODVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
      if (this.FMODVoiceRecordDriverId < 0) {
         DebugLog.Voice.error("No record device found");
      } else {
         var1 = javafmod.FMOD_System_RecordStart(this.FMODVoiceRecordDriverId, this.FMODRecordSound, true);
         if (var1 != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStart result=%d", var1);
         }

      }
   }

   public void DeinitVMClient() {
      if (this.thread != null) {
         this.bQuit = true;
         synchronized(this.notifier) {
            this.notifier.notify();
         }

         while(this.thread.isAlive()) {
            try {
               Thread.sleep(10L);
            } catch (InterruptedException var4) {
            }
         }

         this.thread = null;
      }

      this.DeinitRecSound();
      ArrayList var1 = VoiceManagerData.data;

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         VoiceManagerData var3 = (VoiceManagerData)var1.get(var2);
         if (var3.userplaychannel != 0L) {
            javafmod.FMOD_Channel_Stop(var3.userplaychannel);
         }

         if (var3.userplaysound != 0L) {
            javafmod.FMOD_RAWPlaySound_Release(var3.userplaysound);
            var3.userplaysound = 0L;
         }
      }

      VoiceManagerData.data.clear();
   }

   public void setTestingMicrophone(boolean var1) {
      if (var1) {
         this.testingMicrophoneMS = System.currentTimeMillis();
      }

      if (var1 != this.bTestingMicrophone) {
         this.bTestingMicrophone = var1;
         this.notifyThread();
      }

   }

   public void notifyThread() {
      synchronized(this.notifier) {
         this.notifier.notify();
      }
   }

   public void update() {
      if (!GameServer.bServer) {
         if (this.bTestingMicrophone) {
            long var1 = System.currentTimeMillis();
            if (var1 - this.testingMicrophoneMS > 1000L) {
               this.setTestingMicrophone(false);
            }
         }

         if ((!GameClient.bClient || GameClient.connection == null) && !FakeClientManager.isVOIPEnabled()) {
            if (this.bIsClient) {
               this.bIsClient = false;
               this.notifyThread();
            }
         } else if (!this.bIsClient) {
            this.bIsClient = true;
            this.notifyThread();
         }

      }
   }

   private float getCanHearAllVolume(float var1) {
      return var1 > minDistance ? IsoUtils.clamp(1.0F - IsoUtils.lerp(var1, minDistance, maxDistance), 0.2F, 1.0F) : 1.0F;
   }

   private void threadSafeCode(Runnable var1) {
      while(true) {
         try {
            this.recDevSemaphore.acquire();
         } catch (InterruptedException var7) {
            continue;
         }

         try {
            var1.run();
         } finally {
            this.recDevSemaphore.release();
         }

         return;
      }
   }

   synchronized void UpdateVMClient() throws InterruptedException {
      while(!this.bQuit && !this.bIsClient && !this.bTestingMicrophone) {
         synchronized(this.notifier) {
            try {
               this.notifier.wait();
            } catch (InterruptedException var13) {
            }
         }
      }

      if (serverVOIPEnable) {
         if (IsoPlayer.getInstance() != null) {
            IsoPlayer.getInstance().isSpeek = System.currentTimeMillis() - this.indicatorIsVoice <= 300L;
         }

         if (this.initialiseRecDev) {
            this.recDevSemaphore.acquire();
            javafmod.FMOD_System_GetRecordPosition(this.FMODVoiceRecordDriverId, this.FMODRecordPosition);
            if (FMODReceiveBuffer != null) {
               label210:
               while(true) {
                  while(true) {
                     if ((this.FMODSoundDataError = javafmod.FMOD_Sound_GetData(this.FMODRecordSound, FMODReceiveBuffer, this.FMODSoundData)) != 0) {
                        break label210;
                     }

                     if ((IsoPlayer.getInstance() == null || GameClient.connection == null) && !FakeClientManager.isVOIPEnabled()) {
                        break;
                     }

                     if (!is3D || !IsoPlayer.getInstance().isDead()) {
                        if (this.isModePPT) {
                           if (GameKeyboard.isKeyDown("Enable voice transmit")) {
                              RakVoice.SendFrame(GameClient.connection.connectedGUID, (long)IsoPlayer.getInstance().getOnlineID(), FMODReceiveBuffer, this.FMODSoundData.size);
                              this.indicatorIsVoice = System.currentTimeMillis();
                           } else if (FakeClientManager.isVOIPEnabled()) {
                              RakVoice.SendFrame(FakeClientManager.getConnectedGUID(), FakeClientManager.getOnlineID(), FMODReceiveBuffer, this.FMODSoundData.size);
                              this.indicatorIsVoice = System.currentTimeMillis();
                           }
                        }

                        if (this.isModeVAD && this.FMODSoundData.vad != 0L) {
                           RakVoice.SendFrame(GameClient.connection.connectedGUID, (long)IsoPlayer.getInstance().getOnlineID(), FMODReceiveBuffer, this.FMODSoundData.size);
                           this.indicatorIsVoice = System.currentTimeMillis();
                        }
                        break;
                     }
                  }

                  if (this.isDebug) {
                     if (GameClient.IDToPlayerMap.values().size() > 0) {
                        VoiceDebug.updateGui((SoundBuffer)null, FMODReceiveBuffer);
                     } else if (this.isDebugLoopback) {
                        VoiceDebug.updateGui((SoundBuffer)null, FMODReceiveBuffer);
                     } else {
                        VoiceDebug.updateGui((SoundBuffer)null, FMODReceiveBuffer);
                     }
                  }

                  if (this.isDebugLoopback) {
                     javafmod.FMOD_System_RAWPlayData(this.getUserPlaySound((short)0), FMODReceiveBuffer, this.FMODSoundData.size);
                  }
               }
            }

            this.recDevSemaphore.release();
         }

         ArrayList var1 = GameClient.instance.getPlayers();
         ArrayList var2 = VoiceManagerData.data;

         int var6;
         for(int var3 = 0; var3 < var2.size(); ++var3) {
            VoiceManagerData var4 = (VoiceManagerData)var2.get(var3);
            boolean var5 = false;

            for(var6 = 0; var6 < var1.size(); ++var6) {
               IsoPlayer var7 = (IsoPlayer)var1.get(var6);
               if (var7.OnlineID == var4.index) {
                  var5 = true;
                  break;
               }
            }

            if (this.isDebugLoopback & var4.index == 0) {
               break;
            }

            if (var4.userplaychannel != 0L & !var5) {
               javafmod.FMOD_Channel_Stop(var4.userplaychannel);
               var4.userplaychannel = 0L;
            }
         }

         long var15 = System.currentTimeMillis() - this.timeLast;
         if (var15 >= (long)period) {
            this.timeLast += var15;
            if (IsoPlayer.getInstance() != null) {
               VoiceManagerData.VoiceDataSource var16 = VoiceManagerData.VoiceDataSource.Unknown;
               var6 = 0;
               Iterator var17 = var1.iterator();

               label172:
               while(true) {
                  IsoPlayer var8;
                  IsoPlayer var9;
                  do {
                     do {
                        if (!var17.hasNext()) {
                           MPStatistics.setVOIPSource(var16, var6);
                           return;
                        }

                        var8 = (IsoPlayer)var17.next();
                        var9 = IsoPlayer.getInstance();
                     } while(var8 == var9);
                  } while(var8.getOnlineID() == -1);

                  VoiceManagerData var10 = VoiceManagerData.get(var8.getOnlineID());

                  while(true) {
                     do {
                        if (!RakVoice.ReceiveFrame((long)var8.getOnlineID(), this.buf)) {
                           if (var10.voicetimeout == 0L) {
                              var8.isSpeek = false;
                           } else {
                              --var10.voicetimeout;
                              var8.isSpeek = true;
                           }
                           continue label172;
                        }

                        var10.voicetimeout = 10L;
                     } while(var10.userplaymute);

                     float var11 = IsoUtils.DistanceTo(var9.getX(), var9.getY(), var8.getX(), var8.getY());
                     if (var9.isCanHearAll()) {
                        javafmodJNI.FMOD_Channel_Set3DLevel(var10.userplaychannel, 0.0F);
                        javafmod.FMOD_Channel_Set3DAttributes(var10.userplaychannel, var9.getX(), var9.getY(), var9.getZ(), 0.0F, 0.0F, 0.0F);
                        this.setUserPlaySound(var10.userplaychannel, this.getCanHearAllVolume(var11));
                        var16 = VoiceManagerData.VoiceDataSource.Cheat;
                        var6 = 0;
                     } else {
                        VoiceManagerData.RadioData var12 = this.checkForNearbyRadios(var10);
                        if (var12 != null && var12.deviceData != null) {
                           javafmodJNI.FMOD_Channel_Set3DLevel(var10.userplaychannel, 0.0F);
                           javafmod.FMOD_Channel_Set3DAttributes(var10.userplaychannel, var9.getX(), var9.getY(), var9.getZ(), 0.0F, 0.0F, 0.0F);
                           this.setUserPlaySound(var10.userplaychannel, var12.deviceData.getDeviceVolume());
                           var12.deviceData.doReceiveMPSignal(var12.lastReceiveDistance);
                           var16 = VoiceManagerData.VoiceDataSource.Radio;
                           var6 = var12.freq;
                        } else {
                           if (var12 == null) {
                              javafmodJNI.FMOD_Channel_Set3DLevel(var10.userplaychannel, 0.0F);
                              javafmod.FMOD_Channel_Set3DAttributes(var10.userplaychannel, var9.getX(), var9.getY(), var9.getZ(), 0.0F, 0.0F, 0.0F);
                              javafmod.FMOD_Channel_SetVolume(var10.userplaychannel, 0.0F);
                              var16 = VoiceManagerData.VoiceDataSource.Unknown;
                           } else {
                              if (is3D) {
                                 javafmodJNI.FMOD_Channel_Set3DLevel(var10.userplaychannel, IsoUtils.lerp(var11, 0.0F, minDistance));
                                 javafmod.FMOD_Channel_Set3DAttributes(var10.userplaychannel, var8.getX(), var8.getY(), var8.getZ(), 0.0F, 0.0F, 0.0F);
                              } else {
                                 javafmodJNI.FMOD_Channel_Set3DLevel(var10.userplaychannel, 0.0F);
                                 javafmod.FMOD_Channel_Set3DAttributes(var10.userplaychannel, var9.getX(), var9.getY(), var9.getZ(), 0.0F, 0.0F, 0.0F);
                              }

                              this.setUserPlaySound(var10.userplaychannel, IsoUtils.smoothstep(maxDistance, minDistance, var12.lastReceiveDistance));
                              var16 = VoiceManagerData.VoiceDataSource.Voice;
                           }

                           var6 = 0;
                           if (var11 > maxDistance) {
                              logFrame(var9, var8, var11);
                           }
                        }
                     }

                     javafmod.FMOD_System_RAWPlayData(this.getUserPlaySound(var8.getOnlineID()), this.buf, (long)this.buf.length);
                     if (this.isDebugLoopbackLong) {
                        RakVoice.SendFrame(GameClient.connection.connectedGUID, (long)var9.getOnlineID(), this.buf, (long)this.buf.length);
                     }
                  }
               }
            }
         }
      }
   }

   private static void logFrame(IsoPlayer var0, IsoPlayer var1, float var2) {
      long var3 = System.currentTimeMillis();
      if (var3 > timestamp) {
         timestamp = var3 + 5000L;
         DebugLog.Multiplayer.warn(String.format("\"%s\" (%b) received VOIP frame from \"%s\" (%b) at distance=%f", var0.getUsername(), var0.isCanHearAll(), var1.getUsername(), var1.isCanHearAll(), var2));
      }

   }

   private VoiceManagerData.RadioData checkForNearbyRadios(VoiceManagerData var1) {
      IsoPlayer var2 = IsoPlayer.getInstance();
      VoiceManagerData var3 = VoiceManagerData.get(var2.OnlineID);
      if (var3.isCanHearAll) {
         ((VoiceManagerData.RadioData)var3.radioData.get(0)).lastReceiveDistance = 0.0F;
         return (VoiceManagerData.RadioData)var3.radioData.get(0);
      } else {
         VoiceManagerData.RadioData var10000;
         synchronized(var3.radioData) {
            int var5 = 1;

            while(true) {
               if (var5 >= var3.radioData.size()) {
                  break;
               }

               synchronized(var1.radioData) {
                  int var7 = 1;

                  while(true) {
                     if (var7 >= var1.radioData.size()) {
                        break;
                     }

                     if (((VoiceManagerData.RadioData)var3.radioData.get(var5)).freq == ((VoiceManagerData.RadioData)var1.radioData.get(var7)).freq) {
                        float var8 = (float)(((VoiceManagerData.RadioData)var3.radioData.get(var5)).x - ((VoiceManagerData.RadioData)var1.radioData.get(var7)).x);
                        float var9 = (float)(((VoiceManagerData.RadioData)var3.radioData.get(var5)).y - ((VoiceManagerData.RadioData)var1.radioData.get(var7)).y);
                        ((VoiceManagerData.RadioData)var3.radioData.get(var5)).lastReceiveDistance = (float)Math.sqrt((double)(var8 * var8 + var9 * var9));
                        if (((VoiceManagerData.RadioData)var3.radioData.get(var5)).lastReceiveDistance < ((VoiceManagerData.RadioData)var1.radioData.get(var7)).distance) {
                           var10000 = (VoiceManagerData.RadioData)var3.radioData.get(var5);
                           return var10000;
                        }
                     }

                     ++var7;
                  }
               }

               ++var5;
            }
         }

         synchronized(var3.radioData) {
            synchronized(var1.radioData) {
               if (!var1.radioData.isEmpty() && !var3.radioData.isEmpty()) {
                  float var6 = (float)(((VoiceManagerData.RadioData)var3.radioData.get(0)).x - ((VoiceManagerData.RadioData)var1.radioData.get(0)).x);
                  float var18 = (float)(((VoiceManagerData.RadioData)var3.radioData.get(0)).y - ((VoiceManagerData.RadioData)var1.radioData.get(0)).y);
                  ((VoiceManagerData.RadioData)var3.radioData.get(0)).lastReceiveDistance = (float)Math.sqrt((double)(var6 * var6 + var18 * var18));
                  if (((VoiceManagerData.RadioData)var3.radioData.get(0)).lastReceiveDistance < ((VoiceManagerData.RadioData)var1.radioData.get(0)).distance) {
                     var10000 = (VoiceManagerData.RadioData)var3.radioData.get(0);
                     return var10000;
                  }
               }

               return null;
            }
         }
      }
   }

   public void UpdateChannelsRoaming(UdpConnection var1) {
      IsoPlayer var2 = IsoPlayer.getInstance();
      if (var2.OnlineID != -1) {
         VoiceManagerData var3 = VoiceManagerData.get(var2.OnlineID);
         boolean var4 = false;
         synchronized(var3.radioData) {
            var3.radioData.clear();
            HashSet var6 = new HashSet();

            for(int var7 = 0; var7 < IsoPlayer.numPlayers; ++var7) {
               IsoPlayer var8 = IsoPlayer.players[var7];
               if (var8 != null) {
                  var4 |= var8.isCanHearAll();
                  var3.radioData.add(new VoiceManagerData.RadioData(RakVoice.GetMaxDistance(), var8.getX(), var8.getY()));

                  int var9;
                  for(var9 = 0; var9 < var8.getInventory().getItems().size(); ++var9) {
                     InventoryItem var10 = (InventoryItem)var8.getInventory().getItems().get(var9);
                     if (var10 instanceof Radio) {
                        DeviceData var11 = ((Radio)var10).getDeviceData();
                        if (var11 != null && var11.getIsTurnedOn()) {
                           var3.radioData.add(new VoiceManagerData.RadioData(var11, var8.getX(), var8.getY()));
                        }
                     }
                  }

                  for(var9 = (int)var8.getX() - 4; (float)var9 < var8.getX() + 5.0F; ++var9) {
                     for(int var21 = (int)var8.getY() - 4; (float)var21 < var8.getY() + 5.0F; ++var21) {
                        for(int var22 = (int)var8.getZ() - 1; (float)var22 < var8.getZ() + 1.0F; ++var22) {
                           IsoGridSquare var12 = IsoCell.getInstance().getGridSquare(var9, var21, var22);
                           if (var12 != null) {
                              int var13;
                              DeviceData var15;
                              if (var12.getObjects() != null) {
                                 for(var13 = 0; var13 < var12.getObjects().size(); ++var13) {
                                    IsoObject var14 = (IsoObject)var12.getObjects().get(var13);
                                    if (var14 instanceof IsoRadio) {
                                       var15 = ((IsoRadio)var14).getDeviceData();
                                       if (var15 != null && var15.getIsTurnedOn()) {
                                          var3.radioData.add(new VoiceManagerData.RadioData(var15, (float)var12.x, (float)var12.y));
                                          if (!var14.getModData().isEmpty()) {
                                             Object var16 = var14.getModData().rawget("RadioItemID");
                                             if (var16 != null && var16 instanceof Double) {
                                                var6.add(((Double)var16).intValue());
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              if (var12.getWorldObjects() != null) {
                                 for(var13 = 0; var13 < var12.getWorldObjects().size(); ++var13) {
                                    IsoWorldInventoryObject var24 = (IsoWorldInventoryObject)var12.getWorldObjects().get(var13);
                                    if (var24.getItem() != null && var24.getItem() instanceof Radio && !var6.contains(var24.getItem().getID())) {
                                       var15 = ((Radio)var24.getItem()).getDeviceData();
                                       if (var15 != null && var15.getIsTurnedOn()) {
                                          var3.radioData.add(new VoiceManagerData.RadioData(var15, (float)var12.x, (float)var12.y));
                                       }
                                    }
                                 }
                              }

                              if (var12.getVehicleContainer() != null && var12 == var12.getVehicleContainer().getSquare()) {
                                 VehiclePart var23 = var12.getVehicleContainer().getPartById("Radio");
                                 if (var23 != null) {
                                    DeviceData var25 = var23.getDeviceData();
                                    if (var25 != null && var25.getIsTurnedOn()) {
                                       var3.radioData.add(new VoiceManagerData.RadioData(var25, (float)var12.x, (float)var12.y));
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         ByteBufferWriter var5 = var1.startPacket();
         PacketTypes.PacketType.SyncRadioData.doPacket(var5);
         var5.putByte((byte)(var4 ? 1 : 0));
         var5.putInt(var3.radioData.size() * 4);
         Iterator var19 = var3.radioData.iterator();

         while(var19.hasNext()) {
            VoiceManagerData.RadioData var20 = (VoiceManagerData.RadioData)var19.next();
            var5.putInt(var20.freq);
            var5.putInt((int)var20.distance);
            var5.putInt(var20.x);
            var5.putInt(var20.y);
         }

         PacketTypes.PacketType.SyncRadioData.send(var1);
      }
   }

   void InitVMServer() {
      this.VoiceInitServer(ServerOptions.instance.VoiceEnable.getValue(), 24000, 20, 5, 8000, ServerOptions.instance.VoiceMinDistance.getValue(), ServerOptions.instance.VoiceMaxDistance.getValue(), ServerOptions.instance.Voice3D.getValue());
   }

   public int getMicVolumeIndicator() {
      return FMODReceiveBuffer == null ? 0 : (int)this.FMODSoundData.loudness;
   }

   public boolean getMicVolumeError() {
      if (FMODReceiveBuffer == null) {
         return true;
      } else {
         return this.FMODSoundDataError == -1;
      }
   }

   public boolean getServerVOIPEnable() {
      return serverVOIPEnable;
   }

   public void VMServerBan(short var1, boolean var2) {
      RakVoice.SetVoiceBan((long)var1, var2);
   }

   static {
      FMOD_SOUND_MODE = FMODManager.FMOD_OPENUSER | FMODManager.FMOD_LOOP_NORMAL | FMODManager.FMOD_CREATESTREAM;
      serverVOIPEnable = true;
      sampleRate = 16000;
      period = 300;
      buffering = 8000;
      is3D = false;
      VoipDisabled = false;
      instance = new VoiceManager();
      timestamp = 0L;
   }
}
