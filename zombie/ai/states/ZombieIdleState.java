package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.gameStates.IngameState;
import zombie.iso.objects.RainManager;

public final class ZombieIdleState extends State {
   private static final ZombieIdleState _instance = new ZombieIdleState();
   private static final Integer PARAM_TICK_COUNT = 0;

   public ZombieIdleState() {
   }

   public static ZombieIdleState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)var1;
      HashMap var3 = var2.getStateMachineParams(this);
      var2.soundSourceTarget = null;
      var2.soundAttract = 0.0F;
      var2.movex = 0.0F;
      var2.movey = 0.0F;
      var2.setStateEventDelayTimer(this.pickRandomWanderInterval());
      if (IngameState.instance == null) {
         var3.put(PARAM_TICK_COUNT, 0L);
      } else {
         var3.put(PARAM_TICK_COUNT, IngameState.instance.numberTicks);
      }

   }

   public void execute(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)var1;
      HashMap var3 = var2.getStateMachineParams(this);
      var2.movex = 0.0F;
      var2.movey = 0.0F;
      int var6;
      if (Core.bLastStand) {
         IsoPlayer var8 = null;
         float var5 = 1000000.0F;

         for(var6 = 0; var6 < IsoPlayer.numPlayers; ++var6) {
            if (IsoPlayer.players[var6] != null && IsoPlayer.players[var6].DistTo(var2) < var5 && !IsoPlayer.players[var6].isDead()) {
               var5 = IsoPlayer.players[var6].DistTo(var2);
               var8 = IsoPlayer.players[var6];
            }
         }

         if (var8 != null) {
            var2.pathToCharacter(var8);
         }

      } else {
         if (!var2.isReanimatedForGrappleOnly()) {
            if (var2.bCrawling) {
               var2.setOnFloor(true);
            } else {
               var2.setOnFloor(false);
            }
         }

         long var4 = (Long)var3.get(PARAM_TICK_COUNT);
         if (IngameState.instance.numberTicks - var4 == 2L) {
            var2.parameterZombieState.setState(ParameterZombieState.State.Idle);
         }

         if (!var2.bIndoorZombie) {
            if (!var2.isUseless()) {
               if (!var2.isReanimatedForGrappleOnly() && var2.getStateEventDelayTimer() <= 0.0F) {
                  var2.setStateEventDelayTimer(this.pickRandomWanderInterval());
                  var6 = PZMath.fastfloor(var2.getX()) + (Rand.Next(8) - 4);
                  int var7 = PZMath.fastfloor(var2.getY()) + (Rand.Next(8) - 4);
                  if (var2.getCell().getGridSquare((double)var6, (double)var7, (double)var2.getZ()) != null && var2.getCell().getGridSquare((double)var6, (double)var7, (double)var2.getZ()).isFree(true)) {
                     var2.pathToLocation(var6, var7, PZMath.fastfloor(var2.getZ()));
                     var2.AllowRepathDelay = 200.0F;
                  }
               }

               var2.networkAI.mindSync.zombieIdleUpdate();
            }
         }
      }
   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
   }

   private float pickRandomWanderInterval() {
      float var1 = (float)Rand.Next(400, 1000);
      if (!RainManager.isRaining()) {
         var1 *= 1.5F;
      }

      return var1;
   }
}
