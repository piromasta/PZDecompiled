package zombie.iso;

import fmod.javafmod;
import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.WorldSoundManager;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class Helicopter {
   private static float MAX_BOTHER_SECONDS = 60.0F;
   private static float MAX_UNSEEN_SECONDS = 15.0F;
   private static int RADIUS_HOVER = 50;
   private static int RADIUS_SEARCH = 100;
   protected State state;
   public IsoGameCharacter target;
   protected float timeSinceChopperSawPlayer;
   protected float hoverTime;
   protected float searchTime;
   public float x;
   public float y;
   protected float targetX;
   protected float targetY;
   protected Vector2 move = new Vector2();
   protected boolean bActive;
   protected static long inst;
   protected static FMOD_STUDIO_EVENT_DESCRIPTION event;
   protected boolean bSoundStarted;
   protected float volume;
   protected float occlusion;

   public Helicopter() {
   }

   public void pickRandomTarget() {
      ArrayList var1;
      if (GameServer.bServer) {
         var1 = GameServer.getPlayers();
      } else {
         if (GameClient.bClient) {
            throw new IllegalStateException("can't call this on the client");
         }

         var1 = new ArrayList();

         for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
            IsoPlayer var3 = IsoPlayer.players[var2];
            if (var3 != null && var3.isAlive()) {
               var1.add(var3);
            }
         }
      }

      if (var1.isEmpty()) {
         this.bActive = false;
         this.target = null;
      } else {
         this.setTarget((IsoGameCharacter)var1.get(Rand.Next(var1.size())));
      }
   }

   public void setTarget(IsoGameCharacter var1) {
      this.target = var1;
      this.x = this.target.getX() + 1000.0F;
      this.y = this.target.getY() + 1000.0F;
      this.targetX = this.target.getX();
      this.targetY = this.target.getY();
      this.move.x = this.targetX - this.x;
      this.move.y = this.targetY - this.y;
      this.move.normalize();
      this.move.setLength(0.5F);
      this.state = Helicopter.State.Arriving;
      this.bActive = true;
      DebugLog.log("chopper: activated");
   }

   protected void changeState(State var1) {
      DebugLog.log("chopper: state " + this.state + " -> " + var1);
      this.state = var1;
   }

   public void update() {
      if (this.bActive) {
         if (GameClient.bClient) {
            this.updateSound();
            this.checkMusicIntensityEvent();
         } else {
            float var1 = 1.0F;
            if (GameServer.bServer) {
               if (!GameServer.Players.contains(this.target)) {
                  this.target = null;
               }
            } else {
               var1 = GameTime.getInstance().getTrueMultiplier();
            }

            switch (this.state) {
               case Arriving:
                  if (this.target != null && !this.target.isDead()) {
                     if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 4.0F) {
                        this.changeState(Helicopter.State.Hovering);
                        this.hoverTime = 0.0F;
                        this.searchTime = 0.0F;
                        this.timeSinceChopperSawPlayer = 0.0F;
                     } else {
                        this.targetX = this.target.getX();
                        this.targetY = this.target.getY();
                        this.move.x = this.targetX - this.x;
                        this.move.y = this.targetY - this.y;
                        this.move.normalize();
                        this.move.setLength(0.75F);
                     }
                  } else {
                     this.changeState(Helicopter.State.Leaving);
                  }
                  break;
               case Hovering:
                  if (this.target != null && !this.target.isDead()) {
                     this.hoverTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * var1;
                     if (this.hoverTime + this.searchTime > MAX_BOTHER_SECONDS) {
                        this.changeState(Helicopter.State.Leaving);
                     } else {
                        if (!this.isTargetVisible()) {
                           this.timeSinceChopperSawPlayer += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * var1;
                           if (this.timeSinceChopperSawPlayer > MAX_UNSEEN_SECONDS) {
                              this.changeState(Helicopter.State.Searching);
                              break;
                           }
                        }

                        if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 1.0F) {
                           this.targetX = this.target.getX() + (float)(Rand.Next(RADIUS_HOVER * 2) - RADIUS_HOVER);
                           this.targetY = this.target.getY() + (float)(Rand.Next(RADIUS_HOVER * 2) - RADIUS_HOVER);
                           this.move.x = this.targetX - this.x;
                           this.move.y = this.targetY - this.y;
                           this.move.normalize();
                           this.move.setLength(0.5F);
                        }
                     }
                  } else {
                     this.changeState(Helicopter.State.Leaving);
                  }
                  break;
               case Searching:
                  if (this.target != null && !this.target.isDead()) {
                     this.searchTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * var1;
                     if (this.hoverTime + this.searchTime > MAX_BOTHER_SECONDS) {
                        this.changeState(Helicopter.State.Leaving);
                     } else if (this.isTargetVisible()) {
                        this.timeSinceChopperSawPlayer = 0.0F;
                        this.changeState(Helicopter.State.Hovering);
                     } else if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 1.0F) {
                        this.targetX = this.target.getX() + (float)(Rand.Next(RADIUS_SEARCH * 2) - RADIUS_SEARCH);
                        this.targetY = this.target.getY() + (float)(Rand.Next(RADIUS_SEARCH * 2) - RADIUS_SEARCH);
                        this.move.x = this.targetX - this.x;
                        this.move.y = this.targetY - this.y;
                        this.move.normalize();
                        this.move.setLength(0.5F);
                     }
                  } else {
                     this.state = Helicopter.State.Leaving;
                  }
                  break;
               case Leaving:
                  boolean var2 = false;
                  if (GameServer.bServer) {
                     ArrayList var7 = GameServer.getPlayers();

                     for(int var9 = 0; var9 < var7.size(); ++var9) {
                        IsoPlayer var5 = (IsoPlayer)var7.get(var9);
                        if (IsoUtils.DistanceToSquared(this.x, this.y, var5.getX(), var5.getY()) < 1000000.0F) {
                           var2 = true;
                           break;
                        }
                     }
                  } else {
                     for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
                        IsoPlayer var4 = IsoPlayer.players[var3];
                        if (var4 != null && IsoUtils.DistanceToSquared(this.x, this.y, var4.getX(), var4.getY()) < 1000000.0F) {
                           var2 = true;
                           break;
                        }
                     }
                  }

                  if (!var2) {
                     this.deactivate();
                     return;
                  }
            }

            if (Rand.Next(Rand.AdjustForFramerate(300)) == 0) {
               WorldSoundManager.instance.addSound((Object)null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, 500, 500);
            }

            float var6 = this.move.x * GameTime.getInstance().getThirtyFPSMultiplier();
            float var8 = this.move.y * GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.state != Helicopter.State.Leaving && IsoUtils.DistanceToSquared(this.x + var6, this.y + var8, this.targetX, this.targetY) > IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY)) {
               this.x = this.targetX;
               this.y = this.targetY;
            } else {
               this.x += var6;
               this.y += var8;
            }

            if (GameServer.bServer) {
               GameServer.sendHelicopter(this.x, this.y, this.bActive);
            }

            this.updateSound();
            this.checkMusicIntensityEvent();
         }
      }
   }

   protected void updateSound() {
      if (!GameServer.bServer) {
         if (!Core.SoundDisabled) {
            if (FMODManager.instance.getNumListeners() != 0) {
               GameSound var1 = GameSounds.getSound("Helicopter");
               if (var1 != null && !var1.clips.isEmpty()) {
                  if (inst == 0L) {
                     GameSoundClip var2 = var1.getRandomClip();
                     event = var2.eventDescription;
                     if (event != null) {
                        javafmod.FMOD_Studio_LoadEventSampleData(event.address);
                        inst = javafmod.FMOD_Studio_System_CreateEventInstance(event.address);
                     }
                  }

                  if (inst != 0L) {
                     float var5 = SoundManager.instance.getSoundVolume();
                     var5 = 1.0F;
                     var5 *= var1.getUserVolume();
                     if (var5 != this.volume) {
                        javafmod.FMOD_Studio_EventInstance_SetVolume(inst, var5);
                        this.volume = var5;
                     }

                     javafmod.FMOD_Studio_EventInstance3D(inst, this.x, this.y, 200.0F);
                     float var3 = 0.0F;
                     if (IsoPlayer.numPlayers == 1) {
                        IsoGridSquare var4 = IsoPlayer.getInstance().getCurrentSquare();
                        if (var4 != null && !var4.Is(IsoFlagType.exterior)) {
                           var3 = 1.0F;
                        }
                     }

                     if (this.occlusion != var3) {
                        this.occlusion = var3;
                        javafmod.FMOD_Studio_EventInstance_SetParameterByName(inst, "Occlusion", this.occlusion);
                     }

                     if (!this.bSoundStarted) {
                        javafmod.FMOD_Studio_StartEvent(inst);
                        this.bSoundStarted = true;
                     }
                  }

               }
            }
         }
      }
   }

   protected boolean isTargetVisible() {
      if (this.target != null && !this.target.isDead()) {
         IsoGridSquare var1 = this.target.getCurrentSquare();
         if (var1 == null) {
            return false;
         } else if (!var1.getProperties().Is(IsoFlagType.exterior)) {
            return false;
         } else {
            Zone var2 = var1.getZone();
            if (var2 == null) {
               return true;
            } else {
               return !"Forest".equals(var2.getType()) && !"DeepForest".equals(var2.getType());
            }
         }
      } else {
         return false;
      }
   }

   public void deactivate() {
      if (this.bActive) {
         this.bActive = false;
         if (this.bSoundStarted) {
            javafmod.FMOD_Studio_EventInstance_Stop(inst, false);
            this.bSoundStarted = false;
         }

         if (GameServer.bServer) {
            GameServer.sendHelicopter(this.x, this.y, this.bActive);
         }

         DebugLog.log("chopper: deactivated");
      }

   }

   public boolean isActive() {
      return this.bActive;
   }

   public void clientSync(float var1, float var2, boolean var3) {
      if (GameClient.bClient) {
         this.x = var1;
         this.y = var2;
         if (!var3) {
            this.deactivate();
         }

         this.bActive = var3;
      }
   }

   public void save(ByteBuffer var1) {
      var1.put((byte)(this.bActive ? 1 : 0));
      var1.putInt(this.state == null ? 0 : this.state.ordinal());
      var1.putFloat(this.x);
      var1.putFloat(this.y);
   }

   public void load(ByteBuffer var1, int var2) {
      this.bActive = var1.get() == 1;
      this.state = Helicopter.State.values()[var1.getInt()];
      this.x = var1.getFloat();
      this.y = var1.getFloat();
      DebugLog.General.debugln("Re-Initializing Chopper %s", this.bActive);
      if (this.bActive && !GameServer.bServer && !GameClient.bClient) {
         this.target = IsoPlayer.players[0];
         if (this.target == null) {
            this.bActive = false;
         } else {
            this.targetX = this.target.getX();
            this.targetY = this.target.getY();
            DebugLog.General.debugln("target at %.4f/%.4f", this.targetX, this.targetY);
            this.move.x = this.targetX - this.x;
            this.move.y = this.targetY - this.y;
            this.move.normalize();
            this.move.setLength(0.5F);
         }
      }
   }

   private void checkMusicIntensityEvent() {
      if (!GameServer.bServer) {
         if (this.bActive) {
            for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
               IsoPlayer var2 = IsoPlayer.players[var1];
               if (var2 != null && !var2.isDeaf() && !var2.isDead()) {
                  float var3 = IsoUtils.DistanceToSquared(this.x, this.y, var2.getX(), var2.getY());
                  if (!(var3 > 2500.0F)) {
                     var2.triggerMusicIntensityEvent("HelicopterOverhead");
                     break;
                  }
               }
            }

         }
      }
   }

   private static enum State {
      Arriving,
      Hovering,
      Searching,
      Leaving;

      private State() {
      }
   }
}
