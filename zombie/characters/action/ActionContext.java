package zombie.characters.action;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.action.conditions.CharacterVariableCondition;
import zombie.characters.action.conditions.EventNotOccurred;
import zombie.characters.action.conditions.EventOccurred;
import zombie.characters.action.conditions.LuaCall;
import zombie.core.profiling.PerformanceProbes;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;

public final class ActionContext {
   private final IAnimatable m_owner;
   private ActionGroup m_actionGroup;
   private ActionState m_currentState;
   private final ArrayList<ActionState> m_childStates = new ArrayList();
   private String m_previousStateName = null;
   private boolean m_statesChanged = false;
   public final ArrayList<IActionStateChanged> onStateChanged = new ArrayList();
   private final ActionContextEvents m_occurredAnimEvents = new ActionContextEvents();
   private final PerformanceProbes.Invokable.Params0.IProbe updateInternal = PerformanceProbes.create("ActionContext.update", this, (Invokers.Params1.ICallback)(ActionContext::updateInternal));
   private final PerformanceProbes.Invokable.Params0.IProbe postUpdateInternal = PerformanceProbes.create("ActionContext.postUpdate", this, (Invokers.Params1.ICallback)(ActionContext::postUpdateInternal));
   private final PerformanceProbes.Invokable.Params0.IProbe evaluateCurrentStateTransitions = PerformanceProbes.create("ActionContext.evaluateCurrentStateTransitions", this, (Invokers.Params1.ICallback)(ActionContext::evaluateCurrentStateTransitions));
   private final PerformanceProbes.Invokable.Params0.IProbe evaluateSubStateTransitions = PerformanceProbes.create("ActionContext.evaluateSubStateTransitions", this, (Invokers.Params1.ICallback)(ActionContext::evaluateSubStateTransitions));

   public ActionContext(IAnimatable var1) {
      this.m_owner = var1;
   }

   public IAnimatable getOwner() {
      return this.m_owner;
   }

   public void update() {
      this.updateInternal.invoke();
      this.postUpdateInternal.invoke();
   }

   private void updateInternal() {
      if (this.m_currentState != null) {
         this.evaluateCurrentStateTransitions.invoke();
         this.evaluateSubStateTransitions.invoke();
      }
   }

   private void postUpdateInternal() {
      this.clearActionContextEvents();
      this.invokeAnyStateChangedEvents();
      this.logCurrentState();
   }

   public ActionState getNextState() {
      ActionState var1 = null;

      int var2;
      ActionState var4;
      for(var2 = 0; var2 < this.m_currentState.m_transitions.size(); ++var2) {
         ActionTransition var3 = (ActionTransition)this.m_currentState.m_transitions.get(var2);
         if (!StringUtils.isNullOrWhitespace(var3.transitionTo) && var3.passes(this, 0)) {
            var4 = this.m_actionGroup.findState(var3.transitionTo);
            if (var4 != null && !this.hasChildState(var4)) {
               if (!var3.asSubstate || !this.currentStateSupportsChildState(var4)) {
                  var1 = var4;
                  break;
               }

               this.tryAddChildState(var4);
            }
         }
      }

      for(var2 = 0; var2 < this.childStateCount(); ++var2) {
         ActionState var8 = null;
         var4 = this.getChildStateAt(var2);

         for(int var5 = 0; var5 < var4.m_transitions.size(); ++var5) {
            ActionTransition var6 = (ActionTransition)var4.m_transitions.get(var5);
            if (var6.passes(this, 1)) {
               if (var6.transitionOut) {
                  --var2;
                  break;
               }

               if (!StringUtils.isNullOrWhitespace(var6.transitionTo)) {
                  ActionState var7 = this.m_actionGroup.findState(var6.transitionTo);
                  if (var7 != null && !this.hasChildState(var7)) {
                     if (this.currentStateSupportsChildState(var7)) {
                        break;
                     }

                     if (var6.forceParent) {
                        var8 = var7;
                        break;
                     }
                  }
               }
            }
         }

         if (var8 != this.m_currentState && var8 != null) {
            var1 = var8;
         }
      }

      return var1;
   }

   private void evaluateCurrentStateTransitions() {
      for(int var1 = 0; var1 < this.m_currentState.m_transitions.size(); ++var1) {
         ActionTransition var2 = (ActionTransition)this.m_currentState.m_transitions.get(var1);
         if (StringUtils.isNullOrWhitespace(var2.transitionTo)) {
            DebugLog.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", this.getOwner().getUID(), var2.transitionTo);
         } else if (var2.passes(this, 0)) {
            ActionState var3 = this.m_actionGroup.findState(var2.transitionTo);
            if (var3 == null) {
               DebugLog.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", this.getOwner().getUID(), var2.transitionTo);
            } else if (!this.hasChildState(var3)) {
               if (!var2.asSubstate || !this.currentStateSupportsChildState(var3)) {
                  if (this.m_owner instanceof IsoPlayer) {
                     DebugLog.DetailedInfo.trace("Player '%s' transits from %s to %s", ((IsoPlayer)this.m_owner).getUsername(), this.m_currentState.getName(), var2.transitionTo);
                  }

                  this.setCurrentState(var3);
                  break;
               }

               this.tryAddChildState(var3);
            }
         }
      }

   }

