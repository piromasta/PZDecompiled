package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import java.util.List;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.iso.Vector3;
import zombie.util.list.PZArrayUtil;

public final class AnimationNodeRecordingFrame extends GenericNameWeightRecordingFrame {
   private String m_actionGroupName;
   private String m_actionStateName;
   private final ArrayList<String> m_actionSubStateNames = new ArrayList();
   private String m_aiStateName;
   private String m_animStateName;
   private final ArrayList<String> m_animSubStateNames = new ArrayList();
   private final ArrayList<String> m_aiSubStateNames = new ArrayList();
   private final Vector3 m_characterToPlayerDiff = new Vector3();

   public AnimationNodeRecordingFrame(String var1) {
      super(var1);
   }

   public void logActionState(ActionGroup var1, ActionState var2, List<ActionState> var3) {
      this.m_actionGroupName = var1.getName();
      this.m_actionStateName = var2 != null ? var2.getName() : null;
      PZArrayUtil.arrayConvert(this.m_actionSubStateNames, var3, ActionState::getName);
   }

   public void logAIState(State var1, List<StateMachine.SubstateSlot> var2) {
      this.m_aiStateName = var1 != null ? var1.getName() : null;
      PZArrayUtil.arrayConvert(this.m_aiSubStateNames, var2, (var0) -> {
         return !var0.isEmpty() ? var0.getState().getName() : "";
      });
   }

   public void logAnimState(AnimState var1) {
      this.m_animStateName = var1 != null ? var1.m_Name : null;
   }

   public void logCharacterToPlayerDiff(Vector3 var1) {
      this.m_characterToPlayerDiff.set(var1);
   }

   public void buildHeader(StringBuilder var1) {
      appendCell(var1, "toPlayer.x");
      appendCell(var1, "toPlayer.y");
      appendCell(var1, "actionGroup");
      appendCell(var1, "actionState");
      appendCell(var1, "actionState.sub[0]");
      appendCell(var1, "actionState.sub[1]");
      appendCell(var1, "aiState");
      appendCell(var1, "aiState.sub[0]");
      appendCell(var1, "aiState.sub[1]");
      appendCell(var1, "animState");
      appendCell(var1, "animState.sub[0]");
      appendCell(var1, "animState.sub[1]");
      appendCell(var1, "nodeWeights.begin");
      super.buildHeader(var1);
   }

   protected void writeData(StringBuilder var1) {
      appendCell(var1, this.m_characterToPlayerDiff.x);
      appendCell(var1, this.m_characterToPlayerDiff.y);
      appendCellQuot(var1, this.m_actionGroupName);
      appendCellQuot(var1, this.m_actionStateName);
      appendCellQuot(var1, (String)PZArrayUtil.getOrDefault((List)this.m_actionSubStateNames, 0, ""));
      appendCellQuot(var1, (String)PZArrayUtil.getOrDefault((List)this.m_actionSubStateNames, 1, ""));
      appendCellQuot(var1, this.m_aiStateName);
      appendCellQuot(var1, (String)PZArrayUtil.getOrDefault((List)this.m_aiSubStateNames, 0, ""));
      appendCellQuot(var1, (String)PZArrayUtil.getOrDefault((List)this.m_aiSubStateNames, 1, ""));
      appendCellQuot(var1, this.m_animStateName);
      appendCellQuot(var1, (String)PZArrayUtil.getOrDefault((List)this.m_animSubStateNames, 0, ""));
      appendCellQuot(var1, (String)PZArrayUtil.getOrDefault((List)this.m_animSubStateNames, 1, ""));
      appendCell(var1);
      super.writeData(var1);
   }
}
