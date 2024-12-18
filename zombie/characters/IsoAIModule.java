package zombie.characters;

import java.util.ArrayList;
import zombie.ai.GameCharacterAIBrain;

public class IsoAIModule {
   public static IsoPlayer invisibleCameraPlayer;
   public IsoPlayer player;
   public GameCharacterAIBrain brain;
   private boolean isInvisibleCamera;
   private ArrayList<IsoZombie> npcSeenZombies = new ArrayList();

   public IsoAIModule(IsoPlayer var1) {
      this.player = var1;
   }

   public IsoAIModule(IsoGameCharacter var1) {
      if (var1 instanceof IsoPlayer) {
         this.player = (IsoPlayer)var1;
      }

   }

   public GameCharacterAIBrain getBrain() {
      return this.brain;
   }

   public boolean doUpdatePlayerControls(boolean var1) {
      if (this.brain != null) {
         var1 = this.brain.HumanControlVars.bMelee;
         this.player.bBannedAttacking = this.brain.HumanControlVars.bBannedAttacking;
      }

      return var1;
   }

   public void update() {
      if (this.brain == null) {
         this.brain = new GameCharacterAIBrain(this.player);
      }

      this.brain.update();
   }

   public void setNPC(boolean var1) {
      if (var1 && this.brain == null) {
         this.brain = new GameCharacterAIBrain(this.player);
      }

      this.player.isNPC = var1;
   }

   public void postUpdate() {
      this.brain.postUpdateHuman(this.player);
      this.player.setInitiateAttack(this.brain.HumanControlVars.initiateAttack);
      this.player.setRunning(this.brain.HumanControlVars.bRunning);
      this.player.setJustMoved(this.brain.HumanControlVars.JustMoved);
      this.player.updateMovementRates();
   }

   public void initPlayerAI() {
   }
}
