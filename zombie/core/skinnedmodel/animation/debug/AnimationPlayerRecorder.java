package zombie.core.skinnedmodel.animation.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import zombie.ZomboidFileSystem;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public final class AnimationPlayerRecorder {
   private static boolean s_isInitialized = false;
   private boolean m_isRecording = false;
   private boolean m_isLineActive = false;
   private final AnimationTrackRecordingFrame m_animationTrackFrame;
   private final AnimationNodeRecordingFrame m_animationNodeFrame;
   private final AnimationVariableRecordingFrame m_animationVariableFrame;
   private final AnimationEventRecordingFrame m_animationEventFrame;
   private final IsoGameCharacter m_character;

   public AnimationPlayerRecorder(IsoGameCharacter var1) {
      this.m_character = var1;
      String var2 = this.m_character.getUID();
      String var3 = var2 + "_AnimRecorder";
      this.m_animationTrackFrame = new AnimationTrackRecordingFrame(var3 + "_Track");
      this.m_animationNodeFrame = new AnimationNodeRecordingFrame(var3 + "_Node");
      this.m_animationVariableFrame = new AnimationVariableRecordingFrame(var3 + "_Vars");
      this.m_animationEventFrame = new AnimationEventRecordingFrame(var3 + "_Events");
      init();
   }

   public static synchronized void init() {
      if (!s_isInitialized) {
         DebugLog.General.debugln("Initializing...");
         s_isInitialized = true;
         backupOldRecordings();
      }
   }

   public static void backupOldRecordings() {
      String var0 = getRecordingDir();

      try {
         File var1 = new File(var0);
         File[] var2 = ZomboidFileSystem.listAllFiles(var1);
         if (var2.length == 0) {
            return;
         }

         String var4 = "backup_" + ZomboidFileSystem.getStartupTimeStamp();
         File var3 = new File(var0 + File.separator + var4);
         ZomboidFileSystem.ensureFolderExists(var3);

         for(int var7 = 0; var7 < var2.length; ++var7) {
            File var5 = var2[var7];
            if (var5.isFile()) {
               String var10003 = var3.getAbsolutePath();
               var5.renameTo(new File(var10003 + File.separator + var5.getName()));
               var5.delete();
            }
         }
      } catch (Exception var6) {
         DebugLog.General.printException(var6, "Exception thrown trying to backup old recordings, Trying to copy old recording files.", LogSeverity.Error);
      }

   }

   public static void discardOldRecordings() {
      String var0 = getRecordingDir();

      try {
         File var1 = new File(var0);
         File[] var2 = ZomboidFileSystem.listAllFiles(var1);
         if (var2.length == 0) {
            return;
         }

         for(int var3 = 0; var3 < var2.length; ++var3) {
            File var4 = var2[var3];
            if (var4.isFile()) {
               var4.delete();
            }
         }
      } catch (Exception var5) {
         DebugLog.General.printException(var5, "Exception thrown trying to discard old recordings, Trying to delete old recording files.", LogSeverity.Error);
      }

   }

   public void newFrame(int var1) {
      if (this.m_isLineActive) {
         this.writeFrame();
      }

      if (!this.isRecording()) {
         this.close();
      } else {
         this.m_isLineActive = true;
         this.m_animationTrackFrame.reset();
         this.m_animationTrackFrame.setFrameNumber(var1);
         this.m_animationNodeFrame.reset();
         this.m_animationNodeFrame.setFrameNumber(var1);
         this.m_animationVariableFrame.reset();
         this.m_animationVariableFrame.setFrameNumber(var1);
         this.m_animationEventFrame.reset();
         this.m_animationEventFrame.setFrameNumber(var1);
      }
   }

   public boolean hasActiveLine() {
      return this.m_isLineActive;
   }

   public void writeFrame() {
      this.m_animationTrackFrame.writeLine();
      this.m_animationNodeFrame.writeLine();
      this.m_animationVariableFrame.writeLine();
      this.m_animationEventFrame.writeLine();
      this.m_isLineActive = false;
   }

   public void discardRecording() {
      this.m_animationTrackFrame.closeAndDiscard();
      this.m_animationNodeFrame.closeAndDiscard();
      this.m_animationVariableFrame.closeAndDiscard();
      this.m_animationEventFrame.closeAndDiscard();
      this.m_isLineActive = false;
   }

   public void close() {
      this.m_animationTrackFrame.close();
      this.m_animationNodeFrame.close();
      this.m_animationVariableFrame.close();
      this.m_animationEventFrame.close();
      this.m_isLineActive = false;
   }

   public static PrintStream openFileStream(String var0, boolean var1, Consumer<String> var2) {
      String var3 = getTimeStampedFilePath(var0);

      try {
         var2.accept(var3);
         File var4 = new File(var3);
         return new PrintStream(new FileOutputStream(var4, var1), true);
      } catch (FileNotFoundException var5) {
         DebugLog.General.error("Exception thrown trying to create animation player recording file.");
         DebugLog.General.error(var5);
         var5.printStackTrace();
         return null;
      }
   }

   public static String getRecordingDir() {
      String var0 = ZomboidFileSystem.instance.getCacheDirSub("Recording");
      ZomboidFileSystem.ensureFolderExists(var0);
      File var1 = new File(var0);
      return var1.getAbsolutePath();
   }

   private static String getTimeStampedFilePath(String var0) {
      String var10000 = getRecordingDir();
      return var10000 + File.separator + getTimeStampedFileName(var0) + ".csv";
   }

   private static String getTimeStampedFileName(String var0) {
      String var10000 = ZomboidFileSystem.getStartupTimeStamp();
      return var10000 + "_" + var0;
   }

   public void logAnimWeights(List<AnimationTrack> var1, int[] var2, float[] var3, Vector2 var4, Vector2 var5) {
      this.m_animationTrackFrame.logAnimWeights(var1, var2, var3);
      this.m_animationVariableFrame.logDeferredMovement(var4, var5);
   }

   public void logAnimNode(LiveAnimNode var1) {
      if (var1.isTransitioningIn()) {
         this.m_animationNodeFrame.logWeight("transition(" + var1.getTransitionFrom() + "->" + var1.getName() + ")", var1.getTransitionLayerIdx(), var1.getTransitionInWeight());
      }

      if (var1.m_RunningRagdollTrack != null) {
         this.m_animationNodeFrame.logWeight(var1.getName() + "." + var1.m_RunningRagdollTrack.getName(), var1.getLayerIdx(), var1.m_RunningRagdollTrack.BlendDelta);
      }

      if (var1.isMainAnimActive()) {
         Iterator var2 = var1.getMainAnimationTracks().iterator();

         while(var2.hasNext()) {
            AnimationTrack var3 = (AnimationTrack)var2.next();
            this.m_animationNodeFrame.logWeight(var1.getName() + "." + var3.getName(), var1.getLayerIdx(), var3.BlendDelta);
         }
      }

      this.m_animationNodeFrame.logWeight(var1.getName(), var1.getLayerIdx(), var1.getWeight());
   }

   public void logActionState(ActionGroup var1, ActionState var2, List<ActionState> var3) {
      this.m_animationNodeFrame.logActionState(var1, var2, var3);
   }

   public void logAIState(State var1, List<StateMachine.SubstateSlot> var2) {
      this.m_animationNodeFrame.logAIState(var1, var2);
   }

   public void logAnimState(AnimState var1) {
      this.m_animationNodeFrame.logAnimState(var1);
   }

   public void logVariables(IAnimationVariableSource var1) {
      this.m_animationVariableFrame.logVariables(var1);
   }

   public void logAnimEvent(AnimationTrack var1, AnimEvent var2) {
      this.m_animationEventFrame.logAnimEvent(var1, var2);
   }

   public void logCharacterPos() {
      IsoPlayer var1 = IsoPlayer.getInstance();
      IsoGameCharacter var2 = this.getOwner();
      Vector3 var3 = var1.getPosition(new Vector3());
      Vector3 var4 = var2.getPosition(new Vector3());
      Vector3 var5 = var3.sub(var4, new Vector3());
      this.m_animationNodeFrame.logCharacterToPlayerDiff(var5);
   }

   public IsoGameCharacter getOwner() {
      return this.m_character;
   }

   public boolean isRecording() {
      return this.m_isRecording;
   }

   public void setRecording(boolean var1) {
      if (this.m_isRecording != var1) {
         this.m_isRecording = var1;
         if (!this.m_isRecording) {
            this.close();
         }

         DebugLog.General.println("AnimationPlayerRecorder %s.", this.m_isRecording ? "recording" : "stopped");
      }
   }
}
