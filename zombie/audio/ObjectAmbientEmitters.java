package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import zombie.AmbientStreamManager;
import zombie.GameSounds;
import zombie.audio.parameters.ParameterCurrentZone;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class ObjectAmbientEmitters {
   private final HashMap<String, PowerPolicy> powerPolicyMap = new HashMap();
   private static ObjectAmbientEmitters instance = null;
   static final Vector2 tempVector2 = new Vector2();
   private final HashMap<IsoObject, ObjectWithDistance> m_added = new HashMap();
   private final ObjectPool<ObjectWithDistance> m_objectPool = new ObjectPool(ObjectWithDistance::new);
   private final ArrayList<ObjectWithDistance> m_objects = new ArrayList();
   private final Slot[] m_slots;
   private final Comparator<ObjectWithDistance> comp = new Comparator<ObjectWithDistance>() {
      public int compare(ObjectWithDistance var1, ObjectWithDistance var2) {
         return Float.compare(var1.distSq, var2.distSq);
      }
   };

   public static ObjectAmbientEmitters getInstance() {
      if (instance == null) {
         instance = new ObjectAmbientEmitters();
      }

      return instance;
   }

   private ObjectAmbientEmitters() {
      byte var1 = 16;
      this.m_slots = (Slot[])PZArrayUtil.newInstance(Slot.class, var1, Slot::new);
      this.powerPolicyMap.put("FactoryMachineAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
      this.powerPolicyMap.put("HotdogMachineAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
      this.powerPolicyMap.put("PayPhoneAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
      this.powerPolicyMap.put("StreetLightAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
      this.powerPolicyMap.put("NeonLightAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
      this.powerPolicyMap.put("NeonSignAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
      this.powerPolicyMap.put("JukeboxAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
      this.powerPolicyMap.put("ControlStationAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
      this.powerPolicyMap.put("ClockAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
      this.powerPolicyMap.put("GasPumpAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
      this.powerPolicyMap.put("LightBulbAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
      this.powerPolicyMap.put("ArcadeMachineAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
   }

   private void addObject(IsoObject var1, PerObjectLogic var2) {
      if (!GameServer.bServer) {
         if (!this.m_added.containsKey(var1)) {
            boolean var3 = false;

            for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
               IsoPlayer var5 = IsoPlayer.players[var4];
               if (var5 != null && var1.getObjectIndex() != -1) {
                  byte var6 = 15;
                  if (var2 instanceof DoorLogic || var2 instanceof WindowLogic) {
                     var6 = 10;
                  }

                  if ((var1.square.z == PZMath.fastfloor(var5.getZ()) || !(var2 instanceof DoorLogic) && !(var2 instanceof WindowLogic)) && !(var5.DistToSquared((float)var1.square.x + 0.5F, (float)var1.square.y + 0.5F) > (float)(var6 * var6))) {
                     var3 = true;
                     break;
                  }
               }
            }

            if (var3) {
               ObjectWithDistance var7 = (ObjectWithDistance)this.m_objectPool.alloc();
               var7.object = var1;
               var7.logic = var2;
               this.m_objects.add(var7);
               this.m_added.put(var1, var7);
            }
         }
      }
   }

   void removeObject(IsoObject var1) {
      if (!GameServer.bServer) {
         ObjectWithDistance var2 = (ObjectWithDistance)this.m_added.remove(var1);
         if (var2 != null) {
            this.m_objects.remove(var2);
            this.m_objectPool.release((Object)var2);
         }
      }
   }

   public void update() {
      if (!GameServer.bServer) {
         this.addObjectsFromChunks();

         int var1;
         for(var1 = 0; var1 < this.m_slots.length; ++var1) {
            this.m_slots[var1].playing = false;
         }

         if (this.m_objects.isEmpty()) {
            this.stopNotPlaying();
         } else {
            IsoObject var3;
            PerObjectLogic var4;
            for(var1 = 0; var1 < this.m_objects.size(); ++var1) {
               ObjectWithDistance var2 = (ObjectWithDistance)this.m_objects.get(var1);
               var3 = var2.object;
               var4 = ((ObjectWithDistance)this.m_objects.get(var1)).logic;
               if (!this.shouldPlay(var3, var4)) {
                  this.m_added.remove(var3);
                  this.m_objects.remove(var1--);
                  this.m_objectPool.release((Object)var2);
               } else {
                  var3.getFacingPosition(tempVector2);
                  var2.distSq = this.getClosestListener(tempVector2.x, tempVector2.y, (float)var3.square.z);
               }
            }

            this.m_objects.sort(this.comp);
            var1 = Math.min(this.m_objects.size(), this.m_slots.length);

            int var5;
            int var6;
            for(var6 = 0; var6 < var1; ++var6) {
               var3 = ((ObjectWithDistance)this.m_objects.get(var6)).object;
               var4 = ((ObjectWithDistance)this.m_objects.get(var6)).logic;
               if (this.shouldPlay(var3, var4)) {
                  var5 = this.getExistingSlot(var3);
                  if (var5 != -1) {
                     this.m_slots[var5].playSound(var3, var4);
                  }
               }
            }

            for(var6 = 0; var6 < var1; ++var6) {
               var3 = ((ObjectWithDistance)this.m_objects.get(var6)).object;
               var4 = ((ObjectWithDistance)this.m_objects.get(var6)).logic;
               if (this.shouldPlay(var3, var4)) {
                  var5 = this.getExistingSlot(var3);
                  if (var5 == -1) {
                     var5 = this.getFreeSlot();
                     if (this.m_slots[var5].object != null) {
                        this.m_slots[var5].stopPlaying();
                        this.m_slots[var5].object = null;
                     }

                     this.m_slots[var5].playSound(var3, var4);
                  }
               }
            }

            this.stopNotPlaying();
            this.m_added.clear();
            this.m_objectPool.release((List)this.m_objects);
            this.m_objects.clear();
         }
      }
   }

   void addObjectsFromChunks() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
         if (!var2.ignore) {
            int var3 = IsoChunkMap.ChunkGridWidth / 2;
            int var4 = IsoChunkMap.ChunkGridWidth / 2;

            for(int var5 = -1; var5 <= 1; ++var5) {
               for(int var6 = -1; var6 <= 1; ++var6) {
                  IsoChunk var7 = var2.getChunk(var3 + var6, var4 + var5);
                  if (var7 != null) {
                     Set var8 = var7.m_objectEmitterData.m_objects.keySet();
                     Iterator var9 = var8.iterator();

                     while(var9.hasNext()) {
                        IsoObject var10 = (IsoObject)var9.next();
                        this.addObject(var10, (PerObjectLogic)var7.m_objectEmitterData.m_objects.get(var10));
                     }
                  }
               }
            }
         }
      }

   }

   float getClosestListener(float var1, float var2, float var3) {
      float var4 = 3.4028235E38F;

      for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
         IsoPlayer var6 = IsoPlayer.players[var5];
         if (var6 != null && var6.getCurrentSquare() != null) {
            float var7 = var6.getX();
            float var8 = var6.getY();
            float var9 = var6.getZ();
            float var10 = IsoUtils.DistanceToSquared(var7, var8, var9 * 3.0F, var1, var2, var3 * 3.0F);
            var10 *= PZMath.pow(var6.getHearDistanceModifier(), 2.0F);
            if (var10 < var4) {
               var4 = var10;
            }
         }
      }

      return var4;
   }

   boolean shouldPlay(IsoObject var1, PerObjectLogic var2) {
      if (var1 == null) {
         return false;
      } else {
         return var1.getObjectIndex() == -1 ? false : var2.shouldPlaySound();
      }
   }

   int getExistingSlot(IsoObject var1) {
      for(int var2 = 0; var2 < this.m_slots.length; ++var2) {
         if (this.m_slots[var2].object == var1) {
            return var2;
         }
      }

      return -1;
   }

   int getFreeSlot() {
      for(int var1 = 0; var1 < this.m_slots.length; ++var1) {
         if (!this.m_slots[var1].playing) {
            return var1;
         }
      }

      return -1;
   }

   void stopNotPlaying() {
      for(int var1 = 0; var1 < this.m_slots.length; ++var1) {
         Slot var2 = this.m_slots[var1];
         if (!var2.playing) {
            var2.stopPlaying();
            var2.object = null;
         }
      }

   }

   public void render() {
      if (DebugOptions.instance.ObjectAmbientEmitterRender.getValue()) {
         IsoChunkMap var1 = IsoWorld.instance.CurrentCell.ChunkMap[IsoCamera.frameState.playerIndex];
         int var2;
         if (!var1.ignore) {
            var2 = IsoChunkMap.ChunkGridWidth / 2;
            int var3 = IsoChunkMap.ChunkGridWidth / 2;

            for(int var4 = -1; var4 <= 1; ++var4) {
               for(int var5 = -1; var5 <= 1; ++var5) {
                  IsoChunk var6 = var1.getChunk(var2 + var5, var3 + var4);
                  if (var6 != null) {
                     Set var7 = var6.m_objectEmitterData.m_objects.keySet();
                     Iterator var8 = var7.iterator();

                     while(var8.hasNext()) {
                        IsoObject var9 = (IsoObject)var8.next();
                        if (var9.square.z == (int)IsoCamera.frameState.CamCharacterZ) {
                           var9.getFacingPosition(tempVector2);
                           float var10 = tempVector2.x;
                           float var11 = tempVector2.y;
                           float var12 = (float)var9.square.z;
                           LineDrawer.addLine(var10 - 0.45F, var11 - 0.45F, var12, var10 + 0.45F, var11 + 0.45F, var12, 0.5F, 0.5F, 0.5F, (String)null, false);
                        }
                     }
                  }
               }
            }
         }

         for(var2 = 0; var2 < this.m_slots.length; ++var2) {
            Slot var13 = this.m_slots[var2];
            if (var13.playing) {
               IsoObject var14 = var13.object;
               var14.getFacingPosition(tempVector2);
               float var15 = tempVector2.x;
               float var16 = tempVector2.y;
               float var17 = (float)var14.square.z;
               LineDrawer.addLine(var15 - 0.45F, var16 - 0.45F, var17, var15 + 0.45F, var16 + 0.45F, var17, 0.0F, 0.0F, 1.0F, (String)null, false);
            }
         }

      }
   }

   public static void Reset() {
      if (instance != null) {
         for(int var0 = 0; var0 < instance.m_slots.length; ++var0) {
            instance.m_slots[var0].stopPlaying();
            instance.m_slots[var0].object = null;
            instance.m_slots[var0].playing = false;
         }

      }
   }

   static final class Slot {
      IsoObject object = null;
      PerObjectLogic logic = null;
      BaseSoundEmitter emitter = null;
      long instance = 0L;
      boolean playing = false;

      Slot() {
      }

      void playSound(IsoObject var1, PerObjectLogic var2) {
         if (this.emitter == null) {
            this.emitter = (BaseSoundEmitter)(Core.SoundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
         }

         var1.getFacingPosition(ObjectAmbientEmitters.tempVector2);
         this.emitter.setPos(ObjectAmbientEmitters.tempVector2.getX(), ObjectAmbientEmitters.tempVector2.getY(), (float)var1.square.z);
         this.object = var1;
         this.logic = var2;
         String var3 = var2.getSoundName();
         if (!this.emitter.isPlaying(var3)) {
            this.emitter.stopAll();
            FMODSoundEmitter var4 = (FMODSoundEmitter)Type.tryCastTo(this.emitter, FMODSoundEmitter.class);
            if (var4 != null) {
               var4.clearParameters();
            }

            this.instance = this.emitter.playSoundImpl(var3, (IsoObject)null);
            var2.startPlaying(this.emitter, this.instance);
         }

         var2.checkParameters(this.emitter, this.instance);
         this.playing = true;
         this.emitter.tick();
      }

      void stopPlaying() {
         if (this.emitter != null && this.instance != 0L) {
            this.logic.stopPlaying(this.emitter, this.instance);
            if (this.emitter.hasSustainPoints(this.instance)) {
               this.emitter.triggerCue(this.instance);
               this.instance = 0L;
            } else {
               this.emitter.stopAll();
               this.instance = 0L;
            }
         }
      }
   }

   static enum PowerPolicy {
      NotRequired,
      InteriorHydro,
      ExteriorOK;

      private PowerPolicy() {
      }
   }

   public static final class DoorLogic extends PerObjectLogic {
      public DoorLogic() {
      }

      public boolean shouldPlaySound() {
         return true;
      }

      public String getSoundName() {
         return "DoorAmbiance";
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         this.parameterValue1 = 0.0F / 0.0F;
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
         IsoDoor var4 = (IsoDoor)Type.tryCastTo(this.object, IsoDoor.class);
         IsoThumpable var5 = (IsoThumpable)Type.tryCastTo(this.object, IsoThumpable.class);
         float var6 = 0.0F;
         if (var4 != null && var4.IsOpen()) {
            var6 = 1.0F;
         }

         if (var5 != null && var5.IsOpen()) {
            var6 = 1.0F;
         }

         this.setParameterValue1(var1, var2, "DoorWindowOpen", var6);
      }
   }

   public static final class WindowLogic extends PerObjectLogic {
      public WindowLogic() {
      }

      public boolean shouldPlaySound() {
         return AmbientStreamManager.instance.isParameterInsideTrue();
      }

      public String getSoundName() {
         return "WindowAmbiance";
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         this.parameterValue1 = 0.0F / 0.0F;
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
         IsoWindow var4 = (IsoWindow)Type.tryCastTo(this.object, IsoWindow.class);
         float var5 = !var4.IsOpen() && !var4.isDestroyed() ? 0.0F : 1.0F;
         if (var5 == 1.0F) {
            IsoBarricade var6 = var4.getBarricadeOnSameSquare();
            IsoBarricade var7 = var4.getBarricadeOnOppositeSquare();
            int var8 = var6 == null ? 0 : var6.getNumPlanks();
            int var9 = var7 == null ? 0 : var7.getNumPlanks();
            if ((var6 == null || !var6.isMetal()) && (var7 == null || !var7.isMetal())) {
               if (var8 > 0 || var9 > 0) {
                  var5 = 1.0F - (float)PZMath.max(var8, var9) / 4.0F;
               }
            } else {
               var5 = 0.0F;
            }
         }

         this.setParameterValue1(var1, var2, "DoorWindowOpen", var5);
      }
   }

   static final class ObjectWithDistance {
      IsoObject object;
      PerObjectLogic logic;
      float distSq;

      ObjectWithDistance() {
      }
   }

   public abstract static class PerObjectLogic {
      public IsoObject object;
      public float parameterValue1 = 0.0F / 0.0F;

      public PerObjectLogic() {
      }

      public PerObjectLogic init(IsoObject var1) {
         this.object = var1;
         this.parameterValue1 = 0.0F / 0.0F;
         return this;
      }

      void setParameterValue1(BaseSoundEmitter var1, long var2, String var4, float var5) {
         if (var5 != this.parameterValue1) {
            this.parameterValue1 = var5;
            FMOD_STUDIO_PARAMETER_DESCRIPTION var6 = FMODManager.instance.getParameterDescription(var4);
            var1.setParameterValue(var2, var6, var5);
         }
      }

      void setParameterValue1(BaseSoundEmitter var1, long var2, FMOD_STUDIO_PARAMETER_DESCRIPTION var4, float var5) {
         if (var5 != this.parameterValue1) {
            this.parameterValue1 = var5;
            var1.setParameterValue(var2, var4, var5);
         }
      }

      public abstract boolean shouldPlaySound();

      public abstract String getSoundName();

      public abstract void startPlaying(BaseSoundEmitter var1, long var2);

      public abstract void stopPlaying(BaseSoundEmitter var1, long var2);

      public abstract void checkParameters(BaseSoundEmitter var1, long var2);
   }

   public static final class ChunkData {
      final HashMap<IsoObject, PerObjectLogic> m_objects = new HashMap();

      public ChunkData() {
      }

      public boolean hasObject(IsoObject var1) {
         return this.m_objects.containsKey(var1);
      }

      public void addObject(IsoObject var1, PerObjectLogic var2) {
         if (!this.m_objects.containsKey(var1)) {
            this.m_objects.put(var1, var2);
         }
      }

      public void removeObject(IsoObject var1) {
         this.m_objects.remove(var1);
      }

      public void reset() {
         this.m_objects.clear();
      }
   }

   public static final class WaterDripLogic extends PerObjectLogic {
      public WaterDripLogic() {
      }

      public boolean shouldPlaySound() {
         return this.object.sprite != null && this.object.sprite.getProperties().Is(IsoFlagType.waterPiped) && (float)this.object.getWaterAmount() > 0.0F;
      }

      public String getSoundName() {
         return "WaterDrip";
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
         if (this.object.sprite != null && this.object.sprite.getProperties().Is("SinkType")) {
            byte var10000;
            switch (this.object.sprite.getProperties().Val("SinkType")) {
               case "Ceramic":
                  var10000 = 1;
                  break;
               case "Metal":
                  var10000 = 2;
                  break;
               default:
                  var10000 = 0;
            }

            byte var5 = var10000;
            this.setParameterValue1(var1, var2, "SinkType", (float)var5);
         }

      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         this.parameterValue1 = 0.0F / 0.0F;
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
      }
   }

   public static final class TreeAmbianceLogic extends PerObjectLogic {
      public TreeAmbianceLogic() {
      }

      public boolean shouldPlaySound() {
         return true;
      }

      public String getSoundName() {
         return "TreeAmbiance";
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
         FMODSoundEmitter var4 = (FMODSoundEmitter)Type.tryCastTo(var1, FMODSoundEmitter.class);
         if (var4 != null) {
            var4.addParameter(new ParameterCurrentZone(this.object));
         }

         var1.playAmbientLoopedImpl("BirdInTree");
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         var1.stopOrTriggerSoundByName("BirdInTree");
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
      }
   }

   public static final class TentAmbianceLogic extends PerObjectLogic {
      public TentAmbianceLogic() {
      }

      public boolean shouldPlaySound() {
         return this.object.sprite != null && this.object.sprite.getName() != null && this.object.sprite.getName().startsWith("camping_01") && (this.object.sprite.tileSheetIndex == 0 || this.object.sprite.tileSheetIndex == 3);
      }

      public String getSoundName() {
         return "TentAmbiance";
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
      }
   }

   public static final class AmbientSoundLogic extends PerObjectLogic {
      PowerPolicy powerPolicy;
      boolean bHasGeneratorParameter;

      public AmbientSoundLogic() {
         this.powerPolicy = ObjectAmbientEmitters.PowerPolicy.NotRequired;
         this.bHasGeneratorParameter = false;
      }

      public PerObjectLogic init(IsoObject var1) {
         super.init(var1);
         String var2 = this.getSoundName();
         this.powerPolicy = (PowerPolicy)ObjectAmbientEmitters.getInstance().powerPolicyMap.getOrDefault(var2, ObjectAmbientEmitters.PowerPolicy.NotRequired);
         if (this.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired) {
            GameSound var3 = GameSounds.getSound(var2);
            this.bHasGeneratorParameter = var3 != null && var3.numClipsUsingParameter("Generator") > 0;
         }

         return this;
      }

      public boolean shouldPlaySound() {
         boolean var1;
         if (this.powerPolicy == ObjectAmbientEmitters.PowerPolicy.InteriorHydro) {
            var1 = this.object.square.haveElectricity() || IsoWorld.instance.isHydroPowerOn() && this.object.square.getRoom() != null;
            if (!var1) {
               return false;
            }
         }

         if (this.powerPolicy == ObjectAmbientEmitters.PowerPolicy.ExteriorOK) {
            var1 = this.object.square.haveElectricity() || IsoWorld.instance.isHydroPowerOn();
            if (!var1) {
               return false;
            }
         }

         if (this.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired && !IsoWorld.instance.isHydroPowerOn() && !this.bHasGeneratorParameter) {
            return false;
         } else {
            PropertyContainer var2 = this.object.getProperties();
            return var2 != null && var2.Is("AmbientSound");
         }
      }

      public String getSoundName() {
         return this.object.getProperties().Val("AmbientSound");
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         this.parameterValue1 = 0.0F / 0.0F;
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
         if (this.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired) {
            this.setParameterValue1(var1, var2, "Generator", IsoWorld.instance.isHydroPowerOn() ? 0.0F : 1.0F);
         }

      }
   }

   public static final class FridgeHumLogic extends PerObjectLogic {
      static String[] s_soundNames = new String[]{"FridgeHumA", "FridgeHumB", "FridgeHumC", "FridgeHumD", "FridgeHumE", "FridgeHumF"};
      int choice = -1;

      public FridgeHumLogic() {
      }

      public PerObjectLogic init(IsoObject var1) {
         super.init(var1);
         this.choice = Rand.Next(6);
         return this;
      }

      public boolean shouldPlaySound() {
         ItemContainer var1 = this.object.getContainerByEitherType("fridge", "freezer");
         return var1 != null && var1.isPowered();
      }

      public String getSoundName() {
         return s_soundNames[this.choice];
      }

      public void startPlaying(BaseSoundEmitter var1, long var2) {
      }

      public void stopPlaying(BaseSoundEmitter var1, long var2) {
         this.parameterValue1 = 0.0F / 0.0F;
      }

      public void checkParameters(BaseSoundEmitter var1, long var2) {
         this.setParameterValue1(var1, var2, "Generator", IsoWorld.instance.isHydroPowerOn() ? 0.0F : 1.0F);
      }
   }
}