   private void evaluateSubStateTransitions() {
      for(int var1 = 0; var1 < this.childStateCount(); ++var1) {
         ActionState var2 = null;
         ActionState var3 = this.getChildStateAt(var1);

         for(int var4 = 0; var4 < var3.m_transitions.size(); ++var4) {
            ActionTransition var5 = (ActionTransition)var3.m_transitions.get(var4);
            if (var5.passes(this, 1)) {
               if (var5.transitionOut) {
                  this.removeChildStateAt(var1);
                  --var1;
                  break;
               }

               if (!StringUtils.isNullOrWhitespace(var5.transitionTo)) {
                  ActionState var6 = this.m_actionGroup.findState(var5.transitionTo);
                  if (var6 == null) {
                     DebugLog.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", this.getOwner().getUID(), var5.transitionTo);
                  } else if (!this.hasChildState(var6)) {
                     if (this.currentStateSupportsChildState(var6)) {
                        this.m_childStates.set(var1, var6);
                        this.onStatesChanged();
                        break;
                     }

                     if (var5.forceParent) {
                        var2 = var6;
                        break;
                     }
                  }
               }
            }
         }

         if (var2 != this.m_currentState && var2 != null) {
            this.setCurrentState(var2);
         }
      }

   }

   public boolean canTransitionToState(String var1) {
      return this.canTransitionToState(var1, true);
   }

