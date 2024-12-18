package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import zombie.CollisionManager;
import zombie.GameTime;
import zombie.MovingObjectUpdateScheduler;
import zombie.SoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.State;
import zombie.ai.astar.Mover;
import zombie.ai.states.AttackState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.CrawlingZombieTurnState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.animals.AnimalAttackState;
import zombie.ai.states.animals.AnimalClimbOverFenceState;
import zombie.ai.states.animals.AnimalZoneState;
import zombie.audio.TreeSoundManager;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.Role;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ZombiePopulationManager;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public class IsoMovingObject extends IsoObject implements Mover {
   public static TreeSoundManager treeSoundMgr = new TreeSoundManager();
   public static final int MAX_ZOMBIES_EATING = 3;
   private static int IDCount = 0;
   private static final Vector2 tempo = new Vector2();
   public boolean noDamage = false;
   public IsoGridSquare last = null;
   private float m_lastX;
   private float ly;
   private float lz;
   private float nx;
   private float ny;
   private float x;
   private float y;
   private float z;
   public Vector2 reqMovement = new Vector2();
   public IsoSpriteInstance def = null;
   protected IsoGridSquare current = null;
   protected Vector2 hitDir = new Vector2();
   protected int ID = 0;
   protected IsoGridSquare movingSq = null;
   protected boolean solid = true;
   protected float width = 0.24F;
   protected boolean shootable = true;
   protected boolean Collidable = true;
   private float scriptnx = 0.0F;
   private float scriptny = 0.0F;
   protected String ScriptModule = "none";
   protected Vector2 movementLastFrame = new Vector2();
   protected float weight = 1.0F;
   boolean bOnFloor = false;
   private boolean closeKilled = false;
   private String collideType = null;
   private float lastCollideTime = 0.0F;
   private int TimeSinceZombieAttack = 1000000;
   private boolean collidedE = false;
   private boolean collidedN = false;
   private IsoObject CollidedObject = null;
   private boolean collidedS = false;
   private boolean collidedThisFrame = false;
   private boolean collidedW = false;
   private boolean CollidedWithDoor = false;
   private boolean collidedWithVehicle = false;
   private boolean destroyed = false;
   private boolean firstUpdate = true;
   private float impulsex = 0.0F;
   private float impulsey = 0.0F;
   private float limpulsex = 0.0F;
   private float limpulsey = 0.0F;
   private float hitForce = 0.0F;
   private float hitFromAngle;
   private int PathFindIndex = -1;
   private float StateEventDelayTimer = 0.0F;
   private Thumpable thumpTarget = null;
   private boolean bAltCollide = false;
   private IsoZombie lastTargettedBy = null;
   private float feelersize = 0.5F;
   private final ArrayList<IsoZombie> eatingZombies = new ArrayList();
   private boolean zombiesDontAttack = false;

   public IsoMovingObject(IsoCell var1) {
      this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
      if (var1 != null) {
         this.ID = IDCount++;
         if (this.getCell().isSafeToAdd()) {
            this.getCell().getObjectList().add(this);
         } else {
            this.getCell().getAddList().add(this);
         }

      }
   }

   public IsoMovingObject(IsoCell var1, boolean var2) {
      this.ID = IDCount++;
      this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
      if (var2) {
         if (this.getCell().isSafeToAdd()) {
            this.getCell().getObjectList().add(this);
         } else {
            this.getCell().getAddList().add(this);
         }
      }

   }

   public IsoMovingObject(IsoCell var1, IsoGridSquare var2, IsoSprite var3, boolean var4) {
      this.ID = IDCount++;
      this.sprite = var3;
      if (var4) {
         if (this.getCell().isSafeToAdd()) {
            this.getCell().getObjectList().add(this);
         } else {
            this.getCell().getAddList().add(this);
         }
      }

   }

   public IsoMovingObject() {
      this.ID = IDCount++;
      this.getCell().getAddList().add(this);
   }

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "{  Name:" + this.getName() + ",  ID:" + this.getID() + " }";
   }

   public static int getIDCount() {
      return IDCount;
   }

   public static void setIDCount(int var0) {
      IDCount = var0;
   }

   public IsoBuilding getBuilding() {
      if (this.current == null) {
         return null;
      } else {
         IsoRoom var1 = this.current.getRoom();
         return var1 == null ? null : var1.building;
      }
   }

   public IWorldRegion getMasterRegion() {
      return this.current != null ? this.current.getIsoWorldRegion() : null;
   }

   public float getWeight() {
      return this.weight;
   }

   public void setWeight(float var1) {
      this.weight = var1;
   }

   public float getWeight(float var1, float var2) {
      return this.weight;
   }

   public void onMouseRightClick(int var1, int var2) {
      if (this.square.getZ() == PZMath.fastfloor(IsoPlayer.getInstance().getZ()) && this.DistToProper(IsoPlayer.getInstance()) <= 2.0F) {
         IsoPlayer.getInstance().setDragObject(this);
      }

   }

   public String getObjectName() {
      return "IsoMovingObject";
   }

   public void onMouseRightReleased() {
   }

   public void collideWith(IsoObject var1) {
      if (this instanceof IsoGameCharacter && var1 instanceof IsoGameCharacter) {
         LuaEventManager.triggerEvent("OnCharacterCollide", this, var1);
      } else {
         LuaEventManager.triggerEvent("OnObjectCollide", this, var1);
      }

   }

   public void doStairs() {
      if (this.current != null) {
         if (this.last != null) {
            if (this instanceof IsoGameCharacter && ((IsoGameCharacter)this).isAnimal() && !((IsoAnimal)this).canClimbStairs()) {
            }

            if (!(this instanceof IsoPhysicsObject)) {
               IsoGridSquare var1 = this.current;
               if ((var1.Has(IsoObjectType.stairsTN) || var1.Has(IsoObjectType.stairsTW)) && this.getZ() - (float)PZMath.fastfloor(this.getZ()) < 0.1F) {
                  IsoGridSquare var2 = IsoWorld.instance.CurrentCell.getGridSquare(var1.x, var1.y, var1.z - 1);
                  if (var2 != null && (var2.Has(IsoObjectType.stairsTN) || var2.Has(IsoObjectType.stairsTW))) {
                     var1 = var2;
                  }
               }

               if (this instanceof IsoGameCharacter && (this.last.Has(IsoObjectType.stairsTN) || this.last.Has(IsoObjectType.stairsTW))) {
                  this.setZ((float)Math.round(this.getZ()));
               }

               float var4 = this.getZ();
               if (var1.HasStairs()) {
                  var4 = var1.getApparentZ(this.getX() - (float)var1.getX(), this.getY() - (float)var1.getY());
               }

               if (this instanceof IsoGameCharacter) {
                  State var3 = ((IsoGameCharacter)this).getCurrentState();
                  if (var3 == ClimbOverFenceState.instance() || var3 == ClimbThroughWindowState.instance()) {
                     if (var1.HasStairs() && this.getZ() > var4) {
                        this.setZ(Math.max(var4, this.getZ() - 0.075F * GameTime.getInstance().getMultiplier()));
                     }

                     return;
                  }
               }

               if (Math.abs(var4 - this.getZ()) < 0.95F) {
                  this.setZ(var4);
               }

            }
         }
      }
   }

   private void handleSlopedSurface() {
      if (!(this instanceof IsoPhysicsObject)) {
         if (this.current != null) {
            if (!(this instanceof IsoGameCharacter) || ((IsoGameCharacter)this).getVehicle() == null) {
               float var1;
               if (this.last != null && this.last != this.current && this.last.hasSlopedSurface()) {
                  var1 = this.last.getSlopedSurfaceHeightMax();
                  if (var1 == 1.0F) {
                     this.setZ((float)Math.round(this.getZ()));
                  }
               }

               var1 = this.current.getSlopedSurfaceHeight(this.getX() % 1.0F, this.getY() % 1.0F);
               if (!(var1 <= 0.0F)) {
                  this.setZ((float)this.current.z + var1);
               }
            }
         }
      }
   }

   public int getID() {
      return this.ID;
   }

   public void setID(int var1) {
      this.ID = var1;
   }

   public int getPathFindIndex() {
      return this.PathFindIndex;
   }

   public void setPathFindIndex(int var1) {
      this.PathFindIndex = var1;
   }

   public float getScreenX() {
      return IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
   }

   public float getScreenY() {
      return IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
   }

   public Thumpable getThumpTarget() {
      return this.thumpTarget;
   }

   public void setThumpTarget(Thumpable var1) {
      this.thumpTarget = var1;
   }

   public Vector2 getVectorFromDirection(Vector2 var1) {
      return getVectorFromDirection(var1, this.dir);
   }

   public static Vector2 getVectorFromDirection(Vector2 var0, IsoDirections var1) {
      if (var0 == null) {
         DebugLog.General.warn("Supplied vector2 is null. Cannot be processed. Using fail-safe fallback.");
         var0 = new Vector2();
      }

      var0.x = 0.0F;
      var0.y = 0.0F;
      switch (var1) {
         case S:
            var0.x = 0.0F;
            var0.y = 1.0F;
            break;
         case N:
            var0.x = 0.0F;
            var0.y = -1.0F;
            break;
         case E:
            var0.x = 1.0F;
            var0.y = 0.0F;
            break;
         case W:
            var0.x = -1.0F;
            var0.y = 0.0F;
            break;
         case NW:
            var0.x = -1.0F;
            var0.y = -1.0F;
            break;
         case NE:
            var0.x = 1.0F;
            var0.y = -1.0F;
            break;
         case SW:
            var0.x = -1.0F;
            var0.y = 1.0F;
            break;
         case SE:
            var0.x = 1.0F;
            var0.y = 1.0F;
      }

      var0.normalize();
      return var0;
   }

   public Vector3 getPosition(Vector3 var1) {
      var1.set(this.getX(), this.getY(), this.getZ());
      return var1;
   }

   public Vector3f getPosition(Vector3f var1) {
      var1.set(this.getX(), this.getY(), this.getZ());
      return var1;
   }

   public void setPosition(float var1, float var2) {
      this.setX(var1);
      this.setY(var2);
   }

   public void setPosition(Vector2 var1) {
      this.setPosition(var1.x, var1.y);
   }

   public void setPosition(float var1, float var2, float var3) {
      this.setX(var1);
      this.setY(var2);
      this.setZ(var3);
   }

   public float getX() {
      return this.x;
   }

   public float setX(float var1) {
      this.x = var1;
      this.setNextX(var1);
      this.setScriptNextX(var1);
      return this.x;
   }

   public void setForceX(float var1) {
      this.setX(var1);
      this.setNextX(var1);
      this.setLastX(var1);
      this.setScriptNextX(var1);
   }

   public float getY() {
      return this.y;
   }

   public float setY(float var1) {
      this.y = var1;
      this.setNextY(var1);
      this.setScriptNextY(var1);
      return this.y;
   }

   public void setForceY(float var1) {
      if (this instanceof IsoPlayer && this != IsoPlayer.getInstance()) {
         boolean var2 = false;
      }

      this.setY(var1);
      this.setNextY(var1);
      this.setLastY(var1);
      this.setScriptNextY(var1);
   }

   public float getZ() {
      return this.z;
   }

   public float setZ(float var1) {
      this.z = var1;
      this.setLastZ(var1);
      return this.z;
   }

   public IsoGridSquare getSquare() {
      return this.current != null ? this.current : this.square;
   }

   public IsoBuilding getCurrentBuilding() {
      if (this.current == null) {
         return null;
      } else {
         return this.current.getRoom() == null ? null : this.current.getRoom().building;
      }
   }

   public float Hit(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5) {
      return 0.0F;
   }

   public void Move(Vector2 var1) {
      this.setNextX(this.getNextX() + var1.x * GameTime.instance.getMultiplier());
      this.setNextY(this.getNextY() + var1.y * GameTime.instance.getMultiplier());
      this.reqMovement.x = var1.x;
      this.reqMovement.y = var1.y;
      if (this instanceof IsoPlayer) {
         this.setCurrent(IsoWorld.instance.CurrentCell.getGridSquare((double)this.getX(), (double)this.getY(), (double)PZMath.fastfloor(this.getZ())));
      }

   }

   public void MoveUnmodded(Vector2 var1) {
      this.setNextX(this.getNextX() + var1.x);
      this.setNextY(this.getNextY() + var1.y);
      this.reqMovement.x = var1.x;
      this.reqMovement.y = var1.y;
      if (this instanceof IsoPlayer) {
         this.setCurrent(IsoWorld.instance.CurrentCell.getGridSquare((double)this.getX(), (double)this.getY(), (double)PZMath.fastfloor(this.getZ())));
      }

   }

   public boolean isCharacter() {
      return this instanceof IsoGameCharacter;
   }

   public float DistTo(int var1, int var2) {
      return IsoUtils.DistanceManhatten((float)var1, (float)var2, this.getX(), this.getY());
   }

   public float DistTo(IsoMovingObject var1) {
      return var1 == null ? 0.0F : IsoUtils.DistanceManhatten(this.getX(), this.getY(), var1.getX(), var1.getY());
   }

   public float DistToProper(IsoObject var1) {
      return IsoUtils.DistanceTo(this.getX(), this.getY(), var1.getX(), var1.getY());
   }

   public float DistToSquared(IsoMovingObject var1) {
      return IsoUtils.DistanceToSquared(this.getX(), this.getY(), var1.getX(), var1.getY());
   }

   public float DistToSquared(float var1, float var2) {
      return IsoUtils.DistanceToSquared(var1, var2, this.getX(), this.getY());
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      float var4 = var1.getFloat();
      float var5 = var1.getFloat();
      this.setX(this.setLastX(this.setNextX(this.setScriptNextX(var1.getFloat() + (float)(IsoWorld.saveoffsetx * IsoCell.CellSizeInSquares)))));
      this.setY(this.setLastY(this.setNextY(this.setScriptNextY(var1.getFloat() + (float)(IsoWorld.saveoffsety * IsoCell.CellSizeInSquares)))));
      this.setZ(this.setLastZ(var1.getFloat()));
      this.dir = IsoDirections.fromIndex(var1.getInt());
      if (var1.get() != 0) {
         if (this.table == null) {
            this.table = LuaManager.platform.newTable();
         }

         this.table.load(var1, var2);
      }

   }

   public String getDescription(String var1) {
      String var2 = this.getClass().getSimpleName() + " [" + var1 + "offset=(" + this.offsetX + ", " + this.offsetY + ") | " + var1 + "pos=(" + this.getX() + ", " + this.getY() + ", " + this.getZ() + ") | " + var1 + "dir=" + this.dir.name() + " ] ";
      return var2;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      DebugLog.Saving.trace("Saving: %s", this);
      var1.put((byte)(this.Serialize() ? 1 : 0));
      var1.put(IsoObject.factoryGetClassID(this.getObjectName()));
      var1.putFloat(this.offsetX);
      var1.putFloat(this.offsetY);
      var1.putFloat(this.getX());
      var1.putFloat(this.getY());
      var1.putFloat(this.getZ());
      var1.putInt(this.dir.index());
      if (this.table != null && !this.table.isEmpty()) {
         var1.put((byte)1);
         this.table.save(var1);
      } else {
         var1.put((byte)0);
      }

   }

   public void removeFromWorld() {
      IsoCell var1 = this.getCell();
      if (var1.isSafeToAdd()) {
         var1.getObjectList().remove(this);
         var1.getRemoveList().remove(this);
      } else {
         var1.getRemoveList().add(this);
      }

      var1.getAddList().remove(this);
      MovingObjectUpdateScheduler.instance.removeObject(this);
      super.removeFromWorld();
   }

   public void removeFromSquare() {
      if (this.current != null) {
         this.current.getMovingObjects().remove(this);
      }

      if (this.last != null) {
         this.last.getMovingObjects().remove(this);
      }

      if (this.movingSq != null) {
         this.movingSq.getMovingObjects().remove(this);
      }

      this.current = this.last = this.movingSq = null;
      if (this.square != null) {
         this.square.getStaticMovingObjects().remove(this);
      }

      super.removeFromSquare();
   }

   public IsoGridSquare getFuturWalkedSquare() {
      if (this.current != null) {
         IsoGridSquare var1 = this.getFeelerTile(this.feelersize);
         if (var1 != null && var1 != this.current) {
            return var1;
         }
      }

      return null;
   }

   public float getGlobalMovementMod() {
      return this.getGlobalMovementMod(true);
   }

   public float getGlobalMovementMod(boolean var1) {
      if (this.current != null && this.getZ() - (float)PZMath.fastfloor(this.getZ()) < 0.5F) {
         if (this.current.Has(IsoObjectType.tree) || this.current.hasBush()) {
            if (var1) {
               this.doTreeNoises();
            }

            for(int var2 = 1; var2 < this.current.getObjects().size(); ++var2) {
               IsoObject var3 = (IsoObject)this.current.getObjects().get(var2);
               if (var3 instanceof IsoTree) {
                  var3.setRenderEffect(RenderEffectType.Vegetation_Rustle);
               } else if (var3.isBush()) {
                  var3.setRenderEffect(RenderEffectType.Vegetation_Rustle);
               }
            }
         }

         IsoGridSquare var5 = this.getFeelerTile(this.feelersize);
         if (var5 != null && var5 != this.current && (var5.Has(IsoObjectType.tree) || var5.hasBush())) {
            if (var1) {
               this.doTreeNoises();
            }

            for(int var6 = 1; var6 < var5.getObjects().size(); ++var6) {
               IsoObject var4 = (IsoObject)var5.getObjects().get(var6);
               if (var4 instanceof IsoTree) {
                  var4.setRenderEffect(RenderEffectType.Vegetation_Rustle);
               } else if (var4.isBush()) {
                  var4.setRenderEffect(RenderEffectType.Vegetation_Rustle);
               }
            }
         }
      }

      return this.current != null && this.current.HasStairs() ? 0.75F : 1.0F;
   }

   private void doTreeNoises() {
      if (!GameServer.bServer) {
         if (!(this instanceof IsoPhysicsObject)) {
            if (this.current != null) {
               if (SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 20.0F)) {
                  treeSoundMgr.addSquare(this.current);
               }
            }
         }
      }
   }

   public void postupdate() {
      IsoGameCharacter var1 = (IsoGameCharacter)Type.tryCastTo(this, IsoGameCharacter.class);
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
      IsoZombie var3 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
      this.slideFeetAwayFromWalls();
      this.slideHeadAwayFromWalls();
      if (var2 != null && var2.isLocalPlayer()) {
         IsoPlayer.setInstance(var2);
         IsoCamera.setCameraCharacter(var2);
      }

      this.ensureOnTile();
      if (this.lastTargettedBy != null && this.lastTargettedBy.isDead()) {
         this.lastTargettedBy = null;
      }

      if (this.lastTargettedBy != null && this.TimeSinceZombieAttack > 120) {
         this.lastTargettedBy = null;
      }

      ++this.TimeSinceZombieAttack;
      if (var2 != null) {
         var2.setLastCollidedW(this.collidedW);
         var2.setLastCollidedN(this.collidedN);
      }

      if (!this.destroyed) {
         this.collidedThisFrame = false;
         this.collidedN = false;
         this.collidedS = false;
         this.collidedW = false;
         this.collidedE = false;
         this.CollidedWithDoor = false;
         this.last = this.current;
         this.CollidedObject = null;
         this.setNextX(this.getNextX() + this.impulsex);
         this.setNextY(this.getNextY() + this.impulsey);
         tempo.set(this.getNextX() - this.getX(), this.getNextY() - this.getY());
         if (tempo.getLength() > 1.0F) {
            tempo.normalize();
            this.setNextX(this.getX() + tempo.getX());
            this.setNextY(this.getY() + tempo.getY());
         }

         this.impulsex = 0.0F;
         this.impulsey = 0.0F;
         if (var3 == null || PZMath.fastfloor(this.getZ()) != 0 || this.getCurrentBuilding() != null || this.isInLoadedArea(PZMath.fastfloor(this.getNextX()), PZMath.fastfloor(this.getNextY())) || !var3.isCurrentState(PathFindState.instance()) && !var3.isCurrentState(WalkTowardState.instance())) {
            IsoAnimal var4 = (IsoAnimal)Type.tryCastTo(this, IsoAnimal.class);
            if (var4 != null && (int)this.getZ() == 0 && this.getCurrentBuilding() == null && !this.isInLoadedArea((int)this.getNextX(), (int)this.getNextY()) && var4.isCurrentState(AnimalZoneState.instance())) {
               AnimalPopulationManager.getInstance().virtualizeAnimal(var4);
            } else {
               float var5 = this.getNextX();
               float var6 = this.getNextY();
               this.collidedWithVehicle = false;
               if (var1 != null && !this.isOnFloor() && var1.getVehicle() == null && this.isCollidable() && (var2 == null || !var2.isNoClip())) {
                  int var7 = PZMath.fastfloor(this.getX());
                  int var8 = PZMath.fastfloor(this.getY());
                  int var9 = PZMath.fastfloor(this.getNextX());
                  int var10 = PZMath.fastfloor(this.getNextY());
                  int var11 = PZMath.fastfloor(this.getZ());
                  if (var1.getCurrentState() == null || !var1.getCurrentState().isIgnoreCollide(var1, var7, var8, var11, var9, var10, var11)) {
                     Vector2f var13 = PolygonalMap2.instance.resolveCollision(var1, this.getNextX(), this.getNextY(), IsoMovingObject.L_postUpdate.vector2f);
                     if (var13.x != this.getNextX() || var13.y != this.getNextY()) {
                        this.setNextX(var13.x);
                        this.setNextY(var13.y);
                        this.collidedWithVehicle = true;
                     }
                  }
               }

               float var15 = this.getNextX();
               float var16 = this.getNextY();
               float var17 = 0.0F;
               boolean var18 = false;
               float var12;
               float var19;
               if (this.Collidable) {
                  if (this.bAltCollide) {
                     this.DoCollide(2);
                  } else {
                     this.DoCollide(1);
                  }

                  if (this.collidedN || this.collidedS) {
                     this.setNextY(this.getLastY());
                     this.DoCollideNorS();
                  }

                  if (this.collidedW || this.collidedE) {
                     this.setNextX(this.getLastX());
                     this.DoCollideWorE();
                  }

                  if (this.bAltCollide) {
                     this.DoCollide(1);
                  } else {
                     this.DoCollide(2);
                  }

                  this.bAltCollide = !this.bAltCollide;
                  if (this.collidedN || this.collidedS) {
                     this.setNextY(this.getLastY());
                     this.DoCollideNorS();
                     var18 = true;
                  }

                  if (this.collidedW || this.collidedE) {
                     this.setNextX(this.getLastX());
                     this.DoCollideWorE();
                     var18 = true;
                  }

                  var17 = Math.abs(this.getNextX() - this.getLastX()) + Math.abs(this.getNextY() - this.getLastY());
                  var19 = this.getNextX();
                  var12 = this.getNextY();
                  this.setNextX(var15);
                  this.setNextY(var16);
                  if (this.Collidable && var18) {
                     if (this.bAltCollide) {
                        this.DoCollide(2);
                     } else {
                        this.DoCollide(1);
                     }

                     if (this.collidedN || this.collidedS) {
                        this.setNextY(this.getLastY());
                        this.DoCollideNorS();
                     }

                     if (this.collidedW || this.collidedE) {
                        this.setNextX(this.getLastX());
                        this.DoCollideWorE();
                     }

                     if (this.bAltCollide) {
                        this.DoCollide(1);
                     } else {
                        this.DoCollide(2);
                     }

                     if (this.collidedN || this.collidedS) {
                        this.setNextY(this.getLastY());
                        this.DoCollideNorS();
                        var18 = true;
                     }

                     if (this.collidedW || this.collidedE) {
                        this.setNextX(this.getLastX());
                        this.DoCollideWorE();
                        var18 = true;
                     }

                     if (Math.abs(this.getNextX() - this.getLastX()) + Math.abs(this.getNextY() - this.getLastY()) < var17) {
                        this.setNextX(var19);
                        this.setNextY(var12);
                     }
                  }
               }

               if (this.collidedThisFrame) {
                  this.setCurrent(this.last);
               }

               this.checkHitWall();
               if (var2 != null && !var2.isCurrentState(CollideWithWallState.instance()) && !this.collidedN && !this.collidedS && !this.collidedW && !this.collidedE) {
                  this.setCollideType((String)null);
               }

               var19 = this.getNextX() - this.getX();
               var12 = this.getNextY() - this.getY();
               float var20 = !(Math.abs(var19) > 0.0F) && !(Math.abs(var12) > 0.0F) ? 0.0F : this.getGlobalMovementMod();
               if (Math.abs(var19) > 0.01F || Math.abs(var12) > 0.01F) {
                  var19 *= var20;
                  var12 *= var20;
               }

               this.setX(this.getX() + var19);
               this.setY(this.getY() + var12);
               this.doStairs();
               this.handleSlopedSurface();
               this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ())));
               if (this.current == null) {
                  for(int var14 = PZMath.fastfloor(this.getZ()); var14 >= 0; --var14) {
                     this.current = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor((float)var14));
                     if (this.current != null) {
                        this.setZ(this.setLastZ(PZMath.min(this.getZ(), (float)this.current.getZ() + 0.99999F)));
                        break;
                     }
                  }

                  if (this.current == null && this.last != null) {
                     this.setCurrent(this.last);
                     this.setX(this.setNextX(this.setScriptNextX((float)this.current.getX() + 0.5F)));
                     this.setY(this.setNextY(this.setScriptNextY((float)this.current.getY() + 0.5F)));
                  }
               }

               if (this.movingSq != null) {
                  this.movingSq.getMovingObjects().remove(this);
                  this.movingSq = null;
               }

               if (this.current != null && !this.current.getMovingObjects().contains(this)) {
                  this.current.getMovingObjects().add(this);
                  this.movingSq = this.current;
               }

               this.ensureOnTile();
               this.square = this.current;
               this.setScriptNextX(this.getNextX());
               this.setScriptNextY(this.getNextY());
               this.firstUpdate = false;
            }
         } else {
            ZombiePopulationManager.instance.virtualizeZombie(var3);
         }
      }
   }

   public void updateAnimation() {
   }

   public void ensureOnTile() {
      if (this.current == null) {
         if (!(this instanceof IsoPlayer)) {
            if (this instanceof IsoSurvivor) {
               IsoWorld.instance.CurrentCell.Remove(this);
               IsoWorld.instance.CurrentCell.getSurvivorList().remove(this);
            }

            return;
         }

         boolean var1 = true;
         if (this.last != null && (this.last.Has(IsoObjectType.stairsTN) || this.last.Has(IsoObjectType.stairsTW))) {
            this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), (int)PZMath.floor(this.getZ() + 1.0F)));
            var1 = false;
         }

         if (this.current == null) {
            this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), (int)PZMath.floor(this.getZ())));
            if (this.current == null) {
               this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), (int)PZMath.floor(this.getZ()) + 1));
               if (this.current != null) {
                  this.setZ((float)this.current.z);
               }
            }

            return;
         }

         if (var1) {
            this.setX(this.setNextX(this.setScriptNextX((float)this.current.getX() + 0.5F)));
            this.setY(this.setNextY(this.setScriptNextY((float)this.current.getY() + 0.5F)));
         }

         this.setZ((float)this.current.getZ());
      }

   }

   public void preupdate() {
      this.setNextX(this.getX());
      this.setNextY(this.getY());
   }

   public void renderlast() {
      this.setOutlineHighlight(IsoCamera.frameState.playerIndex, false);
   }

   public void spotted(IsoMovingObject var1, boolean var2) {
   }

   public void update() {
      if (this.def == null) {
         this.def = IsoSpriteInstance.get(this.sprite);
      }

      this.movementLastFrame.x = this.getX() - this.getLastX();
      this.movementLastFrame.y = this.getY() - this.getLastY();
      this.setLastX(this.getX());
      this.setLastY(this.getY());
      this.setLastZ(this.getZ());
      this.square = this.current;
      if (this.sprite != null) {
         this.sprite.update(this.def);
      }

      this.StateEventDelayTimer -= GameTime.instance.getMultiplier();
   }

   private void Collided() {
      this.collidedThisFrame = true;
   }

   public int compareToY(IsoMovingObject var1) {
      if (this.sprite == null && var1.sprite == null) {
         return 0;
      } else if (this.sprite != null && var1.sprite == null) {
         return -1;
      } else if (this.sprite == null) {
         return 1;
      } else {
         float var2 = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
         float var3 = IsoUtils.YToScreen(var1.getX(), var1.getY(), var1.getZ(), 0);
         if ((double)var2 > (double)var3) {
            return 1;
         } else {
            return (double)var2 < (double)var3 ? -1 : 0;
         }
      }
   }

   public float distToNearestCamCharacter() {
      float var1 = 3.4028235E38F;

      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         IsoPlayer var3 = IsoPlayer.players[var2];
         if (var3 != null) {
            var1 = Math.min(var1, this.DistTo(var3));
         }
      }

      return var1;
   }

   public boolean isSolidForSeparate() {
      if (this instanceof IsoZombieGiblets) {
         return false;
      } else if (this.current == null) {
         return false;
      } else if (!this.solid) {
         return false;
      } else {
         return !this.isOnFloor();
      }
   }

   public boolean isPushableForSeparate() {
      return true;
   }

   public boolean isPushedByForSeparate(IsoMovingObject var1) {
      if (!(this instanceof IsoAnimal) || ((IsoAnimal)this).adef.collidable && !((IsoAnimal)this).getBehavior().blockMovement) {
         return !(var1 instanceof IsoAnimal) || ((IsoAnimal)var1).adef.collidable && !((IsoAnimal)var1).getBehavior().blockMovement;
      } else {
         return false;
      }
   }

   public void separate() {
      if (this.isSolidForSeparate()) {
         if (this.isPushableForSeparate()) {
            IsoGameCharacter var1 = (IsoGameCharacter)Type.tryCastTo(this, IsoGameCharacter.class);
            IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);

            for(int var3 = 0; var3 <= 8; ++var3) {
               IsoGridSquare var4 = var3 == 8 ? this.current : this.current.nav[var3];
               if (var4 != null && !var4.getMovingObjects().isEmpty() && (var4 == this.current || !this.current.isBlockedTo(var4))) {
                  float var5 = var2 != null && var2.getPrimaryHandItem() instanceof HandWeapon ? ((HandWeapon)var2.getPrimaryHandItem()).getMaxRange() : 0.3F;
                  int var6 = 0;

                  for(int var7 = var4.getMovingObjects().size(); var6 < var7; ++var6) {
                     IsoMovingObject var8 = (IsoMovingObject)var4.getMovingObjects().get(var6);
                     if (var8 != this && var8.isSolidForSeparate() && !(Math.abs(this.getZ() - var8.getZ()) > 0.3F)) {
                        IsoGameCharacter var9 = (IsoGameCharacter)Type.tryCastTo(var8, IsoGameCharacter.class);
                        IsoPlayer var10 = (IsoPlayer)Type.tryCastTo(var8, IsoPlayer.class);
                        float var11 = this.width + var8.width;
                        Vector2 var12 = tempo;
                        var12.x = this.getNextX() - var8.getNextX();
                        var12.y = this.getNextY() - var8.getNextY();
                        float var13 = var12.getLength();
                        if (var1 == null || var9 == null && !(var8 instanceof BaseVehicle)) {
                           if (var13 < var11) {
                              CollisionManager.instance.AddContact(this, var8);
                           }

                           return;
                        }

                        if (var9 != null) {
                           if (var2 != null && var2.getBumpedChr() != var8 && var13 < var11 + var5 && (double)var2.getForwardDirection().angleBetween(var12) > 2.6179938155736564 && var2.getBeenSprintingFor() >= 70.0F && WeaponType.getWeaponType((IsoGameCharacter)var2) == WeaponType.spear) {
                              var2.reportEvent("ChargeSpearConnect");
                              var2.setAttackType("charge");
                              var2.setAttackStarted(true);
                              var2.setVariable("StartedAttackWhileSprinting", true);
                              var2.setBeenSprintingFor(0.0F);
                              return;
                           }

                           if (!(var13 >= var11)) {
                              boolean var14 = false;
                              if (var2 != null && var2.getVariableFloat("WalkSpeed", 0.0F) > 0.2F && var2.runningTime > 0.5F && var2.getBumpedChr() != var8) {
                                 var14 = true;
                              }

                              if (GameClient.bClient && var2 != null && var9 instanceof IsoPlayer && !ServerOptions.getInstance().PlayerBumpPlayer.getValue()) {
                                 var14 = false;
                              }

                              if (var14 && !"charge".equals(var2.getAttackType())) {
                                 boolean var15 = !this.isOnFloor() && (var1.getBumpedChr() != null || (System.currentTimeMillis() - var2.getLastBump()) / 100L < 15L || var2.isSprinting()) && (var10 == null || !var10.isNPC());
                                 if (var15) {
                                    ++var1.bumpNbr;
                                    int var16 = 10 - var1.bumpNbr * 3;
                                    var16 += var1.getPerkLevel(PerkFactory.Perks.Fitness);
                                    var16 += var1.getPerkLevel(PerkFactory.Perks.Strength);
                                    var16 -= var1.getMoodles().getMoodleLevel(MoodleType.Drunk) * 2;
                                    if (var1.Traits.Clumsy.isSet()) {
                                       var16 -= 5;
                                    }

                                    if (var1.Traits.Graceful.isSet()) {
                                       var16 += 5;
                                    }

                                    if (var1.Traits.VeryUnderweight.isSet()) {
                                       var16 -= 8;
                                    }

                                    if (var1.Traits.Underweight.isSet()) {
                                       var16 -= 4;
                                    }

                                    if (var1.Traits.Obese.isSet()) {
                                       var16 -= 8;
                                    }

                                    if (var1.Traits.Overweight.isSet()) {
                                       var16 -= 4;
                                    }

                                    BodyPart var17 = var1.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
                                    if (var17.getAdditionalPain(true) > 20.0F) {
                                       var16 = (int)((float)var16 - (var17.getAdditionalPain(true) - 20.0F) / 20.0F);
                                    }

                                    var16 = Math.min(80, var16);
                                    var16 = Math.max(1, var16);
                                    if (Rand.Next(var16) == 0 || var1.isSprinting()) {
                                       var1.setVariable("BumpDone", false);
                                       var1.setBumpFall(true);
                                       var1.setVariable("TripObstacleType", "zombie");
                                    }
                                 } else {
                                    var1.bumpNbr = 0;
                                 }

                                 var1.setLastBump(System.currentTimeMillis());
                                 var1.setBumpedChr(var9);
                                 var1.setBumpType(this.getBumpedType(var9));
                                 boolean var19 = var1.isBehind(var9);
                                 String var18 = var1.getBumpType();
                                 if (var19) {
                                    if (var18.equals("left")) {
                                       var18 = "right";
                                    } else {
                                       var18 = "left";
                                    }
                                 }

                                 var9.setBumpType(var18);
                                 var9.setHitFromBehind(var19);
                                 if (var15 | GameClient.bClient) {
                                    var1.getActionContext().reportEvent("wasBumped");
                                 }
                              }

                              if (GameServer.bServer || this.distToNearestCamCharacter() < 60.0F) {
                                 if (this.isPushedByForSeparate(var8)) {
                                    var12.setLength((var13 - var11) / 8.0F);
                                    this.setNextX(this.getNextX() - var12.x);
                                    this.setNextY(this.getNextY() - var12.y);
                                 }

                                 this.collideWith(var8);
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

   public String getBumpedType(IsoGameCharacter var1) {
      float var2 = this.getX() - var1.getX();
      float var3 = this.getY() - var1.getY();
      String var4 = "left";
      if (this.dir == IsoDirections.S || this.dir == IsoDirections.SE || this.dir == IsoDirections.SW) {
         if (var2 < 0.0F) {
            var4 = "left";
         } else {
            var4 = "right";
         }
      }

      if (this.dir == IsoDirections.N || this.dir == IsoDirections.NE || this.dir == IsoDirections.NW) {
         if (var2 > 0.0F) {
            var4 = "left";
         } else {
            var4 = "right";
         }
      }

      if (this.dir == IsoDirections.E) {
         if (var3 > 0.0F) {
            var4 = "left";
         } else {
            var4 = "right";
         }
      }

      if (this.dir == IsoDirections.W) {
         if (var3 < 0.0F) {
            var4 = "left";
         } else {
            var4 = "right";
         }
      }

      return var4;
   }

   public float getLastX() {
      return this.m_lastX;
   }

   public float setLastX(float var1) {
      this.m_lastX = var1;
      return this.m_lastX;
   }

   public float getLastY() {
      return this.ly;
   }

   public float setLastY(float var1) {
      this.ly = var1;
      return this.ly;
   }

   public float getLastZ() {
      return this.lz;
   }

   public float setLastZ(float var1) {
      this.lz = var1;
      return this.lz;
   }

   public float getNextX() {
      return this.nx;
   }

   public float setNextX(float var1) {
      this.nx = var1;
      return this.nx;
   }

   public float getNextY() {
      return this.ny;
   }

   public float setNextY(float var1) {
      this.ny = var1;
      return this.ny;
   }

   public float getScriptNextX() {
      return this.scriptnx;
   }

   public float setScriptNextX(float var1) {
      this.scriptnx = var1;
      return this.scriptnx;
   }

   public float getScriptNextY() {
      return this.scriptny;
   }

   public float setScriptNextY(float var1) {
      this.scriptny = var1;
      return this.scriptny;
   }

   private void slideHeadAwayFromWalls() {
      if (this.current != null) {
         IsoZombie var1 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
         if (var1 != null && (this.isOnFloor() || var1.isKnockedDown())) {
            if (!var1.isCrawling() || var1.getPath2() == null && !var1.isMoving()) {
               if (!var1.isCurrentState(ClimbOverFenceState.instance()) && !var1.isCurrentState(ClimbThroughWindowState.instance())) {
                  if (var1.hasAnimationPlayer() && var1.getAnimationPlayer().isReady()) {
                     Vector3 var2 = IsoMovingObject.L_slideAwayFromWalls.vector3;
                     Model.BoneToWorldCoords((IsoGameCharacter)var1, var1.getAnimationPlayer().getSkinningBoneIndex("Bip01_Head", -1), var2);
                     if (Core.bDebug && DebugOptions.instance.CollideWithObstacles.Render.Radius.getValue()) {
                        LineDrawer.DrawIsoCircle(var2.x, var2.y, this.getZ(), 0.3F, 16, 1.0F, 1.0F, 0.0F, 1.0F);
                     }

                     Vector2 var3 = IsoMovingObject.L_slideAwayFromWalls.vector2.set(var2.x - this.getX(), var2.y - this.getY());
                     var3.normalize();
                     var2.x += var3.x * 0.3F;
                     var2.y += var3.y * 0.3F;
                     float var5;
                     if (var1.isKnockedDown() && (var1.isCurrentState(ZombieFallDownState.instance()) || var1.isCurrentState(StaggerBackState.instance()))) {
                        Vector2f var4 = PolygonalMap2.instance.resolveCollision(var1, var2.x, var2.y, IsoMovingObject.L_slideAwayFromWalls.vector2f);
                        if (var4.x != var2.x || var4.y != var2.y) {
                           var5 = GameTime.getInstance().getMultiplier() / 5.0F;
                           this.setNextX(this.getNextX() + (var4.x - var2.x) * var5);
                           this.setNextY(this.getNextY() + (var4.y - var2.y) * var5);
                           return;
                        }
                     }

                     if (PZMath.fastfloor(var2.x) != this.current.x || PZMath.fastfloor(var2.y) != this.current.y) {
                        IsoGridSquare var6 = this.getCell().getGridSquare(PZMath.fastfloor(var2.x), PZMath.fastfloor(var2.y), PZMath.fastfloor(this.getZ()));
                        if (var6 != null) {
                           if (this.current.testCollideAdjacent(this, var6.x - this.current.x, var6.y - this.current.y, 0)) {
                              var5 = GameTime.getInstance().getMultiplier() / 5.0F;
                              if (var6.x < this.current.x) {
                                 this.setNextX(this.getNextX() + ((float)this.current.x - var2.x) * var5);
                              } else if (var6.x > this.current.x) {
                                 this.setNextX(this.getNextX() + ((float)var6.x - var2.x) * var5);
                              }

                              if (var6.y < this.current.y) {
                                 this.setNextY(this.getNextY() + ((float)this.current.y - var2.y) * var5);
                              } else if (var6.y > this.current.y) {
                                 this.setNextY(this.getNextY() + ((float)var6.y - var2.y) * var5);
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

   private void slideFeetAwayFromWalls() {
      if (this.current != null) {
         IsoZombie var1 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
         if (var1 != null && (this.isOnFloor() || var1.isKnockedDown())) {
            if (var1.isCurrentState(ZombieHitReactionState.instance())) {
               if (StringUtils.equalsIgnoreCase(var1.getVariableString("HitReaction"), "FenceWindow")) {
                  if (var1.hasAnimationPlayer() && var1.getAnimationPlayer().isReady()) {
                     Vector3 var2 = IsoMovingObject.L_slideAwayFromWalls.vector3;
                     Model.BoneToWorldCoords((IsoGameCharacter)var1, var1.getAnimationPlayer().getSkinningBoneIndex("Bip01_L_Foot", -1), var2);
                     if (Core.bDebug && DebugOptions.instance.CollideWithObstacles.Render.Radius.getValue()) {
                        LineDrawer.DrawIsoCircle(var2.x, var2.y, this.getZ(), 0.3F, 16, 1.0F, 1.0F, 0.0F, 1.0F);
                     }

                     Vector2 var3 = IsoMovingObject.L_slideAwayFromWalls.vector2.set(var2.x - this.getX(), var2.y - this.getY());
                     var3.normalize();
                     var2.x += var3.x * 0.3F;
                     var2.y += var3.y * 0.3F;
                     if (PZMath.fastfloor(var2.x) != this.current.x || PZMath.fastfloor(var2.y) != this.current.y) {
                        IsoGridSquare var4 = this.getCell().getGridSquare(PZMath.fastfloor(var2.x), PZMath.fastfloor(var2.y), PZMath.fastfloor(this.getZ()));
                        if (var4 != null) {
                           if (this.current.testCollideAdjacent(this, var4.x - this.current.x, var4.y - this.current.y, 0)) {
                              float var5 = GameTime.getInstance().getMultiplier() / 5.0F;
                              if (var4.x < this.current.x) {
                                 this.setNextX(this.getNextX() + ((float)this.current.x - var2.x) * var5);
                              } else if (var4.x > this.current.x) {
                                 this.setNextX(this.getNextX() + ((float)var4.x - var2.x) * var5);
                              }

                              if (var4.y < this.current.y) {
                                 this.setNextY(this.getNextY() + ((float)this.current.y - var2.y) * var5);
                              } else if (var4.y > this.current.y) {
                                 this.setNextY(this.getNextY() + ((float)var4.y - var2.y) * var5);
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

   private boolean DoCollide(int var1) {
      IsoGameCharacter var2 = (IsoGameCharacter)Type.tryCastTo(this, IsoGameCharacter.class);
      this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getNextX()), PZMath.fastfloor(this.getNextY()), (int)PZMath.floor(this.getZ())));
      if (var2 != null && var2.isRagdollSimulationActive()) {
         return false;
      } else {
         int var3;
         int var4;
         int var5;
         if (this instanceof IsoMolotovCocktail) {
            for(var3 = PZMath.fastfloor(this.getZ()); var3 > 0; --var3) {
               for(var4 = -1; var4 <= 1; ++var4) {
                  for(var5 = -1; var5 <= 1; ++var5) {
                     IsoGridSquare var6 = this.getCell().createNewGridSquare(PZMath.fastfloor(this.getNextX()) + var5, PZMath.fastfloor(this.getNextY()) + var4, var3, false);
                     if (var6 != null) {
                        var6.RecalcAllWithNeighbours(true);
                     }
                  }
               }
            }
         }

         if (this.current != null) {
            if (!this.current.TreatAsSolidFloor()) {
               this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getNextX()), PZMath.fastfloor(this.getNextY()), (int)PZMath.floor(this.getZ())));
            }

            if (this.current == null) {
               return false;
            }

            this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getNextX()), PZMath.fastfloor(this.getNextY()), (int)PZMath.floor(this.getZ())));
         }

         if (this.current != this.last && this.last != null && this.current != null) {
            if (var2 != null && var2.getCurrentState() != null && var2.getCurrentState().isIgnoreCollide(var2, this.last.x, this.last.y, this.last.z, this.current.x, this.current.y, this.current.z)) {
               return false;
            }

            if (this == IsoCamera.getCameraCharacter()) {
               IsoWorld.instance.CurrentCell.lightUpdateCount = 10;
            }

            var3 = this.current.getX() - this.last.getX();
            var4 = this.current.getY() - this.last.getY();
            var5 = this.current.getZ() - this.last.getZ();
            boolean var9 = false;
            if (this.last.testCollideAdjacent(this, var3, var4, var5) || this.current == null) {
               var9 = true;
            }

            if (var9) {
               if (this.last.getX() < this.current.getX()) {
                  this.collidedE = true;
               }

               if (this.last.getX() > this.current.getX()) {
                  this.collidedW = true;
               }

               if (this.last.getY() < this.current.getY()) {
                  this.collidedS = true;
               }

               if (this.last.getY() > this.current.getY()) {
                  this.collidedN = true;
               }

               this.setCurrent(this.last);
               this.checkBreakHoppable();
               this.checkHitHoppable();
               if (var1 == 2) {
                  if ((this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                     this.collidedS = false;
                     this.collidedN = false;
                  }
               } else if (var1 == 1 && (this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                  this.collidedW = false;
                  this.collidedE = false;
               }

               this.Collided();
               return true;
            }
         } else if (this.getNextX() != this.getLastX() || this.getNextY() != this.getLastY()) {
            if (this instanceof IsoZombie && Core.GameMode.equals("Tutorial")) {
               return true;
            }

            if (this.current == null) {
               if (this.getNextX() < this.getLastX()) {
                  this.collidedW = true;
               }

               if (this.getNextX() > this.getLastX()) {
                  this.collidedE = true;
               }

               if (this.getNextY() < this.getLastY()) {
                  this.collidedN = true;
               }

               if (this.getNextY() > this.getLastY()) {
                  this.collidedS = true;
               }

               this.setNextX(this.getLastX());
               this.setNextY(this.getLastY());
               this.setCurrent(this.last);
               this.Collided();
               return true;
            }

            if (var2 != null && var2.getPath2() != null) {
               PathFindBehavior2 var7 = var2.getPathFindBehavior2();
               if (PZMath.fastfloor(var7.getTargetX()) == PZMath.fastfloor(this.getX()) && PZMath.fastfloor(var7.getTargetY()) == PZMath.fastfloor(this.getY()) && PZMath.fastfloor(var7.getTargetZ()) == PZMath.fastfloor(this.getZ())) {
                  return false;
               }
            }

            if (var2 != null && var2.isSittingOnFurniture()) {
               return false;
            }

            IsoGridSquare var8 = this.getFeelerTile(this.feelersize);
            if (var2 != null) {
               if (var2.isClimbing()) {
                  var8 = this.current;
               }

               if (var8 != null && var8 != this.current && var2.getPath2() != null && !var2.getPath2().crossesSquare(var8.x, var8.y, var8.z)) {
                  var8 = this.current;
               }
            }

            if (var8 != null && var8 != this.current && this.current != null) {
               if (var2 != null && var2.getCurrentState() != null && var2.getCurrentState().isIgnoreCollide(var2, this.current.x, this.current.y, this.current.z, var8.x, var8.y, var8.z)) {
                  return false;
               }

               if (this.current.testCollideAdjacent(this, var8.getX() - this.current.getX(), var8.getY() - this.current.getY(), var8.getZ() - this.current.getZ())) {
                  if (this.last != null) {
                     if (this.current.getX() < var8.getX()) {
                        this.collidedE = true;
                     }

                     if (this.current.getX() > var8.getX()) {
                        this.collidedW = true;
                     }

                     if (this.current.getY() < var8.getY()) {
                        this.collidedS = true;
                     }

                     if (this.current.getY() > var8.getY()) {
                        this.collidedN = true;
                     }

                     this.checkBreakHoppable();
                     this.checkHitHoppable();
                     if (var1 == 2 && (this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                        this.collidedS = false;
                        this.collidedN = false;
                     }

                     if (var1 == 1 && (this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                        this.collidedW = false;
                        this.collidedE = false;
                     }
                  }

                  this.Collided();
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void checkHitHoppableAnimal(IsoAnimal var1) {
      if (var1.adef.canClimbFences) {
         if (!var1.isCurrentState(AnimalAttackState.instance()) && !var1.isCurrentState(AnimalClimbOverFenceState.instance())) {
            if (this.collidedW && !this.collidedN && !this.collidedS && this.last.Is(IsoFlagType.HoppableW)) {
               var1.climbOverFence(IsoDirections.W);
            }

            if (this.collidedN && !this.collidedE && !this.collidedW && this.last.Is(IsoFlagType.HoppableN)) {
               var1.climbOverFence(IsoDirections.N);
            }

            IsoGridSquare var2;
            if (this.collidedS && !this.collidedE && !this.collidedW) {
               var2 = this.last.nav[IsoDirections.S.index()];
               if (var2 != null && var2.Is(IsoFlagType.HoppableN)) {
                  var1.climbOverFence(IsoDirections.S);
               }
            }

            if (this.collidedE && !this.collidedN && !this.collidedS) {
               var2 = this.last.nav[IsoDirections.E.index()];
               if (var2 != null && var2.Is(IsoFlagType.HoppableW)) {
                  var1.climbOverFence(IsoDirections.E);
               }
            }

         }
      }
   }

   private void checkHitHoppable() {
      IsoAnimal var1 = (IsoAnimal)Type.tryCastTo(this, IsoAnimal.class);
      if (var1 != null) {
         this.checkHitHoppableAnimal(var1);
      } else {
         IsoZombie var2 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
         if (var2 != null && !var2.bCrawling) {
            if (!var2.isCurrentState(AttackState.instance()) && !var2.isCurrentState(StaggerBackState.instance()) && !var2.isCurrentState(ClimbOverFenceState.instance()) && !var2.isCurrentState(ClimbThroughWindowState.instance())) {
               IsoGridSquare var3;
               if (this.collidedW && !this.collidedN && !this.collidedS) {
                  var3 = this.last.nav[IsoDirections.W.index()];
                  if (this.last.Is(IsoFlagType.HoppableW) && !this.last.HasStairsNorth() && (var3 == null || !var3.HasStairsNorth())) {
                     var2.climbOverFence(IsoDirections.W);
                  }
               }

               if (this.collidedN && !this.collidedE && !this.collidedW) {
                  var3 = this.last.nav[IsoDirections.N.index()];
                  if (this.last.Is(IsoFlagType.HoppableN) && !this.last.HasStairsWest() && (var3 == null || !var3.HasStairsWest())) {
                     var2.climbOverFence(IsoDirections.N);
                  }
               }

               if (this.collidedS && !this.collidedE && !this.collidedW) {
                  var3 = this.last.nav[IsoDirections.S.index()];
                  if (var3 != null && var3.Is(IsoFlagType.HoppableN) && !this.last.HasStairsWest() && !var3.HasStairsWest()) {
                     var2.climbOverFence(IsoDirections.S);
                  }
               }

               if (this.collidedE && !this.collidedN && !this.collidedS) {
                  var3 = this.last.nav[IsoDirections.E.index()];
                  if (var3 != null && var3.Is(IsoFlagType.HoppableW) && !this.last.HasStairsNorth() && !var3.HasStairsNorth()) {
                     var2.climbOverFence(IsoDirections.E);
                  }
               }

            }
         }
      }
   }

   private void checkBreakHoppable() {
      IsoZombie var1 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
      if (var1 != null && var1.bCrawling) {
         if (!var1.isCurrentState(AttackState.instance()) && !var1.isCurrentState(StaggerBackState.instance()) && !var1.isCurrentState(CrawlingZombieTurnState.instance())) {
            IsoDirections var2 = IsoDirections.Max;
            if (this.collidedW && !this.collidedN && !this.collidedS) {
               var2 = IsoDirections.W;
            }

            if (this.collidedN && !this.collidedE && !this.collidedW) {
               var2 = IsoDirections.N;
            }

            if (this.collidedS && !this.collidedE && !this.collidedW) {
               var2 = IsoDirections.S;
            }

            if (this.collidedE && !this.collidedN && !this.collidedS) {
               var2 = IsoDirections.E;
            }

            if (var2 != IsoDirections.Max) {
               IsoObject var3 = this.last.getHoppableTo(this.last.getAdjacentSquare(var2));
               IsoThumpable var4 = (IsoThumpable)Type.tryCastTo(var3, IsoThumpable.class);
               if (var4 != null && !var4.isThumpable()) {
                  var1.setThumpTarget(var4);
               } else if (var3 != null && var3.getThumpableFor(var1) != null) {
                  var1.setThumpTarget(var3);
               }

            }
         }
      }
   }

   private void checkHitWall() {
      if (this.collidedN || this.collidedS || this.collidedE || this.collidedW) {
         if (this.current != null) {
            IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
            if (var1 != null) {
               if (StringUtils.isNullOrEmpty(this.getCollideType())) {
                  boolean var2 = false;
                  int var3 = this.current.getWallType();
                  if ((var3 & 1) != 0 && this.collidedN && this.getDir() == IsoDirections.N) {
                     var2 = true;
                  }

                  if ((var3 & 2) != 0 && this.collidedS && this.getDir() == IsoDirections.S) {
                     var2 = true;
                  }

                  if ((var3 & 4) != 0 && this.collidedW && this.getDir() == IsoDirections.W) {
                     var2 = true;
                  }

                  if ((var3 & 8) != 0 && this.collidedE && this.getDir() == IsoDirections.E) {
                     var2 = true;
                  }

                  if (this.checkVaultOver()) {
                     var2 = false;
                  }

                  if (var2 && var1.isSprinting() && var1.isLocalPlayer()) {
                     this.setCollideType("wall");
                     var1.getActionContext().reportEvent("collideWithWall");
                     this.lastCollideTime = 70.0F;
                  }

               }
            }
         }
      }
   }

   private boolean checkVaultOver() {
      IsoPlayer var1 = (IsoPlayer)this;
      if (!var1.isCurrentState(ClimbOverFenceState.instance()) && !var1.isIgnoreAutoVault()) {
         if (!var1.IsRunning() && !var1.isSprinting() && !var1.isRemoteAndHasObstacleOnPath()) {
            return false;
         } else {
            IsoDirections var2 = this.getDir();
            IsoGridSquare var3 = this.current.getAdjacentSquare(IsoDirections.SE);
            if (var2 == IsoDirections.SE && var3 != null && var3.Is(IsoFlagType.HoppableN) && var3.Is(IsoFlagType.HoppableW)) {
               return false;
            } else {
               IsoGridSquare var4 = this.current;
               if (this.collidedS) {
                  var4 = this.current.getAdjacentSquare(IsoDirections.S);
               } else if (this.collidedE) {
                  var4 = this.current.getAdjacentSquare(IsoDirections.E);
               }

               if (var4 == null) {
                  return false;
               } else {
                  boolean var5 = false;
                  if (this.current.getProperties().Is(IsoFlagType.HoppableN) && this.collidedN && !this.collidedW && !this.collidedE && (var2 == IsoDirections.NW || var2 == IsoDirections.N || var2 == IsoDirections.NE)) {
                     var2 = IsoDirections.N;
                     var5 = true;
                  }

                  if (var4.getProperties().Is(IsoFlagType.HoppableN) && this.collidedS && !this.collidedW && !this.collidedE && (var2 == IsoDirections.SW || var2 == IsoDirections.S || var2 == IsoDirections.SE)) {
                     var2 = IsoDirections.S;
                     var5 = true;
                  }

                  if (this.current.getProperties().Is(IsoFlagType.HoppableW) && this.collidedW && !this.collidedN && !this.collidedS && (var2 == IsoDirections.NW || var2 == IsoDirections.W || var2 == IsoDirections.SW)) {
                     var2 = IsoDirections.W;
                     var5 = true;
                  }

                  if (var4.getProperties().Is(IsoFlagType.HoppableW) && this.collidedE && !this.collidedN && !this.collidedS && (var2 == IsoDirections.NE || var2 == IsoDirections.E || var2 == IsoDirections.SE)) {
                     var2 = IsoDirections.E;
                     var5 = true;
                  }

                  if (var5 && var1.isSafeToClimbOver(var2)) {
                     ClimbOverFenceState.instance().setParams(var1, var2);
                     var1.getActionContext().reportEvent("EventClimbFence");
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   public void setMovingSquareNow() {
      if (this.movingSq != null) {
         this.movingSq.getMovingObjects().remove(this);
         this.movingSq = null;
      }

      if (this.current != null && !this.current.getMovingObjects().contains(this)) {
         this.current.getMovingObjects().add(this);
         this.movingSq = this.current;
      }

   }

   public IsoGridSquare getFeelerTile(float var1) {
      Vector2 var2 = tempo;
      var2.x = this.getNextX() - this.getLastX();
      var2.y = this.getNextY() - this.getLastY();
      var2.setLength(var1);
      return this.getCell().getGridSquare(PZMath.fastfloor(this.getX() + var2.x), PZMath.fastfloor(this.getY() + var2.y), PZMath.fastfloor(this.getZ()));
   }

   public void DoCollideNorS() {
      this.setNextY(this.getLastY());
   }

   public void DoCollideWorE() {
      this.setNextX(this.getLastX());
   }

   public int getTimeSinceZombieAttack() {
      return this.TimeSinceZombieAttack;
   }

   public void setTimeSinceZombieAttack(int var1) {
      this.TimeSinceZombieAttack = var1;
   }

   public boolean isCollidedE() {
      return this.collidedE;
   }

   public void setCollidedE(boolean var1) {
      this.collidedE = var1;
   }

   public boolean isCollidedN() {
      return this.collidedN;
   }

   public void setCollidedN(boolean var1) {
      this.collidedN = var1;
   }

   public IsoObject getCollidedObject() {
      return this.CollidedObject;
   }

   public void setCollidedObject(IsoObject var1) {
      this.CollidedObject = var1;
   }

   public boolean isCollidedS() {
      return this.collidedS;
   }

   public void setCollidedS(boolean var1) {
      this.collidedS = var1;
   }

   public boolean isCollidedThisFrame() {
      return this.collidedThisFrame;
   }

   public void setCollidedThisFrame(boolean var1) {
      this.collidedThisFrame = var1;
   }

   public boolean isCollidedW() {
      return this.collidedW;
   }

   public void setCollidedW(boolean var1) {
      this.collidedW = var1;
   }

   public boolean isCollidedWithDoor() {
      return this.CollidedWithDoor;
   }

   public void setCollidedWithDoor(boolean var1) {
      this.CollidedWithDoor = var1;
   }

   public boolean isCollidedWithVehicle() {
      return this.collidedWithVehicle;
   }

   public IsoGridSquare getCurrentSquare() {
      return this.current;
   }

   public Zone getCurrentZone() {
      return this.current != null ? this.current.getZone() : null;
   }

   public void setCurrent(IsoGridSquare var1) {
      this.current = var1;
   }

   public boolean isDestroyed() {
      return this.destroyed;
   }

   public void setDestroyed(boolean var1) {
      this.destroyed = var1;
   }

   public boolean isFirstUpdate() {
      return this.firstUpdate;
   }

   public void setFirstUpdate(boolean var1) {
      this.firstUpdate = var1;
   }

   public Vector2 getHitDir() {
      return this.hitDir;
   }

   public void setHitDir(Vector2 var1) {
      this.hitDir.set(var1);
   }

   public float getImpulsex() {
      return this.impulsex;
   }

   public void setImpulsex(float var1) {
      this.impulsex = var1;
   }

   public float getImpulsey() {
      return this.impulsey;
   }

   public void setImpulsey(float var1) {
      this.impulsey = var1;
   }

   public float getLimpulsex() {
      return this.limpulsex;
   }

   public void setLimpulsex(float var1) {
      this.limpulsex = var1;
   }

   public float getLimpulsey() {
      return this.limpulsey;
   }

   public void setLimpulsey(float var1) {
      this.limpulsey = var1;
   }

   public float getHitForce() {
      return this.hitForce;
   }

   public void setHitForce(float var1) {
      this.hitForce = var1;
   }

   public float getHitFromAngle() {
      return this.hitFromAngle;
   }

   public void setHitFromAngle(float var1) {
      this.hitFromAngle = var1;
   }

   public IsoGridSquare getLastSquare() {
      return this.last;
   }

   public void setLast(IsoGridSquare var1) {
      this.last = var1;
   }

   public boolean getNoDamage() {
      return this.noDamage;
   }

   public void setNoDamage(boolean var1) {
      this.noDamage = var1;
   }

   public boolean isSolid() {
      return this.solid;
   }

   public void setSolid(boolean var1) {
      this.solid = var1;
   }

   public float getStateEventDelayTimer() {
      return this.StateEventDelayTimer;
   }

   public void setStateEventDelayTimer(float var1) {
      this.StateEventDelayTimer = var1;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float var1) {
      this.width = var1;
   }

   public boolean isbAltCollide() {
      return this.bAltCollide;
   }

   public void setbAltCollide(boolean var1) {
      this.bAltCollide = var1;
   }

   public boolean isShootable() {
      return this.shootable;
   }

   public void setShootable(boolean var1) {
      this.shootable = var1;
   }

   public IsoZombie getLastTargettedBy() {
      return this.lastTargettedBy;
   }

   public void setLastTargettedBy(IsoZombie var1) {
      this.lastTargettedBy = var1;
   }

   public boolean isCollidable() {
      return this.Collidable;
   }

   public void setCollidable(boolean var1) {
      this.Collidable = var1;
   }

   public float getScriptnx() {
      return this.getScriptNextX();
   }

   public void setScriptnx(float var1) {
      this.setScriptNextX(var1);
   }

   public float getScriptny() {
      return this.getScriptNextY();
   }

   public void setScriptny(float var1) {
      this.setScriptNextY(var1);
   }

   public String getScriptModule() {
      return this.ScriptModule;
   }

   public void setScriptModule(String var1) {
      this.ScriptModule = var1;
   }

   public Vector2 getMovementLastFrame() {
      return this.movementLastFrame;
   }

   public void setMovementLastFrame(Vector2 var1) {
      this.movementLastFrame = var1;
   }

   public float getFeelersize() {
      return this.feelersize;
   }

   public void setFeelersize(float var1) {
      this.feelersize = var1;
   }

   public byte canHaveMultipleHits() {
      byte var1 = 0;
      ArrayList var2 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoMovingObject var4 = (IsoMovingObject)var2.get(var3);
         IsoPlayer var5 = (IsoPlayer)Type.tryCastTo(var4, IsoPlayer.class);
         if (var5 != null) {
            HandWeapon var6 = (HandWeapon)Type.tryCastTo(var5.getPrimaryHandItem(), HandWeapon.class);
            if (var6 == null || var5.isDoShove() || var5.isForceShove()) {
               var6 = var5.bareHands;
            }

            float var7 = IsoUtils.DistanceTo(var5.getX(), var5.getY(), this.getX(), this.getY());
            float var8 = var6.getMaxRange() * var6.getRangeMod(var5) + 2.0F;
            if (!(var7 > var8)) {
               float var9 = var5.getDotWithForwardDirection(this.getX(), this.getY());
               if (!(var7 > 2.5F) || !(var9 < 0.1F)) {
                  LosUtil.TestResults var10 = LosUtil.lineClear(var5.getCell(), PZMath.fastfloor(var5.getX()), PZMath.fastfloor(var5.getY()), PZMath.fastfloor(var5.getZ()), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), false);
                  if (var10 != LosUtil.TestResults.Blocked && var10 != LosUtil.TestResults.ClearThroughClosedDoor) {
                     ++var1;
                     if (var1 >= 2) {
                        return var1;
                     }
                  }
               }
            }
         }
      }

      return var1;
   }

   public boolean isOnFloor() {
      return this.bOnFloor;
   }

   public void setOnFloor(boolean var1) {
      this.bOnFloor = var1;
   }

   public void Despawn() {
   }

   public boolean isCloseKilled() {
      return this.closeKilled;
   }

   public void setCloseKilled(boolean var1) {
      this.closeKilled = var1;
   }

   public Vector2 getFacingPosition(Vector2 var1) {
      var1.set(this.getX(), this.getY());
      return var1;
   }

   private boolean isInLoadedArea(int var1, int var2) {
      int var3;
      if (GameServer.bServer) {
         for(var3 = 0; var3 < ServerMap.instance.LoadedCells.size(); ++var3) {
            ServerMap.ServerCell var4 = (ServerMap.ServerCell)ServerMap.instance.LoadedCells.get(var3);
            if (var1 >= var4.WX * 64 && var1 < (var4.WX + 1) * 64 && var2 >= var4.WY * 64 && var2 < (var4.WY + 1) * 64) {
               return true;
            }
         }
      } else {
         for(var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
            IsoChunkMap var5 = IsoWorld.instance.CurrentCell.ChunkMap[var3];
            if (!var5.ignore && var1 >= var5.getWorldXMinTiles() && var1 < var5.getWorldXMaxTiles() && var2 >= var5.getWorldYMinTiles() && var2 < var5.getWorldYMaxTiles()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean isCollided() {
      return !StringUtils.isNullOrWhitespace(this.getCollideType());
   }

   public String getCollideType() {
      return this.collideType;
   }

   public void setCollideType(String var1) {
      this.collideType = var1;
   }

   public float getLastCollideTime() {
      return this.lastCollideTime;
   }

   public void setLastCollideTime(float var1) {
      this.lastCollideTime = var1;
   }

   public ArrayList<IsoZombie> getEatingZombies() {
      return this.eatingZombies;
   }

   public void setEatingZombies(ArrayList<IsoZombie> var1) {
      this.eatingZombies.clear();
      this.eatingZombies.addAll(var1);
   }

   public boolean isEatingOther(IsoMovingObject var1) {
      return var1 == null ? false : var1.eatingZombies.contains(this);
   }

   public float getDistanceSq(IsoMovingObject var1) {
      float var2 = this.getX() - var1.getX();
      float var3 = this.getY() - var1.getY();
      var2 *= var2;
      var3 *= var3;
      return var2 + var3;
   }

   public void setZombiesDontAttack(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseZombieDontAttackCheat)) {
         this.zombiesDontAttack = false;
      } else {
         this.zombiesDontAttack = var1;
      }
   }

   public boolean isZombiesDontAttack() {
      return this.zombiesDontAttack;
   }

   public boolean isExistInTheWorld() {
      return this.square != null ? this.square.getMovingObjects().contains(this) : false;
   }

   public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare var1) {
      return false;
   }

   private static final class L_postUpdate {
      static final Vector2f vector2f = new Vector2f();

      private L_postUpdate() {
      }
   }

   private static final class L_slideAwayFromWalls {
      static final Vector2f vector2f = new Vector2f();
      static final Vector2 vector2 = new Vector2();
      static final Vector3 vector3 = new Vector3();

      private L_slideAwayFromWalls() {
      }
   }
}
