package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class EventPacket implements INetworkPacket {
   public static final int MAX_PLAYER_EVENTS = 10;
   private static final long EVENT_TIMEOUT = 5000L;
   private static final int EVENT_FLAGS_VAULT_OVER_SPRINT = 1;
   private static final int EVENT_FLAGS_VAULT_OVER_RUN = 2;
   private static final int EVENT_FLAGS_BUMP_FALL = 4;
   private static final int EVENT_FLAGS_BUMP_STAGGERED = 8;
   private static final int EVENT_FLAGS_ACTIVATE_ITEM = 16;
   private static final int EVENT_FLAGS_CLIMB_SUCCESS = 32;
   private static final int EVENT_FLAGS_CLIMB_STRUGGLE = 64;
   private static final int EVENT_FLAGS_BUMP_FROM_BEHIND = 128;
   private static final int EVENT_FLAGS_BUMP_TARGET_TYPE = 256;
   private static final int EVENT_FLAGS_PRESSED_MOVEMENT = 512;
   private static final int EVENT_FLAGS_PRESSED_CANCEL_ACTION = 1024;
   private static final int EVENT_FLAGS_SMASH_CAR_WINDOW = 2048;
   private static final int EVENT_FLAGS_FITNESS_FINISHED = 4096;
   private static final int EVENT_FLAGS_PET_ANIMAL = 8192;
   private static final int EVENT_FLAGS_SHEAR_ANIMAL = 16384;
   private static final int EVENT_FLAGS_MILK_ANIMAL = 32768;
   private short id;
   public float x;
   public float y;
   public float z;
   private byte eventID;
   private String type1;
   private String type2;
   private String type3;
   private String type4;
   private float strafeSpeed;
   private float walkSpeed;
   private float walkInjury;
   private int booleanVariables;
   private int flags;
   private IsoPlayer player;
   private EventType event;
   private long timestamp;

   public EventPacket() {
   }

   public boolean isRelevant(UdpConnection var1) {
      return var1.RelevantTo(this.x, this.y);
   }

   public boolean isMovableEvent() {
      if (!this.isConsistent((UdpConnection)null)) {
         return false;
      } else {
         return EventPacket.EventType.EventClimbFence.equals(this.event) || EventPacket.EventType.EventFallClimb.equals(this.event);
      }
   }

   private boolean requireNonMoving() {
      return this.isConsistent((UdpConnection)null) && (EventPacket.EventType.EventClimbWindow.equals(this.event) || EventPacket.EventType.EventClimbFence.equals(this.event) || EventPacket.EventType.EventClimbDownRope.equals(this.event) || EventPacket.EventType.EventClimbRope.equals(this.event) || EventPacket.EventType.EventClimbWall.equals(this.event));
   }

   private IsoWindow getWindow(IsoPlayer var1) {
      IsoDirections[] var2 = IsoDirections.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         IsoDirections var5 = var2[var4];
         IsoObject var6 = var1.getContextDoorOrWindowOrWindowFrame(var5);
         if (var6 instanceof IsoWindow) {
            return (IsoWindow)var6;
         }
      }

      return null;
   }

   private IsoObject getObject(IsoPlayer var1) {
      IsoDirections[] var2 = IsoDirections.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         IsoDirections var5 = var2[var4];
         IsoObject var6 = var1.getContextDoorOrWindowOrWindowFrame(var5);
         if (var6 instanceof IsoWindow || var6 instanceof IsoThumpable || var6 instanceof IsoWindowFrame) {
            return var6;
         }
      }

      return null;
   }

   private IsoDirections checkCurrentIsEventGridSquareFence(IsoPlayer var1) {
      IsoGridSquare var3 = var1.getCell().getGridSquare((double)this.x, (double)this.y, (double)this.z);
      IsoGridSquare var4 = var1.getCell().getGridSquare((double)this.x, (double)(this.y + 1.0F), (double)this.z);
      IsoGridSquare var5 = var1.getCell().getGridSquare((double)(this.x + 1.0F), (double)this.y, (double)this.z);
      IsoDirections var2;
      if (var3 != null && var3.Is(IsoFlagType.HoppableN)) {
         var2 = IsoDirections.N;
      } else if (var3 != null && var3.Is(IsoFlagType.HoppableW)) {
         var2 = IsoDirections.W;
      } else if (var4 != null && var4.Is(IsoFlagType.HoppableN)) {
         var2 = IsoDirections.S;
      } else if (var5 != null && var5.Is(IsoFlagType.HoppableW)) {
         var2 = IsoDirections.E;
      } else {
         var2 = IsoDirections.Max;
      }

      return var2;
   }

   public boolean isTimeout() {
      return System.currentTimeMillis() > this.timestamp;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void processClient(UdpConnection var1) {
      if (this.isConsistent(var1)) {
         if (this.player.networkAI.events.size() < 10) {
            this.player.networkAI.events.add(this);
         } else {
            DebugLog.Multiplayer.warn("Event skipped: " + this.getDescription());
         }
      }

   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   public boolean process(IsoPlayer var1, UdpConnection var2) {
      return false;
   }

   public boolean set(IsoPlayer var1, String var2) {
      return false;
   }

   public static enum EventType {
      EventSetActivatedPrimary,
      EventSetActivatedSecondary,
      EventFishing,
      EventFitness,
      EventEmote,
      EventClimbFence,
      EventClimbDownRope,
      EventClimbRope,
      EventClimbWall,
      EventClimbWindow,
      EventOpenWindow,
      EventCloseWindow,
      EventSmashWindow,
      wasBumped,
      collideWithWall,
      EventUpdateFitness,
      EventFallClimb,
      EventOverrideItem,
      ChargeSpearConnect,
      Update,
      PetAnimal,
      ShearAnimal,
      MilkAnimal,
      Unknown;

      private EventType() {
      }
   }
}
