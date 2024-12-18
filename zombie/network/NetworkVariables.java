package zombie.network;

public class NetworkVariables {
   public NetworkVariables() {
   }

   public static enum ZombieState {
      Attack("attack"),
      AttackNetwork("attack-network"),
      AttackVehicle("attackvehicle"),
      AttackVehicleNetwork("attackvehicle-network"),
      Bumped("bumped"),
      ClimbFence("climbfence"),
      ClimbWindow("climbwindow"),
      EatBody("eatbody"),
      FaceTarget("face-target"),
      FakeDead("fakedead"),
      FakeDeadAttack("fakedead-attack"),
      FakeDeadAttackNetwork("fakedead-attack-network"),
      FallDown("falldown"),
      Falling("falling"),
      GetDown("getdown"),
      Getup("getup"),
      HitReaction("hitreaction"),
      HitReactionHit("hitreaction-hit"),
      HitWhileStaggered("hitwhilestaggered"),
      Idle("idle"),
      Lunge("lunge"),
      LungeNetwork("lunge-network"),
      OnGround("onground"),
      PathFind("pathfind"),
      Sitting("sitting"),
      StaggerBack("staggerback"),
      Thump("thump"),
      TurnAlerted("turnalerted"),
      WalkToward("walktoward"),
      WalkTowardNetwork("walktoward-network"),
      FakeZombieStay("fakezombie-stay"),
      FakeZombieNormal("fakezombie-normal"),
      FakeZombieAttack("fakezombie-attack"),
      AnimalAlerted("alerted"),
      AnimalDeath("death"),
      AnimalEating("eating"),
      AnimalHutch("hutch"),
      AnimalTrailer("trailer"),
      AnimalWalk("walk"),
      AnimalZone("zone");

      private final String zombieState;

      private ZombieState(String var3) {
         this.zombieState = var3;
      }

      public String toString() {
         return this.zombieState;
      }

      public static ZombieState fromString(String var0) {
         ZombieState[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            ZombieState var4 = var1[var3];
            if (var4.zombieState.equalsIgnoreCase(var0)) {
               return var4;
            }
         }

         return Idle;
      }

      public static ZombieState fromByte(Byte var0) {
         ZombieState[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            ZombieState var4 = var1[var3];
            if (var4.ordinal() == var0) {
               return var4;
            }
         }

         return Idle;
      }

      public byte toByte() {
         return (byte)this.ordinal();
      }
   }

   public static enum ThumpType {
      TTNone(""),
      TTDoor("Door"),
      TTClaw("DoorClaw"),
      TTBang("DoorBang");

      private final String thumpType;

      private ThumpType(String var3) {
         this.thumpType = var3;
      }

      public String toString() {
         return this.thumpType;
      }

      public static ThumpType fromString(String var0) {
         ThumpType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            ThumpType var4 = var1[var3];
            if (var4.thumpType.equalsIgnoreCase(var0)) {
               return var4;
            }
         }

         return TTNone;
      }

      public static ThumpType fromByte(Byte var0) {
         ThumpType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            ThumpType var4 = var1[var3];
            if (var4.ordinal() == var0) {
               return var4;
            }
         }

         return TTNone;
      }
   }

   public static enum WalkType {
      WT1("1"),
      WT2("2"),
      WT3("3"),
      WT4("4"),
      WT5("5"),
      WTSprint1("sprint1"),
      WTSprint2("sprint2"),
      WTSprint3("sprint3"),
      WTSprint4("sprint4"),
      WTSprint5("sprint5"),
      WTSlow1("slow1"),
      WTSlow2("slow2"),
      WTSlow3("slow3");

      private final String walkType;

      private WalkType(String var3) {
         this.walkType = var3;
      }

      public String toString() {
         return this.walkType;
      }

      public static WalkType fromString(String var0) {
         WalkType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            WalkType var4 = var1[var3];
            if (var4.walkType.equalsIgnoreCase(var0)) {
               return var4;
            }
         }

         return WT1;
      }

      public static WalkType fromByte(byte var0) {
         WalkType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            WalkType var4 = var1[var3];
            if (var4.ordinal() == var0) {
               return var4;
            }
         }

         return WT1;
      }
   }

   public static enum PredictionTypes {
      None,
      Moving,
      Static,
      Thump,
      Climb,
      Lunge,
      LungeHalf,
      Walk,
      WalkHalf,
      PathFind;

      private PredictionTypes() {
      }

      public static PredictionTypes fromByte(byte var0) {
         PredictionTypes[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            PredictionTypes var4 = var1[var3];
            if (var4.ordinal() == var0) {
               return var4;
            }
         }

         return None;
      }
   }
}
