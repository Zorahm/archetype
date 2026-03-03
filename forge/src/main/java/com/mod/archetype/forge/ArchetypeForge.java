package com.mod.archetype.forge;

import com.mod.archetype.Archetype;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Archetype.MOD_ID)
public class ArchetypeForge {

    public ArchetypeForge() {
        Archetype.init();
        MinecraftForge.EVENT_BUS.register(ForgeEventTranslator.class);
    }
}