   public boolean canTransitionToState(String var1, boolean var2) {
      ActionState var3 = this.m_actionGroup.findState(var1);
      if (var3 == null) {
         return false;
      } else {
         int var4;
         for(var4 = 0; var4 < this.m_currentState.m_transitions.size(); ++var4) {
            ActionTransition var5 = (ActionTransition)this.m_currentState.m_transitions.get(var4);
            if (StringUtils.equalsIgnoreCase(var1, var5.transitionTo)) {
               return true;
            }
         }

         if (!var2) {
            return false;
         } else if (!this.currentStateSupportsChildState(var3)) {
            return false;
         } else {
            for(var4 = 0; var4 < this.childStateCount(); ++var4) {
               ActionState var8 = this.getChildStateAt(var4);

               for(int var6 = 0; var6 < var8.m_transitions.size(); ++var6) {
                  ActionTransition var7 = (ActionTransition)var8.m_transitions.get(var6);
                  if (!var7.transitionOut && StringUtils.equalsIgnoreCase(var1, var7.transitionTo)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   protected boolean currentStateSupportsChildState(ActionState var1) {
      return this.m_currentState == null ? false : this.m_currentState.canHaveSubState(var1);
   }

   private boolean hasChildState(ActionState var1) {
      int var2 = this.indexOfChildState((var1x) -> {
         return var1x == var1;
      });
      return var2 > -1;
   }

   public void setPlaybackStateSnapshot(ActionStateSnapshot var1) {
      if (this.m_actionGroup != null) {
         if (var1.stateName == null) {
            DebugLog.General.warn("Snapshot not valid. Missing root state name.");
         } else {
            ActionState var2 = this.m_actionGroup.findState(var1.stateName);
            this.setCurrentState(var2);
            if (PZArrayUtil.isNullOrEmpty((Object[])var1.childStateNames)) {
               while(this.childStateCount() > 0) {
                  this.removeChildStateAt(0);
               }

            } else {
               int var3;
               String var4;
               for(var3 = 0; var3 < this.childStateCount(); ++var3) {
                  var4 = this.getChildStateAt(var3).getName();
                  boolean var5 = StringUtils.contains(var1.childStateNames, var4, StringUtils::equalsIgnoreCase);
                  if (!var5) {
                     this.removeChildStateAt(var3);
                     --var3;
                  }
               }

               for(var3 = 0; var3 < var1.childStateNames.length; ++var3) {
                  var4 = var1.childStateNames[var3];
                  ActionState var6 = this.m_actionGroup.findState(var4);
                  this.tryAddChildState(var6);
               }

            }
         }
      }
   }

   public ActionStateSnapshot getPlaybackStateSnapshot() {
      if (this.m_currentState == null) {
         return null;
      } else {
         ActionStateSnapshot var1 = new ActionStateSnapshot();
         var1.stateName = this.m_currentState.getName();
         var1.childStateNames = new String[this.m_childStates.size()];

         for(int var2 = 0; var2 < var1.childStateNames.length; ++var2) {
            var1.childStateNames[var2] = ((ActionState)this.m_childStates.get(var2)).getName();
         }

         return var1;
      }
   }

   public boolean setCurrentState(ActionState var1) {
      if (var1 == this.m_currentState) {
         return false;
      } else {
         this.m_previousStateName = this.m_currentState == null ? "" : this.m_currentState.getName();
         this.m_currentState = var1;

         for(int var2 = 0; var2 < this.m_childStates.size(); ++var2) {
            ActionState var3 = (ActionState)this.m_childStates.get(var2);
            if (!this.m_currentState.canHaveSubState(var3)) {
               this.removeChildStateAt(var2);
               --var2;
            }
         }

         this.onStatesChanged();
         return true;
      }
   }

   protected boolean tryAddChildState(ActionState var1) {
      if (this.hasChildState(var1)) {
         return false;
      } else {
         this.m_childStates.add(var1);
         this.onStatesChanged();
         return true;
      }
   }

   protected void removeChildStateAt(int var1) {
      this.m_childStates.remove(var1);
      this.onStatesChanged();
   }

   private void onStatesChanged() {
      this.m_statesChanged = true;
   }

   public void logCurrentState() {
      if (this.m_owner.isAnimationRecorderActive()) {
         this.m_owner.getAnimationPlayerRecorder().logActionState(this.m_actionGroup, this.m_currentState, this.m_childStates);
      }

   }

   private void invokeAnyStateChangedEvents() {
      if (this.m_statesChanged) {
         this.m_statesChanged = false;

         for(int var1 = 0; var1 < this.onStateChanged.size(); ++var1) {
            IActionStateChanged var2 = (IActionStateChanged)this.onStateChanged.get(var1);
            var2.actionStateChanged(this);
         }

         if (this.m_owner instanceof IsoZombie) {
            ((IsoZombie)this.m_owner).networkAI.extraUpdate();
         }

      }
   }

   private void clearActionContextEvents() {
      this.m_occurredAnimEvents.clear();
   }

   public ActionState getCurrentState() {
      return this.m_currentState;
   }

   public void setGroup(ActionGroup var1) {
      String var2 = this.m_currentState == null ? null : this.m_currentState.getName();
      this.m_actionGroup = var1;
      ActionState var3 = var1.getInitialState();
      if (!StringUtils.equalsIgnoreCase(var2, var3.getName())) {
         this.setCurrentState(var3);
      } else {
         this.m_currentState = var3;
      }

   }

   public ActionGroup getGroup() {
      return this.m_actionGroup;
   }

   public void reportEvent(String var1) {
      this.reportEvent(-1, var1);
   }

   public void reportEvent(int var1, String var2) {
      this.m_occurredAnimEvents.add(var2, var1);
      if (GameClient.bClient && var1 == -1 && IsoPlayer.isLocalPlayer((Object)this.m_owner)) {
         GameClient.sendEvent((IsoPlayer)this.m_owner, var2);
      }

   }

   public final boolean hasChildStates() {
      return this.childStateCount() > 0;
   }

   public final int childStateCount() {
      return this.m_childStates != null ? this.m_childStates.size() : 0;
   }

   public final void foreachChildState(Consumer<ActionState> var1) {
      for(int var2 = 0; var2 < this.childStateCount(); ++var2) {
         ActionState var3 = this.getChildStateAt(var2);
         var1.accept(var3);
      }

   }

   public final int indexOfChildState(Predicate<ActionState> var1) {
      int var2 = -1;

      for(int var3 = 0; var3 < this.childStateCount(); ++var3) {
         ActionState var4 = this.getChildStateAt(var3);
         if (var1.test(var4)) {
            var2 = var3;
            break;
         }
      }

      return var2;
   }

   public final ActionState getChildStateAt(int var1) {
      if (var1 >= 0 && var1 < this.childStateCount()) {
         return (ActionState)this.m_childStates.get(var1);
      } else {
         throw new IndexOutOfBoundsException(String.format("Index %d out of bounds. childCount: %d", var1, this.childStateCount()));
      }
   }

   public List<ActionState> getChildStates() {
      return this.m_childStates;
   }

   public String getCurrentStateName() {
      return this.m_currentState == null ? this.m_actionGroup.getDefaultState().getName() : this.m_currentState.getName();
   }

   public String getPreviousStateName() {
      return this.m_previousStateName;
   }

   public boolean hasEventOccurred(String var1) {
      return this.hasEventOccurred(var1, -1);
   }

   public boolean hasEventOccurred(String var1, int var2) {
      return this.m_occurredAnimEvents.contains(var1, var2);
   }

   public void clearEvent(String var1) {
      this.m_occurredAnimEvents.clearEvent(var1);
   }

   static {
      CharacterVariableCondition.Factory var0 = new CharacterVariableCondition.Factory();
      IActionCondition.registerFactory("isTrue", var0);
      IActionCondition.registerFactory("isFalse", var0);
      IActionCondition.registerFactory("compare", var0);
      IActionCondition.registerFactory("gtr", var0);
      IActionCondition.registerFactory("less", var0);
      IActionCondition.registerFactory("equals", var0);
      IActionCondition.registerFactory("lessEqual", var0);
      IActionCondition.registerFactory("gtrEqual", var0);
      IActionCondition.registerFactory("notEquals", var0);
      IActionCondition.registerFactory("eventOccurred", new EventOccurred.Factory());
      IActionCondition.registerFactory("eventNotOccurred", new EventNotOccurred.Factory());
      IActionCondition.registerFactory("lua", new LuaCall.Factory());
   }
}
