package uk.co.mysterymayhem.gravitymod.core;

import net.minecraft.launchwrapper.Launch;

public class ObfableName {
    
    private static final boolean deObf = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment");
    
    private ObfableName(String mcp, String srg) {
    }
    
    public static String get(String mcp, String srg) {
        if (deObf) {
            return mcp;
        } else  {
            return srg;
        }
    }
}
