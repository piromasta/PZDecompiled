package zombie.core.skinnedmodel.model;

import zombie.util.StringUtils;

public enum SkeletonBone {
   Dummy01,
   Bip01,
   Bip01_Pelvis,
   Bip01_Spine,
   Bip01_Spine1,
   Bip01_Neck,
   Bip01_Head,
   Bip01_L_Clavicle,
   Bip01_L_UpperArm,
   Bip01_L_Forearm,
   Bip01_L_Hand,
   Bip01_L_Finger0,
   Bip01_L_Finger1,
   Bip01_R_Clavicle,
   Bip01_R_UpperArm,
   Bip01_R_Forearm,
   Bip01_R_Hand,
   Bip01_R_Finger0,
   Bip01_R_Finger1,
   Bip01_BackPack,
   Bip01_L_Thigh,
   Bip01_L_Calf,
   Bip01_L_Foot,
   Bip01_L_Toe0,
   Bip01_R_Thigh,
   Bip01_R_Calf,
   Bip01_R_Foot,
   Bip01_R_Toe0,
   Bip01_DressFront,
   Bip01_DressFront02,
   Bip01_DressBack,
   Bip01_DressBack02,
   Bip01_Prop1,
   Bip01_Prop2,
   Translation_Data,
   BONE_COUNT,
   None;

   private static SkeletonBone[] m_all = null;
   private static final Object m_allLock = "SkeletonBone_All_Lock";

   private SkeletonBone() {
   }

   public int index() {
      return this.ordinal() < BONE_COUNT.ordinal() ? this.ordinal() : -1;
   }

   public static int count() {
      return BONE_COUNT.ordinal();
   }

   public static SkeletonBone[] all() {
      if (m_all != null) {
         return m_all;
      } else {
         synchronized(m_allLock) {
            if (m_all == null) {
               SkeletonBone[] var1 = values();
               SkeletonBone[] var2 = new SkeletonBone[count()];

               for(int var3 = 0; var3 < count(); ++var3) {
                  var2[var3] = var1[var3];
               }

               m_all = var2;
            }

            return m_all;
         }
      }
   }

   public static String getBoneName(int var0) {
      return var0 >= 0 && var0 < count() ? all()[var0].toString() : "~IndexOutOfBounds:" + var0 + "~";
   }

   public static int getBoneOrdinal(String var0) {
      SkeletonBone var1 = (SkeletonBone)StringUtils.tryParseEnum(SkeletonBone.class, var0, None);
      return var1.ordinal();
   }
}
