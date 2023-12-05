package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.HashMap;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.animation.AnimationClip;

public final class ModelTxt {
   boolean bStatic;
   boolean bReverse;
   VertexBufferObject.VertexArray vertices;
   int[] elements;
   HashMap<String, Integer> boneIndices = new HashMap();
   ArrayList<Integer> SkeletonHierarchy = new ArrayList();
   ArrayList<Matrix4f> bindPose = new ArrayList();
   ArrayList<Matrix4f> skinOffsetMatrices = new ArrayList();
   ArrayList<Matrix4f> invBindPose = new ArrayList();
   HashMap<String, AnimationClip> clips = new HashMap();

   public ModelTxt() {
   }
}
